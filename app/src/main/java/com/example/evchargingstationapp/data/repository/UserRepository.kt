package com.example.evchargingstationapp.data.repository

import android.content.Context
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
        private const val BASE_URL = "http://10.0.2.2:2030" // Use 10.0.2.2 for Android emulator to access localhost
    }

    // helper: send POST with JSON body and parse response as JSONObject (or null)
    private fun postJson(urlString: String, jsonBody: JSONObject): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000 // 10 seconds
            conn.readTimeout = 10000

            val out = BufferedWriter(OutputStreamWriter(conn.outputStream, "UTF-8"))
            out.write(jsonBody.toString())
            out.flush()
            out.close()

            val code = conn.responseCode
            val input = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = input.bufferedReader().use { it.readText() }
            JSONObject(text)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            conn?.disconnect()
        }
    }

    // register: send to server, on success save to local SQLite + prefs
    fun register(user: User, password: String, confirmPassword: String, callback: (success:Boolean, message:String)->Unit) {
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
            val resp = postJson("$BASE_URL/api/v1/EVOwners/register", body)
            if (resp != null) {
                val success = resp.optBoolean("success", false)
                val message = resp.optString("message", "Unknown error")

                if (success) {
                    val data = resp.optJSONObject("data")
                    val accessToken = data?.optString("accessToken", "")
                    val refreshToken = data?.optString("refreshToken", "")

                    // save tokens and user locally
                    if (!accessToken.isNullOrEmpty()) {
                        prefs.saveAccessToken(accessToken)
                    }
                    if (!refreshToken.isNullOrEmpty()) {
                        prefs.saveRefreshToken(refreshToken)
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

                    // callback on main thread
                    (context as? android.app.Activity)?.runOnUiThread { callback(true, message) } ?: callback(true, message)
                } else {
                    (context as? android.app.Activity)?.runOnUiThread { callback(false, message) } ?: callback(false, message)
                }
            } else {
                val msg = "Cannot connect to server. Please check your connection."
                (context as? android.app.Activity)?.runOnUiThread { callback(false, msg) } ?: callback(false, msg)
            }
        }.start()
    }

    // login: similar
    fun login(nic: String, password: String, callback: (success:Boolean, message:String)->Unit) {
        Thread {
            val body = JSONObject().apply {
                put("nic", nic)
                put("password", password)
            }
            val resp = postJson("$BASE_URL/api/v1/EVOwners/login", body)
            if (resp != null) {
                val success = resp.optBoolean("success", false)
                val message = resp.optString("message", "Unknown error")

                if (success) {
                    val data = resp.optJSONObject("data")
                    val owner = data?.optJSONObject("owner")
                    val accessToken = data?.optString("accessToken", "")
                    val refreshToken = data?.optString("refreshToken", "")

                    // save tokens and user locally
                    if (!accessToken.isNullOrEmpty()) {
                        prefs.saveAccessToken(accessToken)
                    }
                    if (!refreshToken.isNullOrEmpty()) {
                        prefs.saveRefreshToken(refreshToken)
                    }
                    prefs.saveNic(nic)

                    // Update user in SQLite if we got data from server
                    if (owner != null) {
                        val serverUser = User(
                            nic = owner.optString("nic", nic),
                            firstName = owner.optString("firstName", ""),
                            lastName = owner.optString("lastName", ""),
                            email = owner.optString("email", ""),
                            phoneNumber = owner.optString("phoneNumber", ""),
                            password = password,
                            isActive = if (owner.optBoolean("isActive", true)) 1 else 0
                        )
                        db.insertOrUpdate(serverUser)
                    }

                    (context as? android.app.Activity)?.runOnUiThread { callback(true, message) } ?: callback(true, message)
                } else {
                    (context as? android.app.Activity)?.runOnUiThread { callback(false, message) } ?: callback(false, message)
                }
            } else {
                val msg = "Cannot connect to server. Please check your connection."
                (context as? android.app.Activity)?.runOnUiThread { callback(false, msg) } ?: callback(false, msg)
            }
        }.start()
    }

    // Refresh token to get new access token
    fun refreshAccessToken(callback: (success: Boolean, message: String) -> Unit) {
        Thread {
            val refreshToken = prefs.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, "No refresh token available")
                } ?: callback(false, "No refresh token available")
                return@Thread
            }

            val body = JSONObject().apply {
                put("refreshToken", refreshToken)
            }
            val resp = postJson("$BASE_URL/api/v1/EVOwners/refresh-token", body)
            if (resp != null) {
                val success = resp.optBoolean("success", false)
                val message = resp.optString("message", "Unknown error")

                if (success) {
                    val data = resp.optJSONObject("data")
                    val newAccessToken = data?.optString("accessToken", "")
                    val newRefreshToken = data?.optString("refreshToken", "")

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
                    // Refresh token expired or invalid - clear session
                    prefs.clear()
                    (context as? android.app.Activity)?.runOnUiThread {
                        callback(false, message)
                    } ?: callback(false, message)
                }
            } else {
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
        prefs.clear()
    }

    // local-only helpers
    fun getLocalUser(nic: String): User? = db.getUser(nic)
    fun deactivateLocalUser(nic: String) = db.setStatus(nic, 0)
}
