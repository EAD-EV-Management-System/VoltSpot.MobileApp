package com.example.evchargingstationapp.ui.operator

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.local.PrefsHelper
import com.example.evchargingstationapp.data.repository.OperatorRepository
import com.example.evchargingstationapp.model.Booking
import com.facebook.shimmer.ShimmerFrameLayout

class OperatorBookingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var adapter: OperatorBookingAdapter
    private lateinit var operatorRepo: OperatorRepository

    private var operatorId: String = "" // will load from prefs or login

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_bookings)

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        shimmerLayout = findViewById(R.id.shimmerLayout)

        // Initialize adapter with empty list
        adapter = OperatorBookingAdapter(emptyList(), this) { booking ->
            // Optional: handle item click (open detail, validate, etc.)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize repository
        operatorRepo = OperatorRepository(this)
        operatorId = PrefsHelper(this).getNic() ?: "" // get logged-in operator ID

        // Load operator bookings
        loadBookings()
    }

    private fun loadBookings() {
        shimmerLayout.startShimmer()
        shimmerLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        operatorRepo.getBookings(operatorId) { success: Boolean, message: String, bookings: List<Booking>? ->
            shimmerLayout.stopShimmer()
            shimmerLayout.visibility = View.GONE

            if (success && !bookings.isNullOrEmpty()) {
                recyclerView.visibility = View.VISIBLE
                adapter.updateBookings(bookings)
            } else {
                Toast.makeText(
                    this,
                    message.ifEmpty { "No bookings found" },
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }
}
