package com.example.evchargingstationapp.ui.bookings

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingstationapp.R

class BookingAdapter(private val bookings: List<String>, private val context: Context) :
    RecyclerView.Adapter<BookingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bookingText: TextView = view.findViewById(R.id.bookingTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = bookings[position]
        holder.bookingText.text = booking

        holder.itemView.setOnClickListener {
            val intent = Intent(context, BookingDetailActivity::class.java)
            intent.putExtra("BOOKING_ID", booking)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = bookings.size
}
