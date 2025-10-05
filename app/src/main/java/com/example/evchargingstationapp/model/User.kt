package com.example.evchargingstationapp.model

data class User(
    val nic: String,
    var name: String,
    var email: String,
    val password: String? = null,   // 👈 optional now
    var isActive: Int = 1 // 1 = active, 0 = deactivated
)
