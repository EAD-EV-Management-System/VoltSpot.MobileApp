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
import com.example.evchargingstationapp.ui.bookings.BookingAdapter
import com.example.evchargingstationapp.data.repository.BookingRepository
import com.example.evchargingstationapp.data.repository.ChargingStationRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.content.Intent
import com.example.evchargingstationapp.model.ChargingStation
import com.example.evchargingstationapp.ui.bookings.AddBookingActivity
import com.example.evchargingstationapp.utils.createMarkerWithLabel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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

    private lateinit var chargingStationRepository: ChargingStationRepository


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize repositories and prefs
        bookingRepository = BookingRepository(requireContext())
        prefs = PrefsHelper(requireContext())
        chargingStationRepository = ChargingStationRepository(requireContext())

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

        googleMap.setOnMarkerClickListener { marker ->
            val station = marker.tag as? ChargingStation // set the tag when adding markers
            if (station != null) {
                val intent = Intent(requireContext(), AddBookingActivity::class.java)
                intent.putExtra("STATION_ID", station.id)        // pass ID
                intent.putExtra("STATION_NAME", station.name)    // pass name for display
                startActivity(intent)
            }
            true
        }


        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        googleMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
                googleMap.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))

                // Fetch stations only if logged in
                if (prefs.isLoggedIn()) {
                    fetchChargingStations()
                } else {
                    Toast.makeText(context, "Login to see charging stations", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchChargingStations() {
        chargingStationRepository.getAllStations { success, message, stations ->
            if (success && stations != null) {
                stations.forEach { station ->
                    val position = LatLng(station.latitude, station.longitude)
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .icon(createMarkerWithLabel(requireContext(), station.name))
                            .anchor(0.5f, 1f) // anchor bottom-center
                    )
                    marker?.tag = station // store the station object in the marker
                    marker?.showInfoWindow()

                }
            } else {
                Toast.makeText(context, "Failed to load charging stations: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mapView.getMapAsync(this) // retry loading map now that permission is granted
        } else {
            Toast.makeText(context, "Location permission is required to show nearby stations", Toast.LENGTH_SHORT).show()
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