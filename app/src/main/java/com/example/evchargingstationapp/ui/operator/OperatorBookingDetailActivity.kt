package com.example.evchargingstationapp.ui.operator

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.repository.BookingRepository
import com.example.evchargingstationapp.model.Booking
import com.example.evchargingstationapp.model.BookingStatus
import com.example.evchargingstationapp.model.CancelBookingRequest
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.*

class OperatorBookingDetailActivity : AppCompatActivity() {

    private lateinit var bookingRepository: BookingRepository
    private lateinit var loadingLayout: LinearLayout
    private lateinit var tvBookingId: TextView
    private lateinit var tvEvOwnerNic: TextView
    private lateinit var tvStationName: TextView
    private lateinit var tvSlotNumber: TextView
    private lateinit var tvReservationDate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var qrImage: ImageView
    private lateinit var actionButtonsLayout: LinearLayout
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button

    private var bookingId: String? = null
    private var currentBooking: Booking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_booking_detail)

        bookingRepository = BookingRepository(this)
        bookingId = intent.getStringExtra("BOOKING_ID")

        initViews()
        setupClickListeners()
        loadBookingDetails()
    }

    private fun initViews() {
        loadingLayout = findViewById(R.id.loadingLayout)
        tvBookingId = findViewById(R.id.tvBookingId)
        tvEvOwnerNic = findViewById(R.id.tvEvOwnerNic)
        tvStationName = findViewById(R.id.tvStationName)
        tvSlotNumber = findViewById(R.id.tvSlotNumber)
        tvReservationDate = findViewById(R.id.tvReservationDate)
        tvStatus = findViewById(R.id.tvStatus)
        qrImage = findViewById(R.id.qrImage)
        actionButtonsLayout = findViewById(R.id.actionButtonsLayout)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupClickListeners() {
        btnConfirm.setOnClickListener { confirmBooking() }
        btnCancel.setOnClickListener { showCancelDialog() }
    }

    private fun loadBookingDetails() {
        if (bookingId == null) {
            Toast.makeText(this, "Invalid booking ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadingLayout.visibility = View.VISIBLE

        bookingRepository.getBookingById(bookingId!!) { success, message, booking ->
            loadingLayout.visibility = View.GONE

            if (success && booking != null) {
                currentBooking = booking
                displayBookingDetails(booking)
                generateQRCode(booking.id)
            } else {
                Toast.makeText(this, "Error loading booking: $message", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayBookingDetails(booking: Booking) {
        tvBookingId.text = "Booking ID: ${booking.id}"
        tvEvOwnerNic.text = "Customer: ${booking.evOwnerNic}"
        tvStationName.text = booking.chargingStationName ?: booking.chargingStationId
        tvSlotNumber.text = "Slot: ${booking.slotNumber}"

        val formattedDate = formatDateTime(booking.reservationDateTime)
        tvReservationDate.text = "Date: $formattedDate"

        tvStatus.text = "Status: ${booking.status.name}"

        // Set status color
        val statusColor = when (booking.status) {
            BookingStatus.Pending -> getColor(R.color.statusPending)
            BookingStatus.Confirmed -> getColor(R.color.statusConfirmed)
            BookingStatus.Completed -> getColor(R.color.statusCompleted)
            BookingStatus.Cancelled -> getColor(R.color.statusCancelled)
        }
        tvStatus.setTextColor(statusColor)

        // Show action buttons only for Pending status
        if (booking.status == BookingStatus.Pending) {
            actionButtonsLayout.visibility = View.VISIBLE
        } else {
            actionButtonsLayout.visibility = View.GONE
        }
    }

    private fun generateQRCode(bookingId: String) {
        try {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(bookingId, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }

            qrImage.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmBooking() {
        if (bookingId == null) return

        AlertDialog.Builder(this)
            .setTitle("Confirm Booking")
            .setMessage("Are you sure you want to confirm this booking?")
            .setPositiveButton("Confirm") { _, _ ->
                performConfirmBooking()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performConfirmBooking() {
        loadingLayout.visibility = View.VISIBLE
        btnConfirm.isEnabled = false

        bookingRepository.confirmBooking(bookingId!!) { success, message ->
            loadingLayout.visibility = View.GONE
            btnConfirm.isEnabled = true

            if (success) {
                Toast.makeText(this, "Booking confirmed successfully", Toast.LENGTH_SHORT).show()
                loadBookingDetails() // Refresh to update UI
            } else {
                Toast.makeText(this, "Failed to confirm: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showCancelDialog() {
        val input = EditText(this)
        input.hint = "Cancellation reason"

        AlertDialog.Builder(this)
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?")
            .setView(input)
            .setPositiveButton("Cancel Booking") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Please provide a reason", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                performCancelBooking(reason)
            }
            .setNegativeButton("Back", null)
            .show()
    }

    private fun performCancelBooking(reason: String) {
        if (bookingId == null) return

        loadingLayout.visibility = View.VISIBLE
        btnCancel.isEnabled = false

        val request = CancelBookingRequest(bookingId!!, reason)
        bookingRepository.cancelBooking(request) { success, message ->
            loadingLayout.visibility = View.GONE
            btnCancel.isEnabled = true

            if (success) {
                Toast.makeText(this, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                loadBookingDetails() // Refresh to update UI
            } else {
                Toast.makeText(this, "Failed to cancel: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun formatDateTime(isoDateTime: String?): String {
        if (isoDateTime.isNullOrEmpty()) return "Unknown date"
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