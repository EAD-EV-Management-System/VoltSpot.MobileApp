package com.example.evchargingstationapp.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.model.Booking
import com.example.evchargingstationapp.model.BookingStatus
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
        private const val BASE_URL = "http://10.0.2.2:5058/"
    }

    /** Operator login */
    fun login(
        username: String,
        password: String,
        callback: (success: Boolean, message: String, operator: StationOperator?) -> Unit
    ) {
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

                    val operatorId = userJson?.optString("Id") ?: ""
                    val operator = StationOperator(
                        id = operatorId,
                        username = userJson?.optString("Username") ?: username,
                        firstName = userJson?.optString("FirstName") ?: "",
                        lastName = userJson?.optString("LastName") ?: "",
                        email = userJson?.optString("Email") ?: "",
                        role = userJson?.optString("Role") ?: "",
                        assignedStationIds = emptyList(),
                        status = userJson?.optString("Status") ?: "Active"
                    )

                    val accessToken = data?.optString("AccessToken") ?: ""
                    val refreshToken = data?.optString("RefreshToken") ?: ""

                    if (accessToken.isNotEmpty()) prefs.saveAccessToken(accessToken)
                    if (refreshToken.isNotEmpty()) prefs.saveRefreshToken(refreshToken)

                    // FIXED: Save the operator ID instead of username
                    if (operatorId.isNotEmpty()) {
                        prefs.saveOperatorId(operatorId)
                        Log.d(TAG, "Saved operator ID: $operatorId")
                    }

                    runOnUi { callback(true, message, operator) }
                } else {
                    runOnUi { callback(false, message, null) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login failed: ${e.message}", e)
                runOnUi { callback(false, "Error: ${e.message}", null) }
            }
        }.start()
    }

    /** Logout */
    fun logout() {
        prefs.clear()
    }

    fun getBookingById(
        bookingId: String,
        callback: (success: Boolean, message: String, booking: Booking?) -> Unit
    ) {
        Thread {
            try {
                val url = URL("${BASE_URL}api/v1/Booking/$bookingId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer ${prefs.getAccessToken()}")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val code = conn.responseCode
                val resp = conn.inputStream.bufferedReader().use { it.readText() }

                if (code == 200) {
                    val json = JSONObject(resp)
                    val success = json.optBoolean("Success", true)
                    val data = json.optJSONObject("Data")
                    val booking = Booking(
                        id = data?.optString("Id") ?: "",
                        evOwnerNic = data?.optString("EvOwnerNic") ?: "",
                        chargingStationId = data?.optString("ChargingStationId") ?: "",
                        chargingStationName = data?.optString("ChargingStationName"),
                        slotNumber = data?.optInt("SlotNumber") ?: 0,
                        reservationDateTime = data?.optString("ReservationDateTime") ?: "",
                        status = BookingStatus.fromString(data?.optString("Status") ?: "Pending"),
                        cancellationReason = data?.optString("CancellationReason"),
                        createdAt = data?.optString("CreatedAt") ?: "",
                        updatedAt = data?.optString("UpdatedAt") ?: ""
                    )
                    runOnUi { callback(true, "Success", booking) }
                } else {
                    runOnUi { callback(false, "Booking not found", null) }
                }
            } catch (e: Exception) {
                runOnUi { callback(false, e.message ?: "Unknown error", null) }
            }
        }.start()
    }

    /** Refresh token */
    fun refreshToken(callback: (Boolean, String) -> Unit) {
        Thread {
            val token = prefs.getRefreshToken()
            if (token.isNullOrEmpty()) {
                runOnUi { callback(false, "No refresh token") }
                return@Thread
            }

            try {
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
            } catch (e: Exception) {
                runOnUi { callback(false, "Error: ${e.message}") }
            }
        }.start()
    }

    fun getUserById(operatorId: String, callback: (success: Boolean, message: String, operator: StationOperator?) -> Unit) {
        Thread {
            try {
                val url = URL("${BASE_URL}api/v1/Users/$operatorId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer ${prefs.getAccessToken()}")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val code = conn.responseCode
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "getUserById response ($code): $resp")

                if (code == 200) {
                    val json = JSONObject(resp)
                    val success = json.optBoolean("Success", true)
                    val data = json.optJSONObject("Data")  // assuming Data contains the user

                    if (success && data != null) {
                        val operator = StationOperator(
                            id = data.optString("Id"),
                            username = data.optString("Username"),
                            firstName = data.optString("FirstName"),
                            lastName = data.optString("LastName"),
                            email = data.optString("Email"),
                            role = data.optString("Role"),
                            assignedStationIds = emptyList(),
                            status = data.optString("Status")
                        )
                        runOnUi { callback(true, "Success", operator) }
                    } else {
                        runOnUi { callback(false, "Failed to fetch profile", null) }
                    }
                } else {
                    runOnUi { callback(false, "Failed: $code", null) }
                }
            } catch (e: Exception) {
                runOnUi { callback(false, e.message ?: "Unknown error", null) }
            }
        }.start()
    }

    fun getProfile(callback: (success: Boolean, message: String, operator: StationOperator?) -> Unit) {
        Thread {
            try {
                val url = URL("${BASE_URL}api/v1/Auth/me")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer ${prefs.getAccessToken()}")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val code = conn.responseCode
                val resp = conn.inputStream.bufferedReader().use { it.readText() }

                if (code == 200) {
                    val json = JSONObject(resp)
                    val success = json.optBoolean("Success", true)
                    val data = json.optJSONObject("Data")
                    val userJson = data?.optJSONObject("User")

                    if (success && userJson != null) {
                        val operator = StationOperator(
                            id = userJson.optString("Id"),
                            username = userJson.optString("Username"),
                            firstName = userJson.optString("FirstName"),
                            lastName = userJson.optString("LastName"),
                            email = userJson.optString("Email"),
                            role = userJson.optString("Role"),
                            assignedStationIds = emptyList(),
                            status = userJson.optString("Status")
                        )
                        runOnUi { callback(true, "Success", operator) }
                    } else {
                        runOnUi { callback(false, "Failed to fetch profile", null) }
                    }
                } else {
                    runOnUi { callback(false, "Failed: $code", null) }
                }
            } catch (e: Exception) {
                runOnUi { callback(false, e.message ?: "Unknown error", null) }
            }
        }.start()
    }


    /** Fetch bookings for operator */
    fun getBookings(
        operatorId: String,
        callback: (success: Boolean, message: String, bookings: List<Booking>?) -> Unit
    ) {
        Thread {
            try {
                Log.d(TAG, "Fetching bookings for operator ID: $operatorId")

                val url = URL("${BASE_URL}api/v1/Booking/operator/$operatorId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer ${prefs.getAccessToken()}")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val code = connection.responseCode
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Operator Bookings Response ($code): $responseText")

                if (code == 200) {
                    val json = JSONObject(responseText)
                    val success = json.optBoolean("Success", true)
                    val message = json.optString("Message", "")
                    val dataArray = json.optJSONArray("Data")
                    val bookings = mutableListOf<Booking>()

                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val obj = dataArray.getJSONObject(i)
                            bookings.add(
                                Booking(
                                    id = obj.optString("Id"),
                                    evOwnerNic = obj.optString("EvOwnerNic"),
                                    chargingStationId = obj.optString("ChargingStationId", ""),
                                    chargingStationName = obj.optString("ChargingStationName"),
                                    slotNumber = obj.optInt("SlotNumber", 0),
                                    reservationDateTime = obj.optString("ReservationDateTime", ""),
                                    status = BookingStatus.fromString(obj.optString("Status", "Pending")),
                                    cancellationReason = obj.optString("CancellationReason"),
                                    createdAt = obj.optString("CreatedAt", ""),
                                    updatedAt = obj.optString("UpdatedAt", "")
                                )
                            )
                        }
                    }

                    Log.d(TAG, "Successfully parsed ${bookings.size} bookings")
                    runOnUi { callback(true, message, bookings) }
                } else {
                    runOnUi { callback(false, "Failed to fetch bookings", null) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching operator bookings: ${e.message}", e)
                runOnUi { callback(false, e.message ?: "Unknown error", null) }
            }
        }.start()
    }

    /** Helper to run on UI thread */
    private fun runOnUi(block: () -> Unit) {
        if (context is Activity) context.runOnUiThread(block) else block()
    }
}