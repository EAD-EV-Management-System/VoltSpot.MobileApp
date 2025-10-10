package com.example.evchargingstationapp.ui.operator

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

class OperatorBookingDetailActivity : AppCompatActivity() {

    private lateinit var bookingRepository: BookingRepository
    private lateinit var tvBookingId: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerNic: TextView
    private lateinit var tvStationName: TextView
    private lateinit var tvSlotNumber: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var tvStatus: TextView
    private lateinit var ivQRCode: ImageView
    private lateinit var btnConfirm: Button

    private var currentBooking: Booking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_booking_detail)

        bookingRepository = BookingRepository(this)

        // Initialize views
        tvBookingId = findViewById(R.id.tvBookingId)
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvCustomerNic = findViewById(R.id.tvCustomerNic)
        tvStationName = findViewById(R.id.tvStationName)
        tvSlotNumber = findViewById(R.id.tvSlotNumber)
        tvDateTime = findViewById(R.id.tvDateTime)
        tvStatus = findViewById(R.id.tvStatus)
        ivQRCode = findViewById(R.id.ivQRCode)
        btnConfirm = findViewById(R.id.btnConfirm)

        val bookingId = intent.getStringExtra("BOOKING_ID")
        if (bookingId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid booking", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadBookingDetails(bookingId)

        btnConfirm.setOnClickListener {
            currentBooking?.let { booking ->
                confirmBooking(booking.id)
            }
        }
    }

    private fun loadBookingDetails(bookingId: String) {
        bookingRepository.getBookingById(bookingId) { success, message, booking ->
            if (success && booking != null) {
                currentBooking = booking
                displayBookingDetails(booking)
            } else {
                Toast.makeText(this, "Failed to load: $message", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun displayBookingDetails(booking: Booking) {
        tvBookingId.text = "Booking ID: #${booking.id.takeLast(8)}"
        tvCustomerName.text = "Name: Customer (${booking.evOwnerNic})"
        tvCustomerNic.text = "NIC: ${booking.evOwnerNic}"
        tvStationName.text = "Station: ${booking.chargingStationName ?: "Unknown"}"
        tvSlotNumber.text = "Slot: ${booking.slotNumber}"
        tvDateTime.text = "Date: ${formatDateTime(booking.reservationDateTime)}"
        tvStatus.text = "Status: ${booking.status.name}"

        // Status color
        val statusColor = when (booking.status) {
            BookingStatus.Pending -> ContextCompat.getColor(this, R.color.statusPending)
            BookingStatus.Confirmed -> ContextCompat.getColor(this, R.color.statusConfirmed)
            BookingStatus.Completed -> ContextCompat.getColor(this, R.color.statusCompleted)
            BookingStatus.Cancelled -> ContextCompat.getColor(this, R.color.statusCancelled)
        }
        tvStatus.setTextColor(statusColor)

        // Show confirm button only for pending bookings
        if (booking.status == BookingStatus.Pending) {
            btnConfirm.visibility = View.VISIBLE
        } else {
            btnConfirm.visibility = View.GONE
        }

        // Generate QR code
        generateQRCode(booking.id, ivQRCode)
    }

    private fun confirmBooking(bookingId: String) {
        btnConfirm.isEnabled = false
        Toast.makeText(this, "Confirming booking...", Toast.LENGTH_SHORT).show()

        bookingRepository.confirmBooking(bookingId) { success, message ->
            btnConfirm.isEnabled = true

            if (success) {
                Toast.makeText(this, "Booking confirmed!", Toast.LENGTH_SHORT).show()
                // Reload to update status
                loadBookingDetails(bookingId)
            } else {
                Toast.makeText(this, "Failed: $message", Toast.LENGTH_LONG).show()
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