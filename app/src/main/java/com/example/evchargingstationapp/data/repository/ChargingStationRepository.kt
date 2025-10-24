package com.example.evchargingstationapp.data.repository

import android.content.Context
import android.util.Log
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.model.ChargingStation
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ChargingStationRepository(private val context: Context) {

    private val prefs = PrefsHelper(context)

    companion object {
        private const val TAG = "ChargingStationRepo"
        private const val BASE_URL = "http://192.168.8.126:5058/" // Your backend URL
    }

    private fun sendRequest(urlString: String, method: String, jsonBody: JSONObject? = null): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            Log.d(TAG, "=== NETWORK REQUEST ===")
            Log.d(TAG, "URL: $urlString")
            Log.d(TAG, "Method: $method")

            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.setRequestProperty("Content-Type", "application/json")

            // Add Bearer token if available
            val token = prefs.getAccessToken()
            if (!token.isNullOrEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer $token")
                Log.d(TAG, "Authorization header set")
            } else {
                Log.e(TAG, "No access token found")
            }

            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (jsonBody != null) {
                conn.doOutput = true
                val out = BufferedWriter(OutputStreamWriter(conn.outputStream, "UTF-8"))
                out.write(jsonBody.toString())
                out.flush()
                out.close()
            }

            val code = conn.responseCode
            Log.d(TAG, "Response Code: $code")

            if (code == 401 || code == 403) {
                Log.e(TAG, "Authentication/Authorization failed")
                return JSONObject().apply {
                    put("Success", false)
                    put("Message", "Authentication required")
                    put("Data", null)
                }
            }

            val input = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = input?.bufferedReader()?.use { it.readText() } ?: ""
            Log.d(TAG, "Response Body: $text")
            Log.d(TAG, "=== END NETWORK REQUEST ===")

            if (text.isBlank()) {
                return JSONObject().apply {
                    put("Success", false)
                    put("Message", "Empty response from server")
                    put("Data", null)
                }
            }

            JSONObject(text)
        } catch (e: Exception) {
            Log.e(TAG, "Request failed: ${e.message}", e)
            JSONObject().apply {
                put("Success", false)
                put("Message", e.message ?: "Unknown error")
                put("Data", null)
            }
        } finally {
            conn?.disconnect()
        }
    }

    private fun parseStation(json: JSONObject): ChargingStation {
        return ChargingStation(
            id = json.optString("Id", ""),
            name = json.optString("Name", ""),
            location = json.optString("Location", ""),
            latitude = json.optDouble("Latitude", 0.0),
            longitude = json.optDouble("Longitude", 0.0),
            totalSlots = json.optInt("TotalSlots", 0),
            availableSlots = json.optInt("AvailableSlots", 0),
            pricePerHour = json.optDouble("PricePerHour", 0.0),
            status = json.optString("Status", "Active")
        )
    }

    fun getAllStations(callback: (success: Boolean, message: String, stations: List<ChargingStation>?) -> Unit) {
        Thread {
            if (prefs.getAccessToken().isNullOrEmpty()) {
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, "User not logged in", null)
                }
                return@Thread
            }

            val resp = sendRequest("${BASE_URL}api/v1/ChargingStation?onlyActive=true", "GET")
            val success = resp?.optBoolean("Success", false) ?: false
            val message = resp?.optString("Message", "Unknown error") ?: "Unknown error"
            val dataArray = resp?.optJSONArray("Data")

            val stations = if (success && dataArray != null) {
                (0 until dataArray.length()).map { i -> parseStation(dataArray.getJSONObject(i)) }
            } else {
                emptyList()
            }

            (context as? android.app.Activity)?.runOnUiThread {
                callback(success, message, stations)
            } ?: callback(success, message, stations)
        }.start()
    }
}
