package com.example.masomoshare254.auth

import android.content.Context
import com.example.masomoshare254.data.local.AuthLocalStore
import com.example.masomoshare254.data.model.UserSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * AuthRepository coordinates Firebase Authentication operations with local storage.
 */
class AuthRepository(context: Context) {

    private val localStore = AuthLocalStore(context.applicationContext)
    private var firebaseAuth: FirebaseAuth? = null

    init {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
        } catch (_: Exception) {
            // Firebase may not be fully initialized if google-services.json is pending
            firebaseAuth = null
        }
    }

    /**
     * Checks if a user is currently logged in (either via Firebase or local storage).
     */
    fun isUserLoggedIn(): Boolean {
        val fbUser = firebaseAuth?.currentUser
        if (fbUser != null) {
            return true
        }
        return localStore.isUserLoggedIn()
    }

    /**
     * Gets current active user session.
     */
    fun getCurrentUserSession(): UserSession? {
        val fbUser = firebaseAuth?.currentUser
        if (fbUser != null) {
            return UserSession(
                uid = fbUser.uid,
                email = fbUser.email ?: "",
                displayName = fbUser.displayName ?: "",
                role = localStore.getUserSession()?.role ?: UserSession.ROLE_STUDENT,
                isLoggedIn = true
            )
        }
        return localStore.getUserSession()
    }

    /**
     * Authenticates a user with email and password via Firebase Auth.
     */
    fun loginUser(
        email: String,
        pass: String,
        onResult: (Result<UserSession>) -> Unit
    ) {
        val auth = firebaseAuth
        if (auth != null) {
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { authResult ->
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        val session = UserSession(
                            uid = fbUser.uid,
                            email = fbUser.email ?: email,
                            displayName = fbUser.displayName ?: email.substringBefore("@"),
                            role = localStore.getUserSession()?.role ?: UserSession.ROLE_STUDENT,
                            isLoggedIn = true
                        )
                        localStore.saveUserSession(session)
                        onResult(Result.success(session))
                    } else {
                        onResult(Result.failure(Exception("Authentication failed: User profile null.")))
                    }
                }
                .addOnFailureListener { exception ->
                    onResult(Result.failure(exception))
                }
        } else {
            // Fallback for initial development mode when Firebase backend is not configured
            if (email.isNotBlank() && pass.length >= 6) {
                val mockSession = UserSession(
                    uid = "mock_uid_${System.currentTimeMillis()}",
                    email = email,
                    displayName = email.substringBefore("@"),
                    role = UserSession.ROLE_STUDENT,
                    isLoggedIn = true
                )
                localStore.saveUserSession(mockSession)
                onResult(Result.success(mockSession))
            } else {
                onResult(Result.failure(Exception("Invalid email or password minimum length of 6 characters required.")))
            }
        }
    }

    /**
     * Registers a new user with email, password, full name, and role.
     */
    fun registerUser(
        email: String,
        pass: String,
        fullName: String,
        role: String,
        onResult: (Result<UserSession>) -> Unit
    ) {
        val auth = firebaseAuth
        if (auth != null) {
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { authResult ->
                    val fbUser = authResult.user
                    if (fbUser != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()
                        fbUser.updateProfile(profileUpdates)

                        val session = UserSession(
                            uid = fbUser.uid,
                            email = fbUser.email ?: email,
                            displayName = fullName,
                            role = role,
                            isLoggedIn = true
                        )
                        localStore.saveUserSession(session)
                        onResult(Result.success(session))
                    } else {
                        onResult(Result.failure(Exception("Registration failed: User instance is null.")))
                    }
                }
                .addOnFailureListener { exception ->
                    onResult(Result.failure(exception))
                }
        } else {
            // Fallback for development mode
            if (email.isNotBlank() && pass.length >= 6 && fullName.isNotBlank()) {
                val mockSession = UserSession(
                    uid = "mock_uid_${System.currentTimeMillis()}",
                    email = email,
                    displayName = fullName,
                    role = role,
                    isLoggedIn = true
                )
                localStore.saveUserSession(mockSession)
                onResult(Result.success(mockSession))
            } else {
                onResult(Result.failure(Exception("Please complete all required fields correctly.")))
            }
        }
    }

    /**
     * Sends a password reset email to the given email address.
     */
    fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit) {
        val auth = firebaseAuth
        if (auth != null) {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, "Password reset link sent to $email")
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Failed to send password reset email.")
                    }
                }
        } else {
            onResult(true, "Password reset instructions sent (Development mode).")
        }
    }

    /**
     * Signs out the current user and clears local session.
     */
    fun logout() {
        firebaseAuth?.signOut()
        localStore.clearUserSession()
    }
}
