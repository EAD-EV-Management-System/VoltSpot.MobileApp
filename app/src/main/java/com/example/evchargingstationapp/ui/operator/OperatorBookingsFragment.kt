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
import com.example.evchargingstationapp.data.repository.BookingRepository
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class OperatorBookingsFragment : Fragment() {

    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabScanQR: FloatingActionButton
    private lateinit var bookingRepository: BookingRepository
    private lateinit var adapter: OperatorBookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_operator_bookings, container, false)

        bookingRepository = BookingRepository(requireContext())

        shimmerLayout = view.findViewById(R.id.shimmerLayout)
        recyclerView = view.findViewById(R.id.recyclerView)
        fabScanQR = view.findViewById(R.id.fabScanQR)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = OperatorBookingAdapter(emptyList(), requireContext()) { booking ->
            // Navigate to detail
            val intent = Intent(requireContext(), OperatorBookingDetailActivity::class.java)
            intent.putExtra("BOOKING_ID", booking.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // FAB for QR Scanner
        fabScanQR.setOnClickListener {
            val intent = Intent(requireContext(), QRScannerActivity::class.java)
            startActivity(intent)
        }

        loadBookings()

        return view
    }

    private fun loadBookings() {
        shimmerLayout.startShimmer()
        shimmerLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        // Get all upcoming bookings (station operators see all)
        bookingRepository.getUpcomingBookings { success, message, bookings ->
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            if (success && bookings != null) {
                adapter.updateBookings(bookings)
                if (bookings.isEmpty()) {
                    Toast.makeText(context, "No bookings found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Failed to load: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookings()
    }
}