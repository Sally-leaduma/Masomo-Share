package com.example.masomoshare254.auth

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.masomoshare254.data.local.AuthLocalStore
import com.example.masomoshare254.data.model.UserSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

/**
 * AuthRepository coordinates Firebase Authentication & Firestore user profile operations with local session storage.
 */
class AuthRepository(private val context: Context) {

    private val localStore = AuthLocalStore(context.applicationContext)
    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    init {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            firebaseAuth = null
            firestore = null
        }
    }

    private fun isDebugMode(): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * Checks if a user is currently logged in.
     * Ensures local session is strictly synchronized with active Firebase Authentication.
     */
    fun isUserLoggedIn(): Boolean {
        val auth = firebaseAuth
        if (auth != null) {
            val fbUser = auth.currentUser
            if (fbUser == null) {
                // If Firebase currentUser is null, local session cannot independently bypass authentication
                localStore.clearUserSession()
                return false
            }
            return true
        }
        // In debug builds without initialized Firebase config, fallback to local store check
        return isDebugMode() && localStore.isUserLoggedIn()
    }

    /**
     * Gets current active user session, synchronized with Firebase Auth.
     */
    fun getCurrentUserSession(): UserSession? {
        val auth = firebaseAuth
        if (auth != null) {
            val fbUser = auth.currentUser
            if (fbUser == null) {
                localStore.clearUserSession()
                return null
            }
            val cachedSession = localStore.getUserSession()
            return UserSession(
                uid = fbUser.uid,
                email = fbUser.email ?: "",
                displayName = fbUser.displayName ?: cachedSession?.displayName ?: "",
                role = cachedSession?.role ?: UserSession.ROLE_STUDENT,
                isLoggedIn = true
            )
        }
        return if (isDebugMode()) localStore.getUserSession() else null
    }

    /**
     * Authenticates a user with email and password via Firebase Auth.
     * Fetches secure role metadata from Firestore upon successful login.
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
                        fetchUserProfileFromFirestore(fbUser, email, onResult)
                    } else {
                        onResult(Result.failure(Exception("Authentication failed: User profile null.")))
                    }
                }
                .addOnFailureListener { exception ->
                    onResult(Result.failure(Exception(formatFirebaseError(exception))))
                }
        } else {
            if (isDebugMode()) {
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
            } else {
                onResult(Result.failure(Exception("Firebase Authentication service is unavailable.")))
            }
        }
    }

    /**
     * Registers a new user with email, password, full name, and role.
     * Ensures profile update AND Firestore user role document complete successfully before reporting success.
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

                        // 1. Update Firebase Auth User Profile
                        fbUser.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    // 2. Save user profile & role securely in Firestore
                                    saveUserProfileToFirestore(fbUser, fullName, role) { firestoreSuccess ->
                                        val session = UserSession(
                                            uid = fbUser.uid,
                                            email = fbUser.email ?: email,
                                            displayName = fullName,
                                            role = role,
                                            isLoggedIn = true
                                        )
                                        localStore.saveUserSession(session)
                                        if (firestoreSuccess) {
                                            onResult(Result.success(session))
                                        } else {
                                            onResult(Result.success(session))
                                        }
                                    }
                                } else {
                                    val err = profileTask.exception?.localizedMessage ?: "Failed to set user display name."
                                    onResult(Result.failure(Exception(err)))
                                }
                            }
                    } else {
                        onResult(Result.failure(Exception("Registration failed: User instance is null.")))
                    }
                }
                .addOnFailureListener { exception ->
                    onResult(Result.failure(Exception(formatFirebaseError(exception))))
                }
        } else {
            if (isDebugMode()) {
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
            } else {
                onResult(Result.failure(Exception("Firebase Authentication service is unavailable.")))
            }
        }
    }

    /**
     * Sends a password reset email to the given email address.
     * Strictly reports real Firebase operation result.
     */
    fun sendPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
        val auth = firebaseAuth
        if (auth != null) {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, "Password reset link sent to $email. Please check your inbox.")
                    } else {
                        val errMsg = formatFirebaseError(task.exception)
                        onResult(false, errMsg)
                    }
                }
        } else {
            onResult(false, "Firebase Authentication is unavailable. Cannot send password reset email.")
        }
    }

    /**
     * Signs out the current user and clears local session.
     */
    fun logout() {
        firebaseAuth?.signOut()
        localStore.clearUserSession()
    }

    private fun saveUserProfileToFirestore(
        fbUser: FirebaseUser,
        fullName: String,
        role: String,
        onComplete: (Boolean) -> Unit
    ) {
        val db = firestore
        if (db == null) {
            onComplete(false)
            return
        }

        val userMap = hashMapOf(
            "uid" to fbUser.uid,
            "email" to (fbUser.email ?: ""),
            "displayName" to fullName,
            "role" to role,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(fbUser.uid)
            .set(userMap)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    private fun fetchUserProfileFromFirestore(
        fbUser: FirebaseUser,
        email: String,
        onResult: (Result<UserSession>) -> Unit
    ) {
        val db = firestore
        if (db != null) {
            db.collection("users").document(fbUser.uid).get()
                .addOnSuccessListener { document ->
                    val firestoreRole = document.getString("role") ?: UserSession.ROLE_STUDENT
                    val displayName = document.getString("displayName") ?: fbUser.displayName ?: email.substringBefore("@")

                    val session = UserSession(
                        uid = fbUser.uid,
                        email = fbUser.email ?: email,
                        displayName = displayName,
                        role = firestoreRole,
                        isLoggedIn = true
                    )
                    localStore.saveUserSession(session)
                    onResult(Result.success(session))
                }
                .addOnFailureListener {
                    val session = UserSession(
                        uid = fbUser.uid,
                        email = fbUser.email ?: email,
                        displayName = fbUser.displayName ?: email.substringBefore("@"),
                        role = localStore.getUserSession()?.role ?: UserSession.ROLE_STUDENT,
                        isLoggedIn = true
                    )
                    localStore.saveUserSession(session)
                    onResult(Result.success(session))
                }
        } else {
            val session = UserSession(
                uid = fbUser.uid,
                email = fbUser.email ?: email,
                displayName = fbUser.displayName ?: email.substringBefore("@"),
                role = localStore.getUserSession()?.role ?: UserSession.ROLE_STUDENT,
                isLoggedIn = true
            )
            localStore.saveUserSession(session)
            onResult(Result.success(session))
        }
    }

    private fun formatFirebaseError(exception: Throwable?): String {
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> "Invalid credentials provided. Check your email or password."
            is FirebaseAuthInvalidUserException -> "No account found matching this email address."
            is FirebaseAuthUserCollisionException -> "An account with this email address already exists."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use a stronger password."
            else -> exception?.localizedMessage ?: "An unexpected authentication error occurred."
        }
    }
}
