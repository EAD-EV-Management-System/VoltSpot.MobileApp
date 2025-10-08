package com.example.evchargingstationapp.model

data class User(
    val nic: String,
    var firstName: String,
    var lastName: String,
    var email: String,
    var phoneNumber: String,
    val password: String? = null,
    var isActive: Int = 1 // 1 = active, 0 = deactivated
)
