package com.example.evchargingstationapp.model

data class StationOperator(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String, // "StationOperator"
    val status: String,
    val assignedStationIds: List<String>
)

data class AuthLoginRequest(
    val username: String,
    val password: String
)

data class AuthLoginResponse(
    val success: Boolean,
    val message: String,
    val data: AuthLoginData?
)

data class AuthLoginData(
    val accessToken: String,
    val refreshToken: String,
    val user: StationOperator
)