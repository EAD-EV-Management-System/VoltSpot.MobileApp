package com.example.evchargingstationapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.repository.UserRepository
import com.example.evchargingstationapp.ui.dashboard.MainActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        userRepository = UserRepository(this)

        val etNic = findViewById<EditText>(R.id.etNic)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val loginBtn = findViewById<Button>(R.id.btnLogin)

        val registerLink = findViewById<TextView>(R.id.tvRegisterLink)
        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        loginBtn.setOnClickListener {
            val nic = etNic.text.toString().trim()
            val password = etPassword.text.toString()

            // Validation
            if (nic.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent multiple clicks
            loginBtn.isEnabled = false
            Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()

            userRepository.login(nic, password) { success, message ->
                loginBtn.isEnabled = true
                if (success) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Login failed: $message", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
