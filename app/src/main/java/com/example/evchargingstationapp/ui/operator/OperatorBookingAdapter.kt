package com.example.evchargingstationapp.ui.operator

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.model.Booking
import com.example.evchargingstationapp.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.*

class OperatorBookingAdapter(
    private var bookings: List<Booking>,
    private val context: Context,
    private val onItemClick: (Booking) -> Unit
) : RecyclerView.Adapter<OperatorBookingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCustomerName: TextView = view.findViewById(R.id.tvCustomerName)
        val tvCustomerNic: TextView = view.findViewById(R.id.tvCustomerNic)
        val tvStationInfo: TextView = view.findViewById(R.id.tvStationInfo)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val tvStatus: TextView = view.findViewById(R.id.tvBookingStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_operator_booking, parent, false)
        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = bookings[position]

        // This would typically come from a user lookup
        holder.tvCustomerName.text = "👤 Customer (${booking.evOwnerNic})"
        holder.tvCustomerNic.text = "NIC: ${booking.evOwnerNic}"
        holder.tvStationInfo.text = "📍 ${booking.chargingStationName ?: "Station"} - Slot ${booking.slotNumber}"
        holder.tvDateTime.text = "🕐 ${formatDateTime(booking.reservationDateTime)}"
        holder.tvStatus.text = booking.status.name

        // Status color
        val statusColor = when (booking.status) {
            BookingStatus.Pending -> context.getColor(R.color.statusPending)
            BookingStatus.Confirmed -> context.getColor(R.color.statusConfirmed)
            BookingStatus.Completed -> context.getColor(R.color.statusCompleted)
            BookingStatus.Cancelled -> context.getColor(R.color.statusCancelled)
        }
        holder.tvStatus.setTextColor(statusColor)

        holder.itemView.setOnClickListener {
            onItemClick(booking)
        }
    }

    override fun getItemCount() = bookings.size

    fun updateBookings(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }

    private fun formatDateTime(isoDateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(isoDateTime)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            isoDateTime
        }
    }
}