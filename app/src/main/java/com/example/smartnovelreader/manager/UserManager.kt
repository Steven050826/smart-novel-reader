// UserManager.kt
package com.example.smartnovelreader.manager

import android.content.Context
import android.content.SharedPreferences

class UserManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_CURRENT_USER = "current_user"
    }

    fun login(userId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CURRENT_USER, userId)
            .apply()
    }

    fun getCurrentUser(): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CURRENT_USER, null)
    }

    fun clearUser() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CURRENT_USER)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return getCurrentUser() != null
    }
}