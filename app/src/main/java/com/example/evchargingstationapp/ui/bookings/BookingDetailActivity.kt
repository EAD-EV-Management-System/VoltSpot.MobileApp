package com.example.evchargingstationapp.ui.bookings

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.repository.BookingRepository
import com.example.evchargingstationapp.model.Booking
import com.example.evchargingstationapp.model.BookingStatus
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.ContextCompat

class BookingDetailActivity : AppCompatActivity() {

    private lateinit var bookingRepository: BookingRepository
    private lateinit var tvStationName: TextView
    private lateinit var tvSlotNumber: TextView
    private lateinit var tvReservationDate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvBookingId: TextView
    private lateinit var qrImage: ImageView
    private lateinit var btnComplete: Button
    private lateinit var loadingLayout: View

    private var currentBooking: Booking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_detail)

        bookingRepository = BookingRepository(this)

        // Initialize views
        tvStationName = findViewById(R.id.tvStationName)
        tvSlotNumber = findViewById(R.id.tvSlotNumber)
        tvReservationDate = findViewById(R.id.tvReservationDate)
        tvStatus = findViewById(R.id.tvStatus)
        tvBookingId = findViewById(R.id.tvBookingId)
        qrImage = findViewById(R.id.qrImage)
        btnComplete = findViewById(R.id.btnComplete)
        loadingLayout = findViewById(R.id.loadingLayout)

        val bookingId = intent.getStringExtra("BOOKING_ID")
        if (bookingId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid booking", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadBookingDetails(bookingId)

        btnComplete.setOnClickListener {
            currentBooking?.let { booking ->
                completeBooking(booking.id)
            }
        }
    }

    private fun loadBookingDetails(bookingId: String) {
        loadingLayout.visibility = View.VISIBLE

        bookingRepository.getBookingById(bookingId) { success, message, booking ->
            loadingLayout.visibility = View.GONE

            if (success && booking != null) {
                currentBooking = booking
                displayBookingDetails(booking)
            } else {
                Toast.makeText(this, "Failed to load booking: $message", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun displayBookingDetails(booking: Booking) {
        tvBookingId.text = "Booking ID: ${booking.id}"
        tvStationName.text = booking.chargingStationName ?: "Station ${booking.chargingStationId}"
        tvSlotNumber.text = "Slot Number: ${booking.slotNumber}"
        tvReservationDate.text = "Date: ${formatDateTime(booking.reservationDateTime)}"
        tvStatus.text = "Status: ${booking.status.name}"

        // Set status color
        val statusColor = when (booking.status) {
            BookingStatus.Pending -> ContextCompat.getColor(this, R.color.statusPending)
            BookingStatus.Confirmed -> ContextCompat.getColor(this, R.color.statusConfirmed)
            BookingStatus.Completed -> ContextCompat.getColor(this, R.color.statusCompleted)
            BookingStatus.Cancelled -> ContextCompat.getColor(this, R.color.statusCancelled)
        }
        tvStatus.setTextColor(statusColor)

        // Show complete button only for confirmed bookings
        if (booking.status == BookingStatus.Confirmed) {
            btnComplete.visibility = View.VISIBLE
        } else {
            btnComplete.visibility = View.GONE
        }

        // Generate QR code
        generateQRCode(booking.qrCodeId ?: booking.id, qrImage)
    }

    private fun completeBooking(bookingId: String) {
        btnComplete.isEnabled = false
        Toast.makeText(this, "Completing booking...", Toast.LENGTH_SHORT).show()

        bookingRepository.completeBooking(bookingId) { success, message ->
            btnComplete.isEnabled = true

            if (success) {
                Toast.makeText(this, "Booking completed successfully!", Toast.LENGTH_SHORT).show()
                // Reload the booking details to update status
                loadBookingDetails(bookingId)
            } else {
                Toast.makeText(this, "Failed to complete: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun generateQRCode(text: String, imageView: ImageView) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val bmp = createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) {
                for (y in 0 until 512) {
                    bmp[x, y] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            imageView.setImageBitmap(bmp)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDateTime(isoDateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(isoDateTime)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            isoDateTime
        }
    }
}