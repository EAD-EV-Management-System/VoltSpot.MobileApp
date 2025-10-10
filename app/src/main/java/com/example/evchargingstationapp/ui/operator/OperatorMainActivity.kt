package com.example.evchargingstationapp.ui.operator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.evchargingstationapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class OperatorMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operator_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation_operator)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_operator_bookings -> {
                    loadFragment(OperatorBookingsFragment())
                    true
                }
                R.id.navigation_operator_profile -> {
                    loadFragment(OperatorProfileFragment())
                    true
                }
                else -> false
            }
        }

        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(OperatorBookingsFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_operator, fragment)
            .commit()
    }
}