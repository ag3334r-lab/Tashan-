package com.example.data

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Result model for strict password validation.
 */
data class PasswordStrengthResult(
    val isValid: Boolean,
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean,
    val strengthScore: Int, // 0 to 100
    val feedbackMessage: String
)

/**
 * Result model for real/fake email validation.
 */
data class EmailValidationResult(
    val isValid: Boolean,
    val isDisposableOrFake: Boolean,
    val errorMessage: String? = null
)

/**
 * Centralized Authentication & Security Manager for Tashan.
 */
class AuthManager(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // Known disposable / temporary / fake email domains to detect and reject
    private val disposableEmailDomains = setOf(
        "mailinator.com", "tempmail.com", "10minutemail.com", "guerrillamail.com",
        "throwawaymail.com", "fakeinbox.com", "sharklasers.com", "yopmail.com",
        "getairmail.com", "dispostable.com", "mytemp.email", "trashmail.com",
        "temp-mail.org", "nada.ltd", "burnermail.io", "maildrop.cc", "test.com", "example.com"
    )

    // Store active Email OTP in memory with 5-minute expiration
    private var activeEmailOtp: String? = null
    private var activeEmailOtpTarget: String? = null
    private var emailOtpExpiresAt: Long = 0L

    // Store active Phone Verification ID from Firebase
    var activePhoneVerificationId: String? = null
        private set
    var activePhoneResendToken: PhoneAuthProvider.ForceResendingToken? = null
        private set

    /**
     * 1. Validate password strength against strict enterprise security criteria:
     * - Minimum 8 characters
     * - At least 1 uppercase letter
     * - At least 1 lowercase letter
     * - At least 1 number
     * - At least 1 special character
     */
    fun validatePassword(password: String): PasswordStrengthResult {
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { "!@#$%^&*()_+-=[]{};':\"|,.<>/?".contains(it) }

        var score = 0
        if (hasMinLength) score += 20
        if (hasUppercase) score += 20
        if (hasLowercase) score += 20
        if (hasDigit) score += 20
        if (hasSpecialChar) score += 20

        val isValid = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecialChar
        val feedback = when {
            !hasMinLength -> "Password must be at least 8 characters long."
            !hasUppercase -> "Include at least one uppercase letter (A-Z)."
            !hasLowercase -> "Include at least one lowercase letter (a-z)."
            !hasDigit -> "Include at least one number (0-9)."
            !hasSpecialChar -> "Include at least one special character (!@#$%^&*)."
            else -> "Strong & Secure Password"
        }

        return PasswordStrengthResult(
            isValid = isValid,
            hasMinLength = hasMinLength,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigit = hasDigit,
            hasSpecialChar = hasSpecialChar,
            strengthScore = score,
            feedbackMessage = feedback
        )
    }

    /**
     * 2. Detect and prevent fake or disposable email addresses.
     */
    fun validateEmail(email: String): EmailValidationResult {
        val trimmed = email.trim().lowercase()
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        if (!emailRegex.matches(trimmed)) {
            return EmailValidationResult(
                isValid = false,
                isDisposableOrFake = false,
                errorMessage = "Invalid email format. Please enter a valid address."
            )
        }
        val domain = trimmed.substringAfter("@", "")
        if (disposableEmailDomains.contains(domain)) {
            return EmailValidationResult(
                isValid = false,
                isDisposableOrFake = true,
                errorMessage = "Temporary or disposable email domains ($domain) are not allowed. Please use a trusted provider (e.g. Gmail, Outlook, Yahoo)."
            )
        }
        return EmailValidationResult(isValid = true, isDisposableOrFake = false)
    }

    /**
     * 3. Send real email verification link to user.
     */
    suspend fun sendEmailVerification(): Result<Unit> = suspendCancellableCoroutine { cont ->
        val user = firebaseAuth.currentUser
        if (user == null) {
            cont.resume(Result.failure(Exception("No logged-in user found to verify.")))
            return@suspendCancellableCoroutine
        }
        user.sendEmailVerification()
            .addOnSuccessListener {
                cont.resume(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                cont.resume(Result.failure(error))
            }
    }

    /**
     * Check if current user's email has been verified.
     * Reloads Firebase user state to get the latest flag.
     */
    suspend fun checkEmailVerified(): Result<Boolean> = suspendCancellableCoroutine { cont ->
        val user = firebaseAuth.currentUser
        if (user == null) {
            cont.resume(Result.success(false))
            return@suspendCancellableCoroutine
        }
        user.reload()
            .addOnSuccessListener {
                cont.resume(Result.success(user.isEmailVerified))
            }
            .addOnFailureListener { error ->
                cont.resume(Result.failure(error))
            }
    }

    /**
     * 4. Mobile Phone OTP: Trigger real SMS verification code via Firebase PhoneAuthProvider.
     */
    fun sendPhoneOtp(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (verificationId: String) -> Unit,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onVerificationCompleted(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onVerificationFailed(e)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                activePhoneVerificationId = verificationId
                activePhoneResendToken = token
                onCodeSent(verificationId)
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        activePhoneResendToken?.let { optionsBuilder.setForceResendingToken(it) }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    /**
     * Verify SMS OTP Code entered by user.
     */
    suspend fun verifyPhoneOtp(verificationId: String, otpCode: String): Result<FirebaseUser> =
        suspendCancellableCoroutine { cont ->
            try {
                val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
                firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener { authResult ->
                        authResult.user?.let { cont.resume(Result.success(it)) }
                            ?: cont.resume(Result.failure(Exception("User is null after phone sign in")))
                    }
                    .addOnFailureListener { error ->
                        cont.resume(Result.failure(error))
                    }
            } catch (e: Exception) {
                cont.resume(Result.failure(e))
            }
        }

    /**
     * 5. Email OTP Alternative: Generate cryptographically secure 6-digit OTP.
     */
    fun generateEmailOtp(email: String): String {
        val random = SecureRandom()
        val code = String.format("%06d", random.nextInt(1000000))
        activeEmailOtp = code
        activeEmailOtpTarget = email.trim().lowercase()
        emailOtpExpiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5)
        return code
    }

    /**
     * Verify the 6-digit Email OTP.
     */
    fun verifyEmailOtp(email: String, inputCode: String): Boolean {
        val trimmedEmail = email.trim().lowercase()
        if (System.currentTimeMillis() > emailOtpExpiresAt) {
            activeEmailOtp = null
            return false
        }
        if (activeEmailOtpTarget != trimmedEmail) {
            return false
        }
        val matches = activeEmailOtp != null && activeEmailOtp == inputCode.trim()
        if (matches) {
            activeEmailOtp = null
        }
        return matches
    }

    /**
     * 6. Email Link Authentication (Passwordless): Send sign-in link to user email.
     */
    fun sendSignInLink(email: String, onResult: (Result<Unit>) -> Unit) {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://tashan-app.firebaseapp.com/finishSignUp?email=$email")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                "com.example",
                true, /* installIfNotAvailable */
                "12" /* minimumVersion */
            )
            .build()

        firebaseAuth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener {
                onResult(Result.failure(it))
            }
    }

    /**
     * 7. Password Reset: Send reset link to user email.
     */
    fun sendPasswordResetEmail(email: String, onResult: (Result<Unit>) -> Unit) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /**
     * 8. Real Browser OAuth Sign-In (Apple, Facebook, X/Twitter) via Firebase OAuthProvider.
     * Opens the official external browser / Custom Tab flow and only succeeds if the
     * user completes OAuth authorization and returns a valid FirebaseUser.
     */
    suspend fun signInWithOAuthBrowser(
        activity: Activity,
        providerName: String
    ): Result<FirebaseUser> = suspendCancellableCoroutine { cont ->
        try {
            val providerId = when (providerName.trim().lowercase()) {
                "apple", "apple.com" -> "apple.com"
                "facebook", "facebook.com" -> "facebook.com"
                "twitter", "x", "twitter.com" -> "twitter.com"
                else -> "${providerName.trim().lowercase()}.com"
            }

            val providerBuilder = OAuthProvider.newBuilder(providerId)
            when (providerId) {
                "apple.com" -> providerBuilder.setScopes(listOf("email", "name"))
                "facebook.com" -> providerBuilder.setScopes(listOf("email", "public_profile"))
            }

            val pendingResultTask = firebaseAuth.pendingAuthResult
            val authTask = pendingResultTask
                ?: firebaseAuth.startActivityForSignInWithProvider(activity, providerBuilder.build())

            authTask
                .addOnSuccessListener { authResult ->
                    val user = authResult.user
                    if (user != null) {
                        cont.resume(Result.success(user))
                    } else {
                        cont.resume(
                            Result.failure(
                                Exception("$providerName authorization did not return a verified user.")
                            )
                        )
                    }
                }
                .addOnFailureListener { error ->
                    val rawMsg = error.localizedMessage ?: "OAuth browser sign-in failed."
                    val formattedMsg = if (rawMsg.contains("INVALID_APP_ID", ignoreCase = true) || rawMsg.contains("GetAuthDomainTask", ignoreCase = true) || rawMsg.contains("project config", ignoreCase = true)) {
                        "Firebase OAuth is not configured for $providerName in Firebase Console (INVALID_APP_ID 400). Please enable $providerName in Firebase Console -> Authentication -> Sign-in method, or use Google / Phone SMS / Email OTP."
                    } else {
                        rawMsg
                    }
                    cont.resume(Result.failure(Exception(formattedMsg)))
                }
        } catch (e: Exception) {
            val rawMsg = e.localizedMessage ?: "OAuth browser initialization failed."
            val formattedMsg = if (rawMsg.contains("INVALID_APP_ID", ignoreCase = true) || rawMsg.contains("GetAuthDomainTask", ignoreCase = true) || rawMsg.contains("project config", ignoreCase = true)) {
                "Firebase OAuth is not configured for $providerName in Firebase Console (INVALID_APP_ID 400). Please enable $providerName in Firebase Console -> Authentication -> Sign-in method, or use Google / Phone SMS / Email OTP."
            } else {
                rawMsg
            }
            cont.resume(Result.failure(Exception(formattedMsg)))
        }
    }

    /**
     * Checks if there is an in-flight OAuth redirect from the browser when the Activity resumes.
     */
    suspend fun checkPendingOAuth(): Result<FirebaseUser?> = suspendCancellableCoroutine { cont ->
        val pendingTask = firebaseAuth.pendingAuthResult
        if (pendingTask == null) {
            cont.resume(Result.success(null))
            return@suspendCancellableCoroutine
        }
        pendingTask
            .addOnSuccessListener { result ->
                cont.resume(Result.success(result.user))
            }
            .addOnFailureListener { error ->
                cont.resume(Result.failure(error))
            }
    }

    /**
     * 9. Generic method to sign in with social credentials.
     */
    suspend fun signInWithSocialCredential(credential: AuthCredential): Result<FirebaseUser> =
        suspendCancellableCoroutine { cont ->
            firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    result.user?.let { cont.resume(Result.success(it)) }
                        ?: cont.resume(Result.failure(Exception("Social login failed: User is null")))
                }
                .addOnFailureListener { error ->
                    cont.resume(Result.failure(error))
                }
        }
}

