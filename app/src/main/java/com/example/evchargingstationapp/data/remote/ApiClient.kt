package com.example.evchargingstationapp.data.remote

import android.content.Context
import android.util.Log
import com.example.evchargingstationapp.data.local.PrefsHelper
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiClient(private val context: Context) {
    private val prefs = PrefsHelper(context)

    companion object {
        private const val TAG = "ApiClient"
        private const val BASE_URL = "http://10.0.2.2:2030"
    }

    /**
     * Makes an authenticated API request with automatic token refresh
     * @param endpoint The API endpoint (e.g., "/api/v1/EVOwners/profile")
     * @param method HTTP method (GET, POST, PUT, DELETE)
     * @param body Optional JSON body for POST/PUT requests
     * @param requiresAuth Whether this request requires authentication
     * @return JSONObject response or null on failure
     */
    fun makeRequest(
        endpoint: String,
        method: String = "GET",
        body: JSONObject? = null,
        requiresAuth: Boolean = true
    ): JSONObject? {
        var response = makeHttpRequest(endpoint, method, body, requiresAuth)

        // If we got 401 Unauthorized and we have a refresh token, try to refresh
        if (response != null && response.optInt("statusCode") == 401 && requiresAuth) {
            val refreshToken = prefs.getRefreshToken()
            if (!refreshToken.isNullOrEmpty()) {
                // Try to refresh the token
                val refreshBody = JSONObject().apply {
                    put("refreshToken", refreshToken)
                }
                val refreshResponse = makeHttpRequest(
                    "/api/v1/EVOwners/refresh-token",
                    "POST",
                    refreshBody,
                    false
                )

                if (refreshResponse != null && refreshResponse.optBoolean("success", false)) {
                    // Update tokens
                    val data = refreshResponse.optJSONObject("data")
                    val newAccessToken = data?.optString("accessToken", "")
                    val newRefreshToken = data?.optString("refreshToken", "")

                    if (!newAccessToken.isNullOrEmpty()) {
                        prefs.saveAccessToken(newAccessToken)
                    }
                    if (!newRefreshToken.isNullOrEmpty()) {
                        prefs.saveRefreshToken(newRefreshToken)
                    }

                    // Retry the original request with new token
                    response = makeHttpRequest(endpoint, method, body, requiresAuth)
                } else {
                    // Refresh failed, clear session
                    prefs.clear()
                }
            }
        }

        return response
    }

    private fun makeHttpRequest(
        endpoint: String,
        method: String,
        body: JSONObject?,
        requiresAuth: Boolean
    ): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL("$BASE_URL$endpoint")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            Log.d(TAG, "=== API REQUEST ===")
            Log.d(TAG, "URL: $BASE_URL$endpoint")
            Log.d(TAG, "Method: $method")
            Log.d(TAG, "Body: ${body?.toString() ?: "null"}")

            // Add authorization header if required
            if (requiresAuth) {
                val accessToken = prefs.getAccessToken()
                Log.d(TAG, "Auth Token: ${if (!accessToken.isNullOrEmpty()) "Present (${accessToken.take(20)}...)" else "Missing"}")
                if (!accessToken.isNullOrEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer $accessToken")
                }
            }

            // Add body if present (for POST/PUT/PATCH)
            if (body != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
                conn.doOutput = true
                val out = BufferedWriter(OutputStreamWriter(conn.outputStream, "UTF-8"))
                out.write(body.toString())
                out.flush()
                out.close()
            } else if (method == "PATCH") {
                // For PATCH without body, still need to set doOutput
                conn.doOutput = true
            }

            val code = conn.responseCode
            val input = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = input?.bufferedReader()?.use { it.readText() } ?: ""

            Log.d(TAG, "Response Code: $code")
            Log.d(TAG, "Response Body: $text")
            Log.d(TAG, "=== END API REQUEST ===")

            // Parse response
            val jsonResponse = if (text.isNotEmpty()) JSONObject(text) else JSONObject()
            jsonResponse.put("statusCode", code) // Add status code to response
            jsonResponse
        } catch (e: Exception) {
            Log.e(TAG, "API Request Error: ${e.message}", e)
            e.printStackTrace()
            null
        } finally {
            conn?.disconnect()
        }
    }
}
