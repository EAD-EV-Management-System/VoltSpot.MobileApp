package com.example.evchargingstationapp.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.model.StationOperator
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OperatorRepository(private val context: Context) {

    private val prefs = PrefsHelper(context)

    companion object {
        private const val TAG = "OperatorRepository"
        private const val BASE_URL = "http://192.168.8.126:5058/"  // ⚠️ Adjust to match your backend IP
    }

    /**
     * Operator login
     */
    fun login(username: String, password: String, callback: (success: Boolean, message: String, operator: StationOperator?) -> Unit) {
        Thread {
            try {
                val url = URL("${BASE_URL}api/v1/Auth/Login")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val body = JSONObject().apply {
                    put("Username", username)
                    put("Password", password)
                }

                BufferedWriter(OutputStreamWriter(connection.outputStream, "UTF-8")).use {
                    it.write(body.toString())
                    it.flush()
                }

                val code = connection.responseCode
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Response ($code): $responseText")

                val responseJson = JSONObject(responseText)
                val success = responseJson.optBoolean("Success", false)
                val message = responseJson.optString("Message", "Unknown error")

                if (success) {
                    val data = responseJson.optJSONObject("Data")
                    val userJson = data?.optJSONObject("User")

                    val operator = StationOperator(
                        id = userJson?.optString("Id") ?: "",
                        username = userJson?.optString("Username") ?: username,
                        firstName = userJson?.optString("FirstName") ?: "",
                        lastName = userJson?.optString("LastName") ?: "",
                        email = userJson?.optString("Email") ?: "",
                        role = userJson?.optString("Role") ?: "",
                        assignedStationIds = emptyList(), // or parse if available
                        status = userJson?.optString("Status") ?: "Active" // fallback
                    )

                    val accessToken = data?.optString("AccessToken") ?: ""
                    val refreshToken = data?.optString("RefreshToken") ?: ""

                    if (accessToken.isNotEmpty()) prefs.saveAccessToken(accessToken)
                    if (refreshToken.isNotEmpty()) prefs.saveRefreshToken(refreshToken)
                    prefs.saveNic(username) // optional; you can rename this to saveUsername()

                    runOnUi {
                        callback(true, message, operator)
                    }
                } else {
                    runOnUi {
                        callback(false, message, null)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Login failed: ${e.message}", e)
                runOnUi {
                    callback(false, "Error: ${e.message}", null)
                }
            }
        }.start()
    }

    /**
     * Logout: clear tokens and cached session
     */
    fun logout() {
        prefs.clear()
    }
    fun refreshToken(callback: (Boolean, String) -> Unit) {
        Thread {
            val token = prefs.getRefreshToken()
            if (token.isNullOrEmpty()) {
                runOnUi { callback(false, "No refresh token") }
                return@Thread
            }

            val body = JSONObject().put("RefreshToken", token)
            val url = URL("${BASE_URL}api/v1/Auth/refresh")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.write(body.toString().toByteArray())

            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(resp)
            val success = json.optBoolean("Success", false)
            val message = json.optString("Message", "Unknown error")

            if (success) {
                val data = json.optJSONObject("Data")
                val newAccess = data?.optString("AccessToken") ?: ""
                val newRefresh = data?.optString("RefreshToken") ?: ""
                if (newAccess.isNotEmpty()) prefs.saveAccessToken(newAccess)
                if (newRefresh.isNotEmpty()) prefs.saveRefreshToken(newRefresh)
            }

            runOnUi { callback(success, message) }
        }.start()
    }

    /**
     * Helper to run code on UI thread
     */
    private fun runOnUi(block: () -> Unit) {
        if (context is Activity) context.runOnUiThread(block) else block()
    }
}
