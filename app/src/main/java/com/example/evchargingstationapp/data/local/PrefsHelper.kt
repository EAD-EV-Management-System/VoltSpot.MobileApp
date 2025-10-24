package com.example.evchargingstationapp.data.local

import android.content.Context
import android.content.SharedPreferences

class PrefsHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_NIC = "nic"
        private const val KEY_OPERATOR_ID = "operator_id"  // ✅ Move it here
    }

    fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun saveOperatorId(operatorId: String) {
        prefs.edit().putString(KEY_OPERATOR_ID, operatorId).apply()
    }

    fun getOperatorId(): String? {
        return prefs.getString(KEY_OPERATOR_ID, null)
    }

    fun saveRefreshToken(token: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveNic(nic: String) {
        prefs.edit().putString(KEY_NIC, nic).apply()
    }

    fun getNic(): String? {
        return prefs.getString(KEY_NIC, null)
    }

    fun isLoggedIn(): Boolean {
        return !getAccessToken().isNullOrEmpty() && !getNic().isNullOrEmpty()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun saveUserRole(role: String) {
        prefs.edit().putString("user_role", role).apply()
    }

    fun getUserRole(): String? {
        return prefs.getString("user_role", null)
    }

    fun saveUserId(id: String) {
        prefs.edit().putString("user_id", id).apply()
    }

    fun getUserId(): String? {
        return prefs.getString("user_id", null)
    }
}