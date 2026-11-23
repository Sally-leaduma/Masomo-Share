package com.example.masomoshare254.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.masomoshare254.data.model.UserSession

/**
 * Local storage manager for persisting User authentication state and session profile.
 * Uses Android SharedPreferences to store user details locally on the device.
 */
class AuthLocalStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * Saves the logged-in user session locally.
     */
    fun saveUserSession(session: UserSession) {
        prefs.edit().apply {
            putString(KEY_UID, session.uid)
            putString(KEY_EMAIL, session.email)
            putString(KEY_DISPLAY_NAME, session.displayName)
            putString(KEY_ROLE, session.role)
            putBoolean(KEY_IS_LOGGED_IN, session.isLoggedIn)
            putLong(KEY_LAST_LOGIN, session.lastLoginTime)
            apply()
        }
    }

    /**
     * Retrieves the stored user session, or null if no active user session is saved.
     */
    fun getUserSession(): UserSession? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        return UserSession(
            uid = prefs.getString(KEY_UID, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: "",
            role = prefs.getString(KEY_ROLE, UserSession.ROLE_STUDENT) ?: UserSession.ROLE_STUDENT,
            isLoggedIn = true,
            lastLoginTime = prefs.getLong(KEY_LAST_LOGIN, 0L)
        )
    }

    /**
     * Returns true if a user is currently logged in locally.
     */
    fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Clears the user session upon logout.
     */
    fun clearUserSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "masomo_share_auth_prefs"
        private const val KEY_UID = "key_user_uid"
        private const val KEY_EMAIL = "key_user_email"
        private const val KEY_DISPLAY_NAME = "key_user_display_name"
        private const val KEY_ROLE = "key_user_role"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_LAST_LOGIN = "key_last_login"
    }
}
