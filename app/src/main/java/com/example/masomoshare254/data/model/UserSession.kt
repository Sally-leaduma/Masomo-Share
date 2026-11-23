package com.example.masomoshare254.data.model

/**
 * Data structure representing a user session in Masomo Share 254.
 */
data class UserSession(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = ROLE_STUDENT,
    val isLoggedIn: Boolean = false,
    val lastLoginTime: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROLE_STUDENT = "Student"
        const val ROLE_TEACHER = "Educator/Teacher"
    }
}
