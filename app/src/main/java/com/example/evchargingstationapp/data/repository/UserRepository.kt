package com.example.evchargingstationapp.data.repository

import android.content.Context
import android.util.Log
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.data.local.UserDbHelper
import com.example.evchargingstationapp.model.User
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class UserRepository(private val context: Context) {
    private val db = UserDbHelper(context)
    private val prefs = PrefsHelper(context)

    companion object {
        private const val TAG = "UserRepository"
        private const val BASE_URL = "http://192.168.8.126:5058/" // Use 10.0.2.2 for Android emulator
    }

    // helper: send POST with JSON body and parse response as JSONObject (or null)
    private fun postJson(urlString: String, jsonBody: JSONObject): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            Log.d(TAG, "=== NETWORK REQUEST ===")
            Log.d(TAG, "URL: $urlString")
            Log.d(TAG, "Method: POST")
            Log.d(TAG, "Request Body: $jsonBody")

            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000 // 10 seconds
            conn.readTimeout = 10000

            Log.d(TAG, "Connecting...")

            val out = BufferedWriter(OutputStreamWriter(conn.outputStream, "UTF-8"))
            out.write(jsonBody.toString())
            out.flush()
            out.close()

            Log.d(TAG, "Request sent, waiting for response...")

            val code = conn.responseCode
            Log.d(TAG, "Response Code: $code")

            val input = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = input.bufferedReader().use { it.readText() }

            Log.d(TAG, "Response Body: $text")
            Log.d(TAG, "=== END NETWORK REQUEST ===")

            JSONObject(text)
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "CONNECTION FAILED: ${e.message}")
            Log.e(TAG, "Cannot reach server at $urlString")
            Log.e(TAG, "Make sure:")
            Log.e(TAG, "1. Backend is running")
            Log.e(TAG, "2. BASE_URL is correct")
            Log.e(TAG, "3. Phone/Emulator can reach the server")
            e.printStackTrace()
            null
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "TIMEOUT: Request took too long")
            Log.e(TAG, "URL: $urlString")
            e.printStackTrace()
            null
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "UNKNOWN HOST: Cannot resolve hostname")
            Log.e(TAG, "URL: $urlString")
            Log.e(TAG, "Check your BASE_URL configuration")
            e.printStackTrace()
            null
        } catch (e: Exception) {
            Log.e(TAG, "UNEXPECTED ERROR: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message: ${e.message}")
            Log.e(TAG, "URL: $urlString")
            e.printStackTrace()
            null
        } finally {
            conn?.disconnect()
        }
    }

    // register: send to server, on success save to local SQLite + prefs
    fun register(user: User, password: String, confirmPassword: String, callback: (success:Boolean, message:String)->Unit) {
        Thread {
            Log.d(TAG, "Starting registration for NIC: ${user.nic}")

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
                // Backend uses capital first letters: Success, Message, Data
                val success = resp.optBoolean("Success", false)
                val message = resp.optString("Message", "Unknown error")

                if (success) {
                    Log.d(TAG, "Registration successful")
                    val data = resp.optJSONObject("Data")
                    val accessToken = data?.optString("AccessToken", "")
                    val refreshToken = data?.optString("RefreshToken", "")

                    // save tokens and user locally
                    if (!accessToken.isNullOrEmpty()) {
                        prefs.saveAccessToken(accessToken)
                        Log.d(TAG, "Access token saved")
                    }
                    if (!refreshToken.isNullOrEmpty()) {
                        prefs.saveRefreshToken(refreshToken)
                        Log.d(TAG, "Refresh token saved")
                    }
                    prefs.saveNic(user.nic)

                    // Insert user into SQLite
                    val localUser = User(
                        nic = user.nic,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        email = user.email,
                        phoneNumber = user.phoneNumber,
                        password = password,
                        isActive = 1
                    )
                    db.insertOrUpdate(localUser)
                    Log.d(TAG, "User saved to local database")

                    // callback on main thread
                    (context as? android.app.Activity)?.runOnUiThread { callback(true, message) } ?: callback(true, message)
                } else {
                    Log.e(TAG, "Registration failed: $message")
                    (context as? android.app.Activity)?.runOnUiThread { callback(false, message) } ?: callback(false, message)
                }
            } else {
                Log.e(TAG, "Registration failed: No response from server")
                val msg = "Cannot connect to server. Please check your connection."
                (context as? android.app.Activity)?.runOnUiThread { callback(false, msg) } ?: callback(false, msg)
            }
        }.start()
    }

    // login: similar
    fun login(nic: String, password: String, callback: (success:Boolean, message:String)->Unit) {
        Thread {
            Log.d(TAG, "Starting login for NIC: $nic")

            val body = JSONObject().apply {
                put("nic", nic)
                put("password", password)
            }
            val resp = postJson("${BASE_URL}api/v1/EVOwners/login", body)
            if (resp != null) {
                // Backend uses capital first letters: Success, Message, Data
                val success = resp.optBoolean("Success", false)
                val message = resp.optString("Message", "Unknown error")

                if (success) {
                    Log.d(TAG, "Login successful")
                    val data = resp.optJSONObject("Data")
                    val evOwner = data?.optJSONObject("EVOwner")
                    val accessToken = data?.optString("AccessToken", "")
                    val refreshToken = data?.optString("RefreshToken", "")

                    // save tokens and user locally
                    if (!accessToken.isNullOrEmpty()) {
                        prefs.saveAccessToken(accessToken)
                        Log.d(TAG, "Access token saved")
                    }
                    if (!refreshToken.isNullOrEmpty()) {
                        prefs.saveRefreshToken(refreshToken)
                        Log.d(TAG, "Refresh token saved")
                    }
                    prefs.saveNic(nic)

                    // Update user in SQLite if we got data from server
                    if (evOwner != null) {
                        val serverUser = User(
                            nic = evOwner.optString("NIC", nic),
                            firstName = evOwner.optString("FirstName", ""),
                            lastName = evOwner.optString("LastName", ""),
                            email = evOwner.optString("Email", ""),
                            phoneNumber = evOwner.optString("PhoneNumber", ""),
                            password = password,
                            isActive = if (evOwner.optString("Status", "Active") == "Active") 1 else 0
                        )
                        db.insertOrUpdate(serverUser)
                        Log.d(TAG, "User data updated from server")
                    }

                    (context as? android.app.Activity)?.runOnUiThread { callback(true, message) } ?: callback(true, message)
                } else {
                    Log.e(TAG, "Login failed: $message")
                    (context as? android.app.Activity)?.runOnUiThread { callback(false, message) } ?: callback(false, message)
                }
            } else {
                Log.e(TAG, "Login failed: No response from server")
                val msg = "Cannot connect to server. Please check your connection."
                (context as? android.app.Activity)?.runOnUiThread { callback(false, msg) } ?: callback(false, msg)
            }
        }.start()
    }

    // Refresh token to get new access token
    fun refreshAccessToken(callback: (success: Boolean, message: String) -> Unit) {
        Thread {
            Log.d(TAG, "Attempting to refresh token")

            val refreshToken = prefs.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                Log.e(TAG, "No refresh token available")
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, "No refresh token available")
                } ?: callback(false, "No refresh token available")
                return@Thread
            }

            val body = JSONObject().apply {
                put("refreshToken", refreshToken)
            }
            val resp = postJson("${BASE_URL}api/v1/EVOwners/refresh-token", body)
            if (resp != null) {
                // Backend uses capital first letters: Success, Message, Data
                val success = resp.optBoolean("Success", false)
                val message = resp.optString("Message", "Unknown error")

                if (success) {
                    Log.d(TAG, "Token refresh successful")
                    val data = resp.optJSONObject("Data")
                    val newAccessToken = data?.optString("AccessToken", "")
                    val newRefreshToken = data?.optString("RefreshToken", "")

                    // Update tokens
                    if (!newAccessToken.isNullOrEmpty()) {
                        prefs.saveAccessToken(newAccessToken)
                    }
                    if (!newRefreshToken.isNullOrEmpty()) {
                        prefs.saveRefreshToken(newRefreshToken)
                    }

                    (context as? android.app.Activity)?.runOnUiThread {
                        callback(true, "Token refreshed")
                    } ?: callback(true, "Token refreshed")
                } else {
                    Log.e(TAG, "Token refresh failed: $message")
                    // Refresh token expired or invalid - clear session
                    prefs.clear()
                    (context as? android.app.Activity)?.runOnUiThread {
                        callback(false, message)
                    } ?: callback(false, message)
                }
            } else {
                Log.e(TAG, "Token refresh failed: No response from server")
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, "Cannot connect to server")
                } ?: callback(false, "Cannot connect to server")
            }
        }.start()
    }

    // Check if user session is valid
    fun isUserLoggedIn(): Boolean = prefs.isLoggedIn()

    // Logout - clear all session data
    fun logout() {
        Log.d(TAG, "User logged out")
        prefs.clear()
    }

    // local-only helpers
    fun getLocalUser(nic: String): User? = db.getUser(nic)
    fun deactivateLocalUser(nic: String) = db.setStatus(nic, 0)
}