package com.example.evchargingstationapp.data.repository

import android.content.Context
import android.util.Log
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.data.local.UserDbHelper
import com.example.evchargingstationapp.data.remote.ApiClient
import com.example.evchargingstationapp.model.User
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class UserRepository(private val context: Context) {
    private val db = UserDbHelper(context)
    private val prefs = PrefsHelper(context)
    private val apiClient = ApiClient(context)

    companion object {
        private const val TAG = "UserRepository"
        private const val BASE_URL = "http://10.0.2.2:5058/"

    }

    /**
     * POST JSON to server and handle error messages properly.
     */
    private fun postJson(urlString: String, jsonBody: JSONObject): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            Log.d(TAG, "=== NETWORK REQUEST ===")
            Log.d(TAG, "URL: $urlString")
            Log.d(TAG, "Body: $jsonBody")

            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            BufferedWriter(OutputStreamWriter(conn.outputStream, "UTF-8")).use {
                it.write(jsonBody.toString())
            }

            val code = conn.responseCode
            val input = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = input.bufferedReader().use { it.readText() }

            Log.d(TAG, "Response Code: $code")
            Log.d(TAG, "Response Body: $text")
            Log.d(TAG, "=== END NETWORK REQUEST ===")

            // 🧠 Parse response and extract validation messages
            return try {
                val json = JSONObject(text)

                // If the server returned validation errors in "errors" or "Errors"
                if (json.has("errors") || json.has("Errors")) {
                    val errorsObj = json.optJSONObject("errors") ?: json.optJSONObject("Errors")
                    val combined = StringBuilder()

                    errorsObj?.let {
                        for (key in it.keys()) {
                            val arr = it.optJSONArray(key)
                            if (arr != null) {
                                for (i in 0 until arr.length()) {
                                    combined.append("• ${arr.getString(i)}\n")
                                }
                            } else {
                                combined.append("• ${it.optString(key)}\n")
                            }
                        }
                    }

                    // Add a clean, readable Message field
                    if (combined.isNotEmpty()) {
                        json.put("Message", combined.toString().trim())
                    }
                }

                // If response is an array of messages (some APIs do this)
                if (text.trim().startsWith("[")) {
                    val arr = JSONArray(text)
                    val message = (0 until arr.length()).joinToString("\n") { "• ${arr.getString(it)}" }
                    return JSONObject().apply {
                        put("Success", false)
                        put("Message", message)
                    }
                }

                json
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing JSON response: ${e.message}")
                JSONObject().apply {
                    put("Success", false)
                    put("Message", text.ifEmpty { "Unknown server error" })
                }
            }
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "CONNECTION FAILED: ${e.message}")
            null
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "TIMEOUT: ${e.message}")
            null
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "UNKNOWN HOST: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "UNEXPECTED ERROR: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Register new EV Owner
     */
    fun register(user: User, password: String, confirmPassword: String, callback: (success: Boolean, message: String) -> Unit) {
        Thread {
            val body = JSONObject().apply {
                put("nic", user.nic)
                put("firstName", user.firstName)
                put("lastName", user.lastName)
                put("email", user.email)
                put("phoneNumber", user.phoneNumber)
                put("password", password)
                put("confirmPassword", confirmPassword)
            }

            val resp = postJson("${BASE_URL}api/v1/EVOwners/register", body)
            if (resp != null) {
                val success = resp.optBoolean("Success", resp.optBoolean("success", false))
                val message = resp.optString("Message", resp.optString("message", "Unknown error"))


                if (success) {
                    val data = resp.optJSONObject("Data")
                    val accessToken = data?.optString("AccessToken", "")
                    val refreshToken = data?.optString("RefreshToken", "")

                    prefs.saveNic(user.nic)
                    if (!accessToken.isNullOrEmpty()) prefs.saveAccessToken(accessToken)
                    if (!refreshToken.isNullOrEmpty()) prefs.saveRefreshToken(refreshToken)

                    db.insertOrUpdate(user)

                    (context as? android.app.Activity)?.runOnUiThread {
                        callback(true, message)
                    } ?: callback(true, message)
                } else {
                    (context as? android.app.Activity)?.runOnUiThread {
                        callback(false, message)
                    } ?: callback(false, message)
                }
            } else {
                val msg = "Cannot connect to server. Please check your connection."
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg)
                } ?: callback(false, msg)
            }
        }.start()
    }

    /**
     * Login existing user
     */
    fun login(nic: String, password: String, callback: (success: Boolean, message: String) -> Unit) {
        Thread {
            val body = JSONObject().apply {
                put("nic", nic)
                put("password", password)
            }

            val resp = postJson("${BASE_URL}api/v1/EVOwners/login", body)
            if (resp != null) {
                val success = resp.optBoolean("Success", resp.optBoolean("success", false))
                val message = resp.optString("Message", resp.optString("message", "Unknown error"))


                if (success) {
                    val data = resp.optJSONObject("Data")
                    val evOwner = data?.optJSONObject("EVOwner")
                    val accessToken = data?.optString("AccessToken", "")
                    val refreshToken = data?.optString("RefreshToken", "")

                    if (!accessToken.isNullOrEmpty()) prefs.saveAccessToken(accessToken)
                    if (!refreshToken.isNullOrEmpty()) prefs.saveRefreshToken(refreshToken)
                    prefs.saveNic(nic)

                    evOwner?.let {
                        val serverUser = User(
                            nic = it.optString("NIC", nic),
                            firstName = it.optString("FirstName", ""),
                            lastName = it.optString("LastName", ""),
                            email = it.optString("Email", ""),
                            phoneNumber = it.optString("PhoneNumber", ""),
                            password = password,
                            isActive = if (it.optString("Status", "Active") == "Active") 1 else 0
                        )
                        db.insertOrUpdate(serverUser)
                    }

                    (context as? android.app.Activity)?.runOnUiThread {
                        callback(true, message)
                    } ?: callback(true, message)
                } else {
                    (context as? android.app.Activity)?.runOnUiThread {
                        callback(false, message)
                    } ?: callback(false, message)
                }
            } else {
                val msg = "Cannot connect to server. Please check your connection."
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg)
                } ?: callback(false, msg)
            }
        }.start()
    }

    // Refresh token
    fun refreshAccessToken(callback: (success: Boolean, message: String) -> Unit) { /* unchanged */ }

    /**
     * Deactivate the current user's account
     */
    fun deactivateAccount(callback: (success: Boolean, message: String) -> Unit) {
        Thread {
            try {
                val response = apiClient.makeRequest(
                    endpoint = "/api/v1/EVOwners/me/deactivate",
                    method = "PATCH",
                    requiresAuth = true
                )

                if (response != null) {
                    val statusCode = response.optInt("statusCode", 0)
                    val success = statusCode in 200..299

                    // Extract message from various possible response formats
                    val message = if (success) {
                        response.optString("message",
                            response.optString("Message", "Account deactivated successfully"))
                    } else {
                        // Try to extract error message from different possible formats
                        val errorMsg = response.optString("message",
                            response.optString("Message",
                                response.optString("title", "Failed to deactivate account")))

                        // Log the full response for debugging
                        Log.e(TAG, "Deactivation failed. Status: $statusCode, Response: $response")
                        errorMsg
                    }

                    if (success) {
                        // Update local database
                        val userNic = prefs.getNic()
                        if (userNic != null) {
                            db.setStatus(userNic, 0)
                        }
                    }

                    (context as? android.app.Activity)?.runOnUiThread {
                        callback(success, message)
                    } ?: callback(success, message)
                } else {
                    val msg = "Cannot connect to server. Please check your connection."
                    (context as? android.app.Activity)?.runOnUiThread {
                        callback(false, msg)
                    } ?: callback(false, msg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deactivating account: ${e.message}")
                val msg = "An error occurred while deactivating account"
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg)
                } ?: callback(false, msg)
            }
        }.start()
    }

    fun isUserLoggedIn(): Boolean = prefs.isLoggedIn()
    fun logout() { prefs.clear() }
    fun getLocalUser(nic: String): User? = db.getUser(nic)
    fun deactivateLocalUser(nic: String) = db.setStatus(nic, 0)
}
