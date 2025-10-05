package com.example.evchargingstationapp.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.evchargingstationapp.R
import com.example.evchargingstationapp.data.local.UserDbHelper
import com.example.evchargingstationapp.model.User

class RegisterActivity : AppCompatActivity() {
    private lateinit var dbHelper: UserDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = UserDbHelper(this)

        val nic = findViewById<EditText>(R.id.etNic)
        val name = findViewById<EditText>(R.id.etName)
        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val registerBtn = findViewById<Button>(R.id.btnRegister)

        val loginLink = findViewById<TextView>(R.id.tvLoginLink)
        loginLink.setOnClickListener {
            finish() // closes RegisterActivity and returns to LoginActivity
        }

        registerBtn.setOnClickListener {
            val user = User(
                nic.text.toString(),
                name.text.toString(),
                email.text.toString(),
                password.text.toString(),
                1
            )
            if (dbHelper.insertUser(user)) {
                Toast.makeText(this, "User registered!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Registration failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
