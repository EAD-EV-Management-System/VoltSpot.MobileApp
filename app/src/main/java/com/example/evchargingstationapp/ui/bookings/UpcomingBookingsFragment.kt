package com.example.evchargingstationapp.ui.bookings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingstationapp.R
import com.facebook.shimmer.ShimmerFrameLayout

class UpcomingBookingsFragment : Fragment() {

    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_booking_list, container, false)

        shimmerLayout = view.findViewById(R.id.shimmerLayout)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Start shimmer (loading)
        shimmerLayout.startShimmer()

        // Simulate delay for loading data
        recyclerView.postDelayed({
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            recyclerView.adapter = BookingAdapter(
                listOf("Booking #001", "Booking #002", "Booking #003"),
                requireContext()
            )
        }, 2000)

        return view
    }
}
