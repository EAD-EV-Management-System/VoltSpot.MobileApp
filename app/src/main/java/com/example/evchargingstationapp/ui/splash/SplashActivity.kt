package com.example.evchargingstationapp.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.evchargingstationapp.data.repository.UserRepository
import com.example.evchargingstationapp.ui.auth.LoginActivity
import com.example.evchargingstationapp.ui.dashboard.MainActivity

class SplashActivity : ComponentActivity() {

    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must come before super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)

        userRepository = UserRepository(this)

        // Check if user is already logged in
        checkUserSession()
    }

    private fun checkUserSession() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (userRepository.isUserLoggedIn()) {
                // User has valid tokens, go directly to main
                // Token refresh will happen automatically when API calls are made
                navigateToMain()
            } else {
                // No session, go to login
                navigateToLogin()
            }
        }, 1000) // 1 second delay for splash screen
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
