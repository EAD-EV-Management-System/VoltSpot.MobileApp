package com.example.evchargingstationapp.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.data.repository.BookingRepository
import com.example.evchargingstationapp.ui.bookings.BookingAdapter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Calendar

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var tvGreeting: TextView
    private lateinit var tvPending: TextView
    private lateinit var tvApproved: TextView
    private lateinit var recyclerUpcoming: RecyclerView
    private lateinit var bookingRepository: BookingRepository
    private lateinit var prefs: PrefsHelper
    private lateinit var bookingAdapter: BookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        bookingRepository = BookingRepository(requireContext())
        prefs = PrefsHelper(requireContext())

        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvPending = view.findViewById(R.id.tvPendingReservations)
        tvApproved = view.findViewById(R.id.tvApprovedReservations)
        recyclerUpcoming = view.findViewById(R.id.recyclerUpcoming)
        mapView = view.findViewById(R.id.mapView)

        setGreeting()
        setupRecycler()
        setupMap(savedInstanceState)

        // Load booking counts and upcoming bookings
        loadBookingCounts()
        loadUpcomingBookings()

        view.findViewById<TextView>(R.id.tvSeeAll).setOnClickListener {
            // Navigate to BookingFragment (which has the tabs)
            (requireActivity() as? MainActivity)?.navigateToBookings()
        }

        return view
    }

    private fun setupRecycler() {
        recyclerUpcoming.layoutManager = LinearLayoutManager(context)
        bookingAdapter = BookingAdapter(emptyList(), requireContext())
        recyclerUpcoming.adapter = bookingAdapter
    }

    private fun loadBookingCounts() {
        bookingRepository.getBookingCounts { success, message, counts ->
            if (success && counts != null) {
                tvPending.text = "Pending: ${counts.pending}"
                tvApproved.text = "Confirmed: ${counts.confirmed}"
            } else {
                // Show default values on error
                tvPending.text = "Pending: 0"
                tvApproved.text = "Confirmed: 0"
            }
        }
    }

    private fun loadUpcomingBookings() {
        val userNic = prefs.getNic()
        if (userNic.isNullOrEmpty()) {
            return
        }

        bookingRepository.getUpcomingBookings { success, message, bookings ->
            if (success && bookings != null) {
                // Show only first 3 bookings in dashboard
                val limitedBookings = bookings.take(3)
                bookingAdapter.updateBookings(limitedBookings)
            } else {
                // Show empty state or error
                if (!success) {
                    Toast.makeText(context, "Failed to load bookings", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        googleMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))

                // Add a few mock nearby stations
                val nearbyStations = listOf(
                    LatLng(it.latitude + 0.002, it.longitude + 0.002),
                    LatLng(it.latitude - 0.002, it.longitude - 0.002)
                )
                nearbyStations.forEachIndexed { i, loc ->
                    googleMap.addMarker(MarkerOptions().position(loc).title("Station ${i + 1}"))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        // Refresh data when returning to home
        loadBookingCounts()
        loadUpcomingBookings()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}