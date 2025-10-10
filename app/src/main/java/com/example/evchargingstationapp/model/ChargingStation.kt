package com.example.evchargingstationapp.model

data class ChargingStation(
    val id: String,
    val name: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val totalSlots: Int,
    val availableSlots: Int,
    val pricePerHour: Double,
    val status: String // Active, Inactive, Maintenance
)