package com.example.evchargingstationapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.ui.bookings.AddBookingActivity
import com.example.evchargingstationapp.ui.bookings.CompletedBookingsFragment
import com.example.evchargingstationapp.ui.bookings.UpcomingBookingsFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2

class BookingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_booking, container, false)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val fabAddBooking = view.findViewById<FloatingActionButton>(R.id.fabAddBooking)

        val fragments = listOf(UpcomingBookingsFragment(), CompletedBookingsFragment())
        val titles = listOf("Upcoming", "Completed")

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        // FAB click listener
        fabAddBooking.setOnClickListener {
            val intent = Intent(requireContext(), AddBookingActivity::class.java)
            startActivity(intent)
        }

        return view
    }
}