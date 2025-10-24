package com.example.evchargingstationapp.ui.bookings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.repository.BookingRepository
import com.example.evchargingstationapp.model.Booking
import java.text.SimpleDateFormat
import java.util.*

class UpdateBookingActivity : AppCompatActivity() {

    private lateinit var bookingRepository: BookingRepository

    private lateinit var tvBookingId: TextView
    private lateinit var tvStationName: TextView
    private lateinit var tvSlotNumber: TextView
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button
    private lateinit var tvSelectedDateTime: TextView
    private lateinit var btnUpdateBooking: Button
    private lateinit var progressBar: ProgressBar

    private var bookingId: String? = null
    private var currentBooking: Booking? = null

    private var selectedDate: Calendar = Calendar.getInstance()
    private var dateSelected = false
    private var timeSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.update_booking_layout)

        // Initialize repository
        bookingRepository = BookingRepository(this)

        // Get booking ID from intent
        bookingId = intent.getStringExtra("BOOKING_ID")

        // Initialize views
        initViews()

        // Setup click listeners
        setupClickListeners()

        // Load booking data
        loadBookingData()
    }

    private fun initViews() {
        tvBookingId = findViewById(R.id.tvBookingId)
        tvStationName = findViewById(R.id.tvStationName)
        tvSlotNumber = findViewById(R.id.tvSlotNumber)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectTime = findViewById(R.id.btnSelectTime)
        tvSelectedDateTime = findViewById(R.id.tvSelectedDateTime)
        btnUpdateBooking = findViewById(R.id.btnUpdateBooking)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnSelectDate.setOnClickListener { showDatePicker() }
        btnSelectTime.setOnClickListener { showTimePicker() }
        btnUpdateBooking.setOnClickListener { updateBooking() }
    }

    private fun loadBookingData() {
        if (bookingId == null) {
            Toast.makeText(this, "Invalid booking ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE

        bookingRepository.getBookingById(bookingId!!) { success, message, booking ->
            progressBar.visibility = View.GONE

            if (success && booking != null) {
                currentBooking = booking
                populateFields()
            } else {
                Toast.makeText(this, "Error loading booking: $message", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun populateFields() {
        currentBooking?.let { booking ->
            // Display booking details
            tvBookingId.text = booking.id
            tvStationName.text = booking.chargingStationName ?: booking.chargingStationId
            tvSlotNumber.text = "Slot ${booking.slotNumber}"

            // Parse and set the current reservation date/time
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(booking.reservationDateTime)
                if (date != null) {
                    selectedDate.time = date
                    dateSelected = true
                    timeSelected = true
                    updateDateTimeDisplay()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("UpdateBookingActivity", "Error parsing date: ${e.message}")
            }
        }
    }

    private fun showDatePicker() {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate.set(Calendar.YEAR, selectedYear)
                selectedDate.set(Calendar.MONTH, selectedMonth)
                selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay)
                dateSelected = true
                updateDateTimeDisplay()
            },
            year, month, day
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        if (!dateSelected) {
            Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show()
            return
        }

        val hour = selectedDate.get(Calendar.HOUR_OF_DAY)
        val minute = selectedDate.get(Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedDate.set(Calendar.MINUTE, selectedMinute)
                selectedDate.set(Calendar.SECOND, 0)
                timeSelected = true
                updateDateTimeDisplay()
            },
            hour, minute, true // true = 24 hour format
        ).show()
    }

    private fun updateDateTimeDisplay() {
        if (dateSelected && timeSelected) {
            val displayFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            tvSelectedDateTime.text = displayFormat.format(selectedDate.time)
        } else if (dateSelected) {
            val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            tvSelectedDateTime.text = "${displayFormat.format(selectedDate.time)} - Select time"
        } else {
            tvSelectedDateTime.text = "Not selected"
        }
    }

    private fun updateBooking() {
        // Validate date and time are selected
        if (!dateSelected || !timeSelected) {
            Toast.makeText(this, "Please select both date and time", Toast.LENGTH_SHORT).show()
            return
        }

        // Format the selected date time for API (ISO 8601)
        val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val reservationDateTime = apiFormat.format(selectedDate.time)

        // Disable button and show progress
        btnUpdateBooking.isEnabled = false
        progressBar.visibility = View.VISIBLE

        // Call repository to update booking
        bookingRepository.updateBooking(bookingId!!, reservationDateTime) { success, message ->
            // Re-enable button and hide progress
            btnUpdateBooking.isEnabled = true
            progressBar.visibility = View.GONE

            if (success) {
                Toast.makeText(this, "Booking time updated successfully!", Toast.LENGTH_SHORT).show()

                // Navigate back to booking detail
                val intent = Intent(this, BookingDetailActivity::class.java)
                intent.putExtra("BOOKING_ID", bookingId)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to update booking: $message", Toast.LENGTH_LONG).show()
            }
        }
    }
}