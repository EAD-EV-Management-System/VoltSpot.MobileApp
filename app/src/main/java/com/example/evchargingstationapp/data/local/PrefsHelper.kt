package com.example.evchargingstationapp.data.local

import android.content.Context
import android.content.SharedPreferences

class PrefsHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ev_prefs", Context.MODE_PRIVATE)

    fun saveAccessToken(token: String) = prefs.edit().putString("access_token", token).apply()
    fun getAccessToken(): String? = prefs.getString("access_token", null)

    fun saveRefreshToken(token: String) = prefs.edit().putString("refresh_token", token).apply()
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun saveNic(nic: String) = prefs.edit().putString("nic", nic).apply()
    fun getNic(): String? = prefs.getString("nic", null)

    fun isLoggedIn(): Boolean {
        return !getAccessToken().isNullOrEmpty() && !getNic().isNullOrEmpty()
    }

    fun clear() = prefs.edit().clear().apply()

    // Legacy support - for migration
    @Deprecated("Use saveAccessToken instead")
    fun saveToken(token: String) = saveAccessToken(token)
    @Deprecated("Use getAccessToken instead")
    fun getToken(): String? = getAccessToken()
}
