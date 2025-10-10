package com.example.evchargingstationapp.ui.operator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.repository.OperatorRepository

class OperatorLoginActivity : AppCompatActivity() {

    private lateinit var operatorRepository: OperatorRepository
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_login)

        operatorRepository = OperatorRepository(this)

        // Initialize views
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        // Login button
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(username, password)
        }
    }

    private fun login(username: String, password: String) {
        btnLogin.isEnabled = false
        progressBar.visibility = View.VISIBLE

        operatorRepository.login(username, password) { success, message, operator ->
            btnLogin.isEnabled = true
            progressBar.visibility = View.GONE

            if (success && operator != null) {
                Toast.makeText(this, "Welcome, ${operator.firstName}!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, OperatorMainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Login failed: $message", Toast.LENGTH_LONG).show()
            }
        }
    }
}
