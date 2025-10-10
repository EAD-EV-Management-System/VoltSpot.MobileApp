package com.example.evchargingstationapp.ui.bookings

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.model.Booking
import com.example.evchargingstationapp.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.ContextCompat

class BookingAdapter(
    private var bookings: List<Booking>,
    private val context: Context,
    private val onCancelClick: ((Booking) -> Unit)? = null,
    private val onUpdateClick: ((Booking) -> Unit)? = null
) : RecyclerView.Adapter<BookingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val stationName: TextView = view.findViewById(R.id.tvStationName)
        val slotNumber: TextView = view.findViewById(R.id.tvSlotNumber)
        val reservationDate: TextView = view.findViewById(R.id.tvReservationDate)
        val status: TextView = view.findViewById(R.id.tvStatus)
        val btnCancel: Button? = view.findViewById(R.id.btnCancel)
        val btnUpdate: Button? = view.findViewById(R.id.btnUpdate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = bookings[position]

        // Display booking information
        holder.stationName.text = booking.chargingStationName ?: "Station ${booking.chargingStationId}"
        holder.slotNumber.text = "Slot: ${booking.slotNumber}"
        holder.reservationDate.text = formatDateTime(booking.reservationDateTime)
        holder.status.text = booking.status.name

        // Set status color
        val statusColor = when (booking.status) {
            BookingStatus.Pending -> ContextCompat.getColor(context, R.color.statusPending)
            BookingStatus.Confirmed -> ContextCompat.getColor(context, R.color.statusConfirmed)
            BookingStatus.Completed -> ContextCompat.getColor(context, R.color.statusCompleted)
            BookingStatus.Cancelled -> ContextCompat.getColor(context, R.color.statusCancelled)
        }
        holder.status.setTextColor(statusColor)

        // Show/hide action buttons based on status
        when (booking.status) {
            BookingStatus.Pending, BookingStatus.Confirmed -> {
                holder.btnCancel?.visibility = View.VISIBLE
                holder.btnUpdate?.visibility = View.VISIBLE

                holder.btnCancel?.setOnClickListener {
                    onCancelClick?.invoke(booking)
                }

                holder.btnUpdate?.setOnClickListener {
                    onUpdateClick?.invoke(booking)
                }
            }
            else -> {
                holder.btnCancel?.visibility = View.GONE
                holder.btnUpdate?.visibility = View.GONE
            }
        }

        // Click to view details
        holder.itemView.setOnClickListener {
            val intent = Intent(context, BookingDetailActivity::class.java)
            intent.putExtra("BOOKING_ID", booking.id)
            context.startActivity(intent)
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