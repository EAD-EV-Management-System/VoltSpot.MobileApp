package com.example.evchargingstationapp.ui.dashboard

import androidx.fragment.app.Fragment
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.evchargingstationapp.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var topAppBar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        topAppBar = findViewById(R.id.topAppBar)

        // Default screen
        replaceFragment(HomeFragment())
        topAppBar.title = "Dashboard"

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bottom_home -> {
                    replaceFragment(HomeFragment())
                    topAppBar.title = "Dashboard"
                    true
                }
                R.id.bottom_list -> {
                    replaceFragment(BookingFragment())
                    topAppBar.title = "Bookings"
                    true
                }
//                R.id.bottom_power -> {
//                    replaceFragment(StationsFragment())
//                    topAppBar.title = "Stations"
//                    true
//                }
                R.id.bottom_profile -> {
                    replaceFragment(ProfileFragment())
                    topAppBar.title = "Profile"
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment)
            .commit()
    }

    fun navigateToBookings() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.bottom_list
    }
}
