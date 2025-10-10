package com.example.evchargingstationapp.ui.bookings

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
import com.example.evchargingstationapp.data.repository.BookingRepository
import com.example.evchargingstationapp.model.BookingStatus
import com.facebook.shimmer.ShimmerFrameLayout

class CompletedBookingsFragment : Fragment() {

    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingRepository: BookingRepository
    private lateinit var prefs: PrefsHelper
    private lateinit var adapter: BookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_booking_list, container, false)

        shimmerLayout = view.findViewById(R.id.shimmerLayout)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        bookingRepository = BookingRepository(requireContext())
        prefs = PrefsHelper(requireContext())

        // Initialize adapter without action buttons (no callbacks for completed bookings)
        adapter = BookingAdapter(emptyList(), requireContext())
        recyclerView.adapter = adapter

        loadBookings()

        return view
    }

    private fun loadBookings() {
        shimmerLayout.startShimmer()
        shimmerLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        val userNic = prefs.getNic()
        if (userNic.isNullOrEmpty()) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            return
        }

        bookingRepository.getBookingsByEvOwner(userNic) { success, message, bookings ->
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            if (success && bookings != null) {
                // Filter for completed and cancelled bookings
                val completedBookings = bookings.filter {
                    it.status == BookingStatus.Completed || it.status == BookingStatus.Cancelled
                }
                adapter.updateBookings(completedBookings)

                if (completedBookings.isEmpty()) {
                    Toast.makeText(context, "No completed bookings", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Failed to load bookings: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookings() // Refresh when fragment becomes visible
    }
}