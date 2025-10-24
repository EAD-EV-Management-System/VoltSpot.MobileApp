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
    private lateinit var btnSelectDate: Button
    private lateinit var btnSelectTime: Button
    private lateinit var tvSelectedDateTime: TextView
    private lateinit var btnCreateBooking: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var spinnerDuration: Spinner
    private lateinit var spinnerAvailableSlots: Spinner

    private var selectedDate: Calendar = Calendar.getInstance()
    private var stationIds = mutableListOf<String>()
    private var stationNames = mutableListOf<String>()
    private var availableSlotsList = mutableListOf<String>()
    private var durationInMinutes: Int = 60 // default duration
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
        btnSelectDate = findViewById(R.id.btnSelectDate)
        btnSelectTime = findViewById(R.id.btnSelectTime)
        tvSelectedDateTime = findViewById(R.id.tvSelectedDateTime)
        btnCreateBooking = findViewById(R.id.btnCreateBooking)
        progressBar = findViewById(R.id.progressBar)
        spinnerDuration = findViewById(R.id.spinnerDuration)
        spinnerAvailableSlots = findViewById(R.id.spinnerAvailableSlots)

        // Load charging stations
        loadChargingStations()

        // Date picker
        btnSelectDate.setOnClickListener { showDatePicker() }

        // Time picker
        btnSelectTime.setOnClickListener { showTimePicker() }

        // Create booking
        btnCreateBooking.setOnClickListener { createBooking() }

        // Setup duration spinner
        val durations = listOf(30, 60, 90, 120)
        val durationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durations)
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDuration.adapter = durationAdapter
        spinnerDuration.setSelection(durations.indexOf(durationInMinutes))

        spinnerDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                durationInMinutes = durations[position]
                fetchAvailableSlots()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
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

                    // Preselect station if coming from map using ID
                    val preselectedStationId = intent.getStringExtra("STATION_ID")
                    preselectedStationId?.let { id ->
                        val index = stationIds.indexOf(id)
                        if (index >= 0) spinnerStations.setSelection(index)
                    }

                    spinnerStations.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            fetchAvailableSlots()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }

                } else {
                    Toast.makeText(this, "No active charging stations available", Toast.LENGTH_LONG).show()
                    addDummyStation()
                }
            } else {
                Toast.makeText(this, "Failed to load stations: $message", Toast.LENGTH_LONG).show()
                addDummyStation()
            }
        }
    }

    private fun addDummyStation() {
        stationIds.clear()
        stationNames.clear()
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
                fetchAvailableSlots()
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
                fetchAvailableSlots()
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

    private fun fetchAvailableSlots() {
        val selectedPosition = spinnerStations.selectedItemPosition
        if (selectedPosition < 0 || stationIds.isEmpty()) return

        val stationId = stationIds[selectedPosition]
        val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = apiFormat.format(selectedDate.time)
        val time = timeFormat.format(selectedDate.time)

        bookingRepository.getAvailableSlots(stationId, date, time, durationInMinutes) { success, message, slots ->
            if (success && slots != null && slots.isNotEmpty()) {

                android.util.Log.d("AddBookingActivity", "Available slots from API: $slots")

                availableSlotsList.clear()
                availableSlotsList.addAll(slots.map { it.toString() })

                val displayList = mutableListOf("Select a slot")
                displayList.addAll(availableSlotsList)

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerAvailableSlots.adapter = adapter

                // Ensure spinner is enabled after adapter set
                spinnerAvailableSlots.isEnabled = true
                btnCreateBooking.isEnabled = false

                spinnerAvailableSlots.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        btnCreateBooking.isEnabled = position != 0
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {
                        btnCreateBooking.isEnabled = false
                    }
                }

            } else {
                availableSlotsList.clear()
                spinnerAvailableSlots.adapter = null
                spinnerAvailableSlots.isEnabled = false
                btnCreateBooking.isEnabled = false
                Toast.makeText(this, "No slots available for selected time/duration", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createBooking() {
        // Validation
        if (stationIds.isEmpty()) {
            Toast.makeText(this, "No charging stations available", Toast.LENGTH_SHORT).show()
            return
        }

        val userNic = prefs.getNic()
        if (userNic.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedStationPosition = spinnerStations.selectedItemPosition
        if (selectedStationPosition < 0) return
        val stationId = stationIds[selectedStationPosition]

        if (availableSlotsList.isEmpty()) {
            Toast.makeText(this, "Please select an available slot", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedSlotPosition = spinnerAvailableSlots.selectedItemPosition
        if (selectedSlotPosition < 0) return
        val slotNumber = availableSlotsList[selectedSlotPosition].toInt()

        // Format date/time for API (ISO 8601)
        val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val reservationDateTime = apiFormat.format(selectedDate.time)

        // Create booking request
        val request = CreateBookingRequest(
            evOwnerNic = userNic,
            chargingStationId = stationId,
            slotNumber = slotNumber,
            reservationDateTime = reservationDateTime,
            durationInMinutes = durationInMinutes
        )

        // Disable button and show progress
        btnCreateBooking.isEnabled = false
        progressBar.visibility = View.VISIBLE

        bookingRepository.createBooking(request) { success, message, booking ->
            btnCreateBooking.isEnabled = true
            progressBar.visibility = View.GONE

            if (success && booking != null) {
                Toast.makeText(this, "Booking created successfully!", Toast.LENGTH_SHORT).show()
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
