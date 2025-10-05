package com.example.evchargingstationapp.data.local

import android.content.Context
import android.content.SharedPreferences

class PrefsHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ev_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) = prefs.edit().putString("token", token).apply()
    fun getToken(): String? = prefs.getString("token", null)
    fun saveNic(nic: String) = prefs.edit().putString("nic", nic).apply()
    fun getNic(): String? = prefs.getString("nic", null)
    fun clear() = prefs.edit().clear().apply()
}
