package com.example.evchargingstationapp.ui.bookings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.data.repository.BookingRepository
import com.example.evchargingstationapp.data.repository.ChargingStationRepository
import com.example.evchargingstationapp.model.CreateBookingRequest
import java.text.SimpleDateFormat
import java.util.*

class AddBookingActivity : AppCompatActivity() {

    private lateinit var bookingRepository: BookingRepository
    private lateinit var stationRepository: ChargingStationRepository
    private lateinit var prefs: PrefsHelper

    private lateinit var spinnerStations: Spinner
    private lateinit var etSlotNumber: EditText
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button
    private lateinit var tvSelectedDateTime: TextView
    private lateinit var btnCreateBooking: Button
    private lateinit var progressBar: ProgressBar

    private var selectedDate: Calendar = Calendar.getInstance()
    private var stationIds = mutableListOf<String>()
    private var stationNames = mutableListOf<String>()

    private var preselectedStationName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_booking)

        preselectedStationName = intent.getStringExtra("STATION_NAME")

        bookingRepository = BookingRepository(this)
        stationRepository = ChargingStationRepository(this)
        prefs = PrefsHelper(this)

        // Debug: Check if user is logged in
        val token = prefs.getAccessToken()
        val nic = prefs.getNic()
        android.util.Log.d("AddBookingActivity", "Token exists: ${!token.isNullOrEmpty()}")
        android.util.Log.d("AddBookingActivity", "NIC: $nic")

        // Initialize views
        spinnerStations = findViewById(R.id.spinnerStations)
        etSlotNumber = findViewById(R.id.etSlotNumber)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectTime = findViewById(R.id.btnSelectTime)
        tvSelectedDateTime = findViewById(R.id.tvSelectedDateTime)
        btnCreateBooking = findViewById(R.id.btnCreateBooking)
        progressBar = findViewById(R.id.progressBar)

        // Load charging stations
        loadChargingStations()

        // Date picker
        btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        // Time picker
        btnSelectTime.setOnClickListener {
            showTimePicker()
        }

        // Create booking
        btnCreateBooking.setOnClickListener {
            createBooking()
        }

        // Update initial date/time display
        updateDateTimeDisplay()
    }

    private fun loadChargingStations() {
        progressBar.visibility = View.VISIBLE

        stationRepository.getAllStations { success, message, stations ->
            progressBar.visibility = View.GONE

            if (success && stations != null && stations.isNotEmpty()) {
                stationIds.clear()
                stationNames.clear()

                // Filter only active stations
                val activeStations = stations.filter { it.status == "Active" }

                activeStations.forEach { station ->
                    stationIds.add(station.id)
                    stationNames.add(station.name)
                }

                if (stationNames.isNotEmpty()) {
                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        stationNames
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerStations.adapter = adapter

                    // Preselect station if coming from map
                    // Preselect station if coming from map using ID
                    val preselectedStationId = intent.getStringExtra("STATION_ID")
                    preselectedStationId?.let { id ->
                        val index = stationIds.indexOf(id)
                        if (index >= 0) {
                            spinnerStations.setSelection(index)
                        }
                    }
                }
                else {
                    Toast.makeText(this, "No active charging stations available", Toast.LENGTH_LONG).show()
                    // For testing, add a dummy station
                    addDummyStation()
                }
            } else {
                Toast.makeText(this, "Failed to load stations: $message", Toast.LENGTH_LONG).show()
                // For testing purposes, add a dummy station
                addDummyStation()
            }
        }
    }

    // Temporary function for testing when API fails
    private fun addDummyStation() {
        stationIds.clear()
        stationNames.clear()

        // Add a test station ID (replace with actual station ID from your backend)
        stationIds.add("test-station-id-123")
        stationNames.add("Test Station 1")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            stationNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStations.adapter = adapter

        Toast.makeText(this, "Using test station (API unavailable)", Toast.LENGTH_SHORT).show()
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateTimeDisplay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDate.set(Calendar.MINUTE, minute)
                updateDateTimeDisplay()
            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateDateTimeDisplay() {
        val displayFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        tvSelectedDateTime.text = displayFormat.format(selectedDate.time)
    }

    private fun createBooking() {
        // Validation
        if (stationIds.isEmpty()) {
            Toast.makeText(this, "No charging stations available", Toast.LENGTH_SHORT).show()
            return
        }

        val slotNumberText = etSlotNumber.text.toString().trim()
        if (slotNumberText.isEmpty()) {
            etSlotNumber.error = "Please enter slot number"
            return
        }

        val slotNumber = slotNumberText.toIntOrNull()
        if (slotNumber == null || slotNumber < 1) {
            etSlotNumber.error = "Invalid slot number"
            return
        }

        val userNic = prefs.getNic()
        if (userNic.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPosition = spinnerStations.selectedItemPosition
        val stationId = stationIds[selectedPosition]

        // Format date/time for API (ISO 8601)
        val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val reservationDateTime = apiFormat.format(selectedDate.time)

        // Create booking request
        val request = CreateBookingRequest(
            evOwnerNic = userNic,
            chargingStationId = stationId,
            slotNumber = slotNumber,
            reservationDateTime = reservationDateTime
        )

        // Disable button and show progress
        btnCreateBooking.isEnabled = false
        progressBar.visibility = View.VISIBLE

        bookingRepository.createBooking(request) { success, message, booking ->
            btnCreateBooking.isEnabled = true
            progressBar.visibility = View.GONE

            if (success && booking != null) {
                Toast.makeText(this, "Booking created successfully!", Toast.LENGTH_SHORT).show()

                // Navigate to booking detail
                val intent = Intent(this, BookingDetailActivity::class.java)
                intent.putExtra("BOOKING_ID", booking.id)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to create booking: $message", Toast.LENGTH_LONG).show()
            }
        }
    }
}