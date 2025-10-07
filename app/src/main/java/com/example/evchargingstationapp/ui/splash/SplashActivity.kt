package com.example.evchargingstationapp.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.evchargingstationapp.ui.auth.LoginActivity
import com.example.evchargingstationapp.R

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ Must come before super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Optional small delay logic can go here

        // Redirect to LoginActivity
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
