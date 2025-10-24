package com.example.evchargingstationapp.ui.bookings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.data.repository.BookingRepository
import com.example.evchargingstationapp.model.Booking
import com.example.evchargingstationapp.model.BookingStatus
import com.example.evchargingstationapp.model.CancelBookingRequest
import com.facebook.shimmer.ShimmerFrameLayout

class UpcomingBookingsFragment : Fragment() {

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

        // Initialize adapter with cancel callback only
        // Update is now handled by UpdateBookingActivity (opened from adapter directly)
        adapter = BookingAdapter(
            emptyList(),
            requireContext(),
            onCancelClick = { booking -> showCancelDialog(booking) }
        )
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
                // Filter for upcoming bookings (Pending and Confirmed)
                val upcomingBookings = bookings.filter {
                    it.status == BookingStatus.Pending || it.status == BookingStatus.Confirmed
                }
                adapter.updateBookings(upcomingBookings)

                if (upcomingBookings.isEmpty()) {
                    Toast.makeText(context, "No upcoming bookings", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Failed to load bookings: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCancelDialog(booking: Booking) {
        val input = EditText(context)
        input.hint = "Cancellation reason"

        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?")
            .setView(input)
            .setPositiveButton("Cancel Booking") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(context, "Please provide a reason", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                cancelBooking(booking.id, reason)
            }
            .setNegativeButton("Back", null)
            .show()
    }

    private fun cancelBooking(bookingId: String, reason: String) {
        val request = CancelBookingRequest(bookingId, reason)
        bookingRepository.cancelBooking(request) { success, message ->
            if (success) {
                Toast.makeText(context, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                loadBookings() // Reload the list
            } else {
                Toast.makeText(context, "Failed to cancel: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookings() // Refresh when fragment becomes visible
    }
}