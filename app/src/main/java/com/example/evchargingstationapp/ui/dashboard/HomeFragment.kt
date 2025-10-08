package com.example.evchargingstationapp.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.ui.bookings.BookingAdapter
import com.example.evchargingstationapp.ui.bookings.UpcomingBookingsFragment
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvPending = view.findViewById(R.id.tvPendingReservations)
        tvApproved = view.findViewById(R.id.tvApprovedReservations)
        recyclerUpcoming = view.findViewById(R.id.recyclerUpcoming)
        mapView = view.findViewById(R.id.mapView)

        setGreeting()
        setupRecycler()
        setupMap(savedInstanceState)
        mockReservationCounts()

        view.findViewById<TextView>(R.id.tvSeeAll).setOnClickListener {
            // Navigate to upcoming bookings
            (parentFragmentManager.beginTransaction())
                .replace(R.id.fragment_container, UpcomingBookingsFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun setupRecycler() {
        recyclerUpcoming.layoutManager = LinearLayoutManager(context)
        recyclerUpcoming.adapter = BookingAdapter(
            listOf("Reservation A", "Reservation B", "Reservation C"),
            requireContext()
        )
    }

    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    private fun mockReservationCounts() {
        // You can replace this later with SQLite or backend
        tvPending.text = "Pending: 2"
        tvApproved.text = "Approved: 4"
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

    // Forward lifecycle events to mapView
    override fun onResume() {
        super.onResume()
        mapView.onResume()
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
