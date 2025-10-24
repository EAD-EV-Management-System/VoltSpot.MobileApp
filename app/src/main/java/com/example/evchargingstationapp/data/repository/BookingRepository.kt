package com.example.evchargingstationapp.data.repository

import android.content.Context
import android.util.Log
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.model.*
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class BookingRepository(private val context: Context) {
    private val prefs = PrefsHelper(context)

    companion object {
        private const val TAG = "BookingRepository"
        private const val BASE_URL = "http://10.0.2.2:5058/"
    }

    // Helper function to safely get boolean from JSON with both cases
    private fun JSONObject.safeGetBoolean(key: String, default: Boolean = false): Boolean {
        return this.optBoolean(key, this.optBoolean(key.lowercase(), default))
    }

    // Helper function to safely get string from JSON with both cases
    private fun JSONObject.safeGetString(key: String, default: String = ""): String {
        val value = this.optString(key, "")
        return value.ifEmpty { this.optString(key.lowercase(), default) }
    }

    // Helper function to safely get JSONObject with both cases
    private fun JSONObject.safeGetJSONObject(key: String): JSONObject? {
        return this.optJSONObject(key) ?: this.optJSONObject(key.lowercase())
    }

    // Helper function to safely get JSONArray with both cases
    private fun JSONObject.safeGetJSONArray(key: String): org.json.JSONArray? {
        return this.optJSONArray(key) ?: this.optJSONArray(key.lowercase())
    }

    // Helper: Send HTTP request with JSON body
    private fun sendRequest(
        urlString: String,
        method: String,
        jsonBody: JSONObject? = null,
        requiresAuth: Boolean = true
    ): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            Log.d(TAG, "=== NETWORK REQUEST ===")
            Log.d(TAG, "URL: $urlString")
            Log.d(TAG, "Method: $method")
            if (jsonBody != null) {
                Log.d(TAG, "Request Body: $jsonBody")
            }

            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.setRequestProperty("Content-Type", "application/json")

            if (requiresAuth) {
                val token = prefs.getAccessToken()
                if (!token.isNullOrEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer $token")
                    Log.d(TAG, "Authorization header added")
                }
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

            Log.d(TAG, "Connecting...")
            val code = conn.responseCode
            Log.d(TAG, "Response Code: $code")

            val input = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = input.bufferedReader().use { it.readText() }

            Log.d(TAG, "Response Body: $text")
            Log.d(TAG, "=== END NETWORK REQUEST ===")

            JSONObject(text)
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "CONNECTION FAILED: ${e.message}")
            null
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "TIMEOUT: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Request failed: ${e.message}", e)
            null
        } finally {
            conn?.disconnect()
        }
    }

    // Parse Booking from JSON
    private fun parseBooking(json: JSONObject): Booking {
        return Booking(
            id = json.optString("Id", ""),
            evOwnerNic = json.optString("EvOwnerNic", ""),
            chargingStationId = json.optString("ChargingStationId", ""),
            chargingStationName = json.optString("ChargingStationName"),
            slotNumber = json.optInt("SlotNumber", 0),
            reservationDateTime = json.optString("ReservationDateTime", ""),
            status = BookingStatus.fromString(json.optString("Status", "Pending")),
            cancellationReason = json.optString("CancellationReason"),
            createdAt = json.optString("CreatedAt", ""),
            updatedAt = json.optString("UpdatedAt")
        )
    }

    // Get available slots
    fun getAvailableSlots(
        stationId: String,
        date: String,   // format: yyyy-MM-dd
        time: String,   // format: HH:mm
        durationInMinutes: Int,
        callback: (success: Boolean, message: String, slots: List<Int>?) -> Unit
    ) {
        Thread {
            val url = "${BASE_URL}api/v1/ChargingStation/$stationId/available-slots?date=$date&time=$time&durationInMinutes=$durationInMinutes"
            Log.d(TAG, "Fetching available slots from: $url")

            val resp = sendRequest(url, "GET")

            if (resp != null) {
                val success = resp.safeGetBoolean("Success")
                val message = resp.safeGetString("Message", "Unknown error")

                // FIXED: Get the nested Data object first
                val dataObject = resp.safeGetJSONObject("Data")
                val slotsArray = dataObject?.safeGetJSONArray("AvailableSlots")

                val slots = if (success && slotsArray != null && slotsArray.length() > 0) {
                    val slotList = (0 until slotsArray.length()).map { slotsArray.getInt(it) }
                    Log.d(TAG, "Parsed available slots: $slotList")
                    slotList
                } else {
                    Log.d(TAG, "No available slots found in response")
                    null
                }

                (context as? android.app.Activity)?.runOnUiThread {
                    callback(success, message, slots)
                } ?: callback(success, message, slots)
            } else {
                Log.e(TAG, "Failed to get response from server")
                val msg = "Cannot connect to server"
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg, null)
                } ?: callback(false, msg, null)
            }
        }.start()
    }

    // Create Booking
    fun createBooking(
        request: CreateBookingRequest,
        callback: (success: Boolean, message: String, booking: Booking?) -> Unit
    ) {
        Thread {
            Log.d(TAG, "Creating booking for station: ${request.chargingStationId}")

            val body = JSONObject().apply {
                put("EvOwnerNic", request.evOwnerNic)
                put("ChargingStationId", request.chargingStationId)
                put("SlotNumber", request.slotNumber)
                put("ReservationDateTime", request.reservationDateTime)
            }

            val resp = sendRequest("${BASE_URL}api/v1/Booking", "POST", body)
            if (resp != null) {
                val success = resp.safeGetBoolean("Success")
                val message = resp.safeGetString("Message", "Unknown error")
                val data = resp.safeGetJSONObject("Data")

                val booking = if (success && data != null) parseBooking(data) else null

                (context as? android.app.Activity)?.runOnUiThread {
                    callback(success, message, booking)
                } ?: callback(success, message, booking)
            } else {
                val msg = "Cannot connect to server"
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg, null)
                } ?: callback(false, msg, null)
            }
        }.start()
    }

    // Get Bookings by EV Owner NIC
    fun getBookingsByEvOwner(
        evOwnerNic: String,
        callback: (success: Boolean, message: String, bookings: List<Booking>?) -> Unit
    ) {
        Thread {
            Log.d(TAG, "Fetching bookings for EV Owner: $evOwnerNic")

            val resp = sendRequest("${BASE_URL}api/v1/Booking/evowner/$evOwnerNic", "GET")
            if (resp != null) {
                val success = resp.safeGetBoolean("Success")
                val message = resp.safeGetString("Message", "Unknown error")
                val dataArray = resp.safeGetJSONArray("Data")

                val bookings = if (success && dataArray != null) {
                    (0 until dataArray.length()).map { i ->
                        parseBooking(dataArray.getJSONObject(i))
                    }
                } else {
                    emptyList()
                }

                (context as? android.app.Activity)?.runOnUiThread {
                    callback(success, message, bookings)
                } ?: callback(success, message, bookings)
            } else {
                val msg = "Cannot connect to server"
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg, null)
                } ?: callback(false, msg, null)
            }
        }.start()
    }

    // Get Single Booking by ID
    fun getBookingById(
        bookingId: String,
        callback: (success: Boolean, message: String, booking: Booking?) -> Unit
    ) {
        Thread {
            Log.d(TAG, "Fetching booking: $bookingId")

            val resp = sendRequest("${BASE_URL}api/v1/Booking/$bookingId", "GET")
            if (resp != null) {
                val success = resp.optBoolean("Success", false)
                val message = resp.optString("Message", "Unknown error")
                val data = resp.optJSONObject("Data")

                val booking = if (success && data != null) parseBooking(data) else null

                (context as? android.app.Activity)?.runOnUiThread {
                    callback(success, message, booking)
                } ?: callback(success, message, booking)
            } else {
                val msg = "Cannot connect to server"
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg, null)
                } ?: callback(false, msg, null)
            }
        }.start()
    }

    // Update Booking
    // Replace the updateBooking method in your BookingRepository class with this:

    // If your API uses PATCH instead of PUT, use this version:

    fun updateBooking(
        bookingId: String,
        reservationDateTime: String,
        callback: (Boolean, String?) -> Unit
    ) {
        Thread {
            Log.d(TAG, "Updating booking: $bookingId with new time: $reservationDateTime")

            val body = JSONObject().apply {
                put("BookingId", bookingId)
                put("NewReservationDateTime", reservationDateTime)
            }

            // Using put method

            val resp = sendRequest("${BASE_URL}api/v1/Booking/update", "PUT", body)

            if (resp != null) {
                val success = resp.safeGetBoolean("Success")
                val message = resp.safeGetString("Message", "Unknown error")

                (context as? android.app.Activity)?.runOnUiThread {
                    callback(success, message)
                } ?: callback(success, message)
            } else {
                val msg = "Cannot connect to server"
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg)
                } ?: callback(false, msg)
            }
        }.start()
    }

    // Cancel Booking
    fun cancelBooking(
        request: CancelBookingRequest,
        callback: (success: Boolean, message: String) -> Unit
    ) {
        Thread {
            Log.d(TAG, "Cancelling booking: ${request.bookingId}")

            val body = JSONObject().apply {
                put("BookingId", request.bookingId)
                put("CancellationReason", request.cancellationReason)
            }

            val resp = sendRequest("${BASE_URL}api/v1/Booking/cancel", "PUT", body)
            if (resp != null) {
                val success = resp.safeGetBoolean("Success")
                val message = resp.safeGetString("Message", "Unknown error")

                (context as? android.app.Activity)?.runOnUiThread {
                    callback(success, message)
                } ?: callback(success, message)
            } else {
                val msg = "Cannot connect to server"
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg)
                } ?: callback(false, msg)
            }
        }.start()
    }

    // Confirm Booking
    fun confirmBooking(
        bookingId: String,
        callback: (success: Boolean, message: String) -> Unit
    ) {
        Thread {
            Log.d(TAG, "Confirming booking: $bookingId")

            val body = JSONObject().apply {
                put("BookingId", bookingId)
            }

            val resp = sendRequest("${BASE_URL}api/v1/Booking/confirm", "PUT", body)
            if (resp != null) {
                val success = resp.optBoolean("Success", false)
                val message = resp.optString("Message", "Unknown error")

                (context as? android.app.Activity)?.runOnUiThread {
                    callback(success, message)
                } ?: callback(success, message)
            } else {
                val msg = "Cannot connect to server"
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg)
                } ?: callback(false, msg)
            }
        }.start()
    }

    // Complete Booking
    fun completeBooking(
        bookingId: String,
        callback: (success: Boolean, message: String) -> Unit
    ) {
        Thread {
            Log.d(TAG, "Completing booking: $bookingId")

            val body = JSONObject().apply {
                put("BookingId", bookingId)
            }

            val resp = sendRequest("${BASE_URL}api/v1/Booking/complete", "PUT", body)
            if (resp != null) {
                val success = resp.safeGetBoolean("Success")
                val message = resp.safeGetString("Message", "Unknown error")

                (context as? android.app.Activity)?.runOnUiThread {
                    callback(success, message)
                } ?: callback(success, message)
            } else {
                val msg = "Cannot connect to server"
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg)
                } ?: callback(false, msg)
            }
        }.start()
    }

    // Get Booking Counts
    fun getBookingCounts(
        callback: (success: Boolean, message: String, counts: BookingCounts?) -> Unit
    ) {
        Thread {
            Log.d(TAG, "Fetching booking counts")

            val resp = sendRequest("${BASE_URL}api/v1/Booking/counts", "GET")
            if (resp != null) {
                val success = resp.safeGetBoolean("Success")
                val message = resp.safeGetString("Message", "Unknown error")
                val data = resp.safeGetJSONObject("Data")

                val counts = if (success && data != null) {
                    BookingCounts(
                        pending = data.optInt("PendingCount", 0),
                        confirmed = data.optInt("ApprovedCount", 0),
                        upcoming = data.optInt("UpcomingCount", 0),
                        completed = data.optInt("CompletedCount", 0),
                        cancelled = data.optInt("CancelledCount", 0)
                    )
                } else null

                (context as? android.app.Activity)?.runOnUiThread {
                    callback(success, message, counts)
                } ?: callback(success, message, counts)
            } else {
                val msg = "Cannot connect to server"
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg, null)
                } ?: callback(false, msg, null)
            }
        }.start()
    }
    // Get Upcoming Bookings (for dashboard)
    fun getUpcomingBookings(
        callback: (success: Boolean, message: String, bookings: List<Booking>?) -> Unit
    ) {
        Thread {
            Log.d(TAG, "Fetching upcoming bookings")

            val resp = sendRequest("${BASE_URL}api/v1/Booking/upcoming", "GET")
            if (resp != null) {
                val success = resp.safeGetBoolean("Success")
                val message = resp.safeGetString("Message", "Unknown error")
                val dataArray = resp.safeGetJSONArray("Data")

                val bookings = if (success && dataArray != null) {
                    (0 until dataArray.length()).map { i ->
                        parseBooking(dataArray.getJSONObject(i))
                    }
                } else {
                    emptyList()
                }

                (context as? android.app.Activity)?.runOnUiThread {
                    callback(success, message, bookings)
                } ?: callback(success, message, bookings)
            } else {
                val msg = "Cannot connect to server"
                (context as? android.app.Activity)?.runOnUiThread {
                    callback(false, msg, null)
                } ?: callback(false, msg, null)
            }
        }.start()
    }
}