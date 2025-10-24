package com.example.evchargingstationapp.ui.operator

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.model.Booking
import com.example.evchargingstationapp.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.*

class OperatorBookingAdapter(
    private var bookings: List<Booking>,
    private val context: Context,
    private val onBookingClick: ((Booking) -> Unit)? = null
) : RecyclerView.Adapter<OperatorBookingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bookingId: TextView = view.findViewById(R.id.tvBookingId)
        val evOwnerNic: TextView = view.findViewById(R.id.tvEvOwnerNic)
        val stationName: TextView = view.findViewById(R.id.tvStationName)
        val slotNumber: TextView = view.findViewById(R.id.tvSlotNumber)
        val reservationTime: TextView = view.findViewById(R.id.tvReservationTime)
        val status: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_operator_booking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = bookings[position]

        // Booking ID
        holder.bookingId.text = "Booking ID: ${booking.id}"

        // EV Owner NIC
        holder.evOwnerNic.text = "Customer: ${booking.evOwnerNic}"

        // Station name (fallback to ID if name is missing)
        holder.stationName.text = "Station: ${booking.chargingStationName ?: booking.chargingStationId}"

        // Slot number
        holder.slotNumber.text = "Slot: ${booking.slotNumber}"

        // Reservation time
        val formattedTime = formatDateTime(booking.reservationDateTime)
        holder.reservationTime.text = "Time: $formattedTime"

        // Status
        holder.status.text = booking.status.name

        val statusColor = when (booking.status) {
            BookingStatus.Pending -> ContextCompat.getColor(context, R.color.statusPending)
            BookingStatus.Confirmed -> ContextCompat.getColor(context, R.color.statusConfirmed)
            BookingStatus.Completed -> ContextCompat.getColor(context, R.color.statusCompleted)
            BookingStatus.Cancelled -> ContextCompat.getColor(context, R.color.statusCancelled)
        }
        holder.status.setTextColor(statusColor)

        // Handle card click
        holder.itemView.setOnClickListener {
            onBookingClick?.invoke(booking)
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<Booking>) {
        bookings = newBookings
        notifyDataSetChanged()
    }

    private fun formatDateTime(isoDateTime: String?): String {
        if (isoDateTime.isNullOrEmpty()) return "Unknown date"
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