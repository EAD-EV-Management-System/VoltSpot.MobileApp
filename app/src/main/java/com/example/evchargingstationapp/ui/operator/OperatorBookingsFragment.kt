package com.example.evchargingstationapp.ui.operator

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.data.repository.OperatorRepository
import com.example.evchargingstationapp.model.Booking
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class OperatorBookingsFragment : Fragment() {

    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabScanQR: FloatingActionButton
    private lateinit var operatorRepository: OperatorRepository
    private lateinit var prefs: PrefsHelper
    private lateinit var adapter: OperatorBookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_operator_bookings, container, false)

        shimmerLayout = view.findViewById(R.id.shimmerLayout)
        recyclerView = view.findViewById(R.id.recyclerView)
        fabScanQR = view.findViewById(R.id.fabScanQR)

        recyclerView.layoutManager = LinearLayoutManager(context)

        operatorRepository = OperatorRepository(requireContext())
        prefs = PrefsHelper(requireContext())

        // Initialize adapter with click callback
        adapter = OperatorBookingAdapter(
            emptyList(),
            requireContext(),
            onBookingClick = { booking -> openBookingDetail(booking) }
        )
        recyclerView.adapter = adapter

        // Setup FAB click listener for QR scanner
        fabScanQR.setOnClickListener {
            openQRScanner()
        }

        loadBookings()

        return view
    }

    private fun loadBookings() {
        shimmerLayout.startShimmer()
        shimmerLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        // Get operator ID from preferences
        val operatorId = prefs.getOperatorId()
        if (operatorId.isNullOrEmpty()) {
            Toast.makeText(context, "Operator not logged in", Toast.LENGTH_SHORT).show()
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            return
        }

        android.util.Log.d("OperatorBookingsFragment", "Loading bookings for operator ID: $operatorId")

        operatorRepository.getBookings(operatorId) { success, message, bookings ->
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            if (success && bookings != null) {
                adapter.updateBookings(bookings)

                if (bookings.isEmpty()) {
                    Toast.makeText(context, "No bookings found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Failed to load bookings: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openBookingDetail(booking: Booking) {
        val intent = Intent(requireContext(), OperatorBookingDetailActivity::class.java)
        intent.putExtra("BOOKING_ID", booking.id)
        startActivity(intent)
    }

    private fun openQRScanner() {
        val intent = Intent(requireContext(), QRScannerActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadBookings() // Refresh when fragment becomes visible
    }
}