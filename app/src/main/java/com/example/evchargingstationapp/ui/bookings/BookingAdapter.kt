package com.example.evchargingstationapp.ui.bookings

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.model.Booking
import com.example.evchargingstationapp.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(
    private var bookings: List<Booking>,
    private val context: Context,
    private val onCancelClick: ((Booking) -> Unit)? = null,
    private val onUpdateClick: ((Booking) -> Unit)? = null
) : RecyclerView.Adapter<BookingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val bookingId: TextView = view.findViewById(R.id.tvBookingId)
        val stationName: TextView = view.findViewById(R.id.tvStationName)
        val slotNumber: TextView = view.findViewById(R.id.tvSlotNumber)
        val status: TextView = view.findViewById(R.id.tvStatus)
        val btnCancel: Button? = view.findViewById(R.id.btnCancel)
        val reservationTime: TextView = view.findViewById(R.id.tvReservationTime)
        val btnUpdate: Button? = view.findViewById(R.id.btnUpdate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = bookings[position]

        // Booking ID
        holder.bookingId.text = "Booking ID: ${booking.id}"

        // Station name (fallback to ID if name is missing)
        holder.stationName.text = "Station: ${booking.chargingStationName ?: booking.chargingStationId}"

        holder.slotNumber.text = "Slot: ${booking.slotNumber}"

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

        // Button visibility and actions
        if (booking.status == BookingStatus.Pending || booking.status == BookingStatus.Confirmed) {
            holder.btnCancel?.visibility = View.VISIBLE
            holder.btnUpdate?.visibility = View.VISIBLE

            holder.btnCancel?.setOnClickListener { onCancelClick?.invoke(booking) }

            // Update button opens UpdateBookingActivity
            holder.btnUpdate?.setOnClickListener {
                val intent = Intent(context, UpdateBookingActivity::class.java)
                intent.putExtra("BOOKING_ID", booking.id)
                context.startActivity(intent)
            }
        } else {
            holder.btnCancel?.visibility = View.GONE
            holder.btnUpdate?.visibility = View.GONE
        }

        // Handle card click to open booking details
        holder.itemView.setOnClickListener {
            val intent = Intent(context, BookingDetailActivity::class.java)
            intent.putExtra("BOOKING_ID", booking.id)
            context.startActivity(intent)
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