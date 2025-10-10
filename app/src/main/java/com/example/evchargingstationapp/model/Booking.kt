package com.example.evchargingstationapp.model

// Request DTOs
data class CreateBookingRequest(
    val evOwnerNic: String,
    val chargingStationId: String,
    val slotNumber: Int,
    val reservationDateTime: String // ISO 8601 format: yyyy-MM-ddTHH:mm:ss
)

data class UpdateBookingRequest(
    val bookingId: String,
    val newReservationDateTime: String
)

data class CancelBookingRequest(
    val bookingId: String,
    val cancellationReason: String
)

data class ConfirmBookingRequest(
    val bookingId: String
)

data class CompleteBookingRequest(
    val bookingId: String
)

// Response DTOs
data class Booking(
    val id: String,
    val evOwnerNic: String,
    val chargingStationId: String,
    val chargingStationName: String? = null,
    val slotNumber: Int,
    val reservationDateTime: String,
    val status: BookingStatus,
    val cancellationReason: String? = null,
    val createdAt: String,
    val updatedAt: String? = null
)

enum class BookingStatus {
    Pending,
    Confirmed,
    Completed,
    Cancelled;

    companion object {
        fun fromString(value: String): BookingStatus {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: Pending
        }
    }
}

data class BookingResponse(
    val success: Boolean,
    val message: String,
    val data: Booking?
)

data class BookingListResponse(
    val success: Boolean,
    val message: String,
    val data: List<Booking>?
)

// Booking Counts for Dashboard
data class BookingCounts(
    val pending: Int,
    val confirmed: Int,
    val upcoming: Int,
    val completed: Int = 0,   // default 0 if missing
    val cancelled: Int = 0    // default 0 if missing
)