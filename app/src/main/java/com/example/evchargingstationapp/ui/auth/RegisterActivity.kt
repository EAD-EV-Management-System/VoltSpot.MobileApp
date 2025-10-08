package com.example.evchargingstationapp.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.repository.UserRepository
import com.example.evchargingstationapp.model.User

class RegisterActivity : AppCompatActivity() {
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        userRepository = UserRepository(this)

        val etNic = findViewById<EditText>(R.id.etNic)
        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val registerBtn = findViewById<Button>(R.id.btnRegister)

        val loginLink = findViewById<TextView>(R.id.tvLoginLink)
        loginLink.setOnClickListener {
            finish() // closes RegisterActivity and returns to LoginActivity
        }

        registerBtn.setOnClickListener {
            val nic = etNic.text.toString().trim()
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phoneNumber = etPhoneNumber.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // Validation
            if (nic.isEmpty() || firstName.isEmpty() || lastName.isEmpty() ||
                email.isEmpty() || phoneNumber.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent multiple clicks
            registerBtn.isEnabled = false
            Toast.makeText(this, "Registering...", Toast.LENGTH_SHORT).show()

            val user = User(
                nic = nic,
                firstName = firstName,
                lastName = lastName,
                email = email,
                phoneNumber = phoneNumber,
                password = password,
                isActive = 1
            )

            userRepository.register(user, password, confirmPassword) { success, message ->
                registerBtn.isEnabled = true
                if (success) {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, "Registration failed: $message", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
