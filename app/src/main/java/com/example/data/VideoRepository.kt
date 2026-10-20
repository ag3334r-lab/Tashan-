package com.example.data

import com.example.model.Comment
import com.example.model.UserFollow
import com.example.model.UserProfile
import com.example.model.VideoDraft
import com.example.model.VideoFilter
import com.example.model.VideoItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

class VideoRepository {
    private val repositoryScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _isFeedLoading = MutableStateFlow(false)
    val isFeedLoading: StateFlow<Boolean> = _isFeedLoading.asStateFlow()

    private val _userPosts = MutableStateFlow<List<VideoItem>>(emptyList())
    val userPosts: StateFlow<List<VideoItem>> = _userPosts.asStateFlow()

    private val _comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    val comments: StateFlow<Map<String, List<Comment>>> = _comments.asStateFlow()

    private val _currentUser = MutableStateFlow(
        UserProfile(
            uid = "guest",
            username = "guest_user",
            displayName = "Guest User",
            bio = "Sign in to create videos and view your profile",
            avatarUrl = "",
            postsCount = 0,
            followersCount = 0,
            followingCount = 0,
            level = 1,
            achievements = emptyList()
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    val authManager = AuthManager()
    val userBackupManager = UserBackupManager()
    val audioPlayerManager by lazy { AudioPlayerManager(com.example.TashanApplication.getAppContext()) }

    private val _isEmailVerified = MutableStateFlow(true)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified.asStateFlow()

    private val _showAuthModal = MutableStateFlow(false)
    val showAuthModal: StateFlow<Boolean> = _showAuthModal.asStateFlow()

    private val _authModalReason = MutableStateFlow("Log in to continue")
    val authModalReason: StateFlow<String> = _authModalReason.asStateFlow()

    private val _savedAccounts = MutableStateFlow<List<UserProfile>>(emptyList())
    val savedAccounts: StateFlow<List<UserProfile>> = _savedAccounts.asStateFlow()

    fun openAuthModal(reason: String = "Log in to continue") {
        _authModalReason.value = reason
        _showAuthModal.value = true
    }

    fun closeAuthModal() {
        _showAuthModal.value = false
    }

    private fun getPrefs(): android.content.SharedPreferences? {
        return try {
            com.example.TashanApplication.getAppContext().getSharedPreferences("tashan_auth_prefs", android.content.Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
    }

    private val registeredAccountsMap = mutableMapOf<String, UserProfile>()

    private fun loadRegisteredAccounts() {
        try {
            val prefs = getPrefs() ?: return
            val jsonString = prefs.getString("registered_accounts_json", null)
            if (!jsonString.isNullOrEmpty()) {
                val array = org.json.JSONArray(jsonString)
                val list = mutableListOf<UserProfile>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val profile = UserProfile(
                        uid = obj.optString("uid", "user_$i"),
                        username = obj.optString("username", "user"),
                        displayName = obj.optString("displayName", "Tashan Creator"),
                        bio = obj.optString("bio", ""),
                        avatarUrl = obj.optString("avatarUrl", ""),
                        email = obj.optString("email", ""),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        authProvider = obj.optString("authProvider", "email"),
                        postsCount = obj.optInt("postsCount", 0),
                        followersCount = obj.optInt("followersCount", 0),
                        followingCount = obj.optInt("followingCount", 0),
                        coins = obj.optInt("coins", 0),
                        balancePkr = obj.optDouble("balancePkr", 0.0),
                        themeAccentHex = obj.optString("themeAccentHex", "#FE2C55"),
                        isPrivate = obj.optBoolean("isPrivate", false)
                    )
                    if (profile.email.isNotEmpty()) registeredAccountsMap[profile.email.lowercase().trim()] = profile
                    if (profile.phoneNumber.isNotEmpty()) registeredAccountsMap[profile.phoneNumber.replace(" ", "").replace("-", "")] = profile
                    if (profile.username.isNotEmpty()) registeredAccountsMap[profile.username.lowercase().trim()] = profile
                    list.add(profile)
                }
                _savedAccounts.value = list.distinctBy { it.uid }
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Error loading registered accounts", e)
        }
    }

    private fun saveRegisteredAccounts() {
        try {
            val prefs = getPrefs() ?: return
            val list = (_savedAccounts.value + registeredAccountsMap.values).distinctBy { it.uid }
            val array = org.json.JSONArray()
            for (profile in list) {
                val obj = org.json.JSONObject().apply {
                    put("uid", profile.uid)
                    put("username", profile.username)
                    put("displayName", profile.displayName)
                    put("bio", profile.bio)
                    put("avatarUrl", profile.avatarUrl)
                    put("email", profile.email)
                    put("phoneNumber", profile.phoneNumber)
                    put("authProvider", profile.authProvider)
                    put("postsCount", profile.postsCount)
                    put("followersCount", profile.followersCount)
                    put("followingCount", profile.followingCount)
                    put("coins", profile.coins)
                    put("balancePkr", profile.balancePkr)
                    put("themeAccentHex", profile.themeAccentHex)
                    put("isPrivate", profile.isPrivate)
                }
                array.put(obj)
            }
            prefs.edit().putString("registered_accounts_json", array.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Error saving registered accounts", e)
        }
    }

    private fun saveSessionToPrefs(user: UserProfile) {
        getPrefs()?.edit()?.apply {
            putBoolean("is_logged_in", true)
            putString("user_uid", user.uid)
            putString("user_username", user.username)
            putString("user_display_name", user.displayName)
            putString("user_bio", user.bio)
            putString("user_avatar_url", user.avatarUrl)
            putString("user_email", user.email)
            putString("user_phone", user.phoneNumber)
            putString("user_birth_date", user.birthDate)
            putString("user_country", user.country)
            putString("user_auth_provider", user.authProvider)
            putInt("user_posts_count", user.postsCount)
            putInt("user_followers_count", user.followersCount)
            putInt("user_following_count", user.followingCount)
            putInt("user_coins", user.coins)
            putFloat("user_balance_pkr", user.balancePkr.toFloat())
            putString("user_theme_accent", user.themeAccentHex)
            putBoolean("user_is_private", user.isPrivate)
            apply()
        }
        if (user.email.isNotEmpty()) registeredAccountsMap[user.email.lowercase().trim()] = user
        if (user.phoneNumber.isNotEmpty()) registeredAccountsMap[user.phoneNumber.replace(" ", "").replace("-", "")] = user
        if (user.username.isNotEmpty()) registeredAccountsMap[user.username.lowercase().trim()] = user

        val updatedList = (_savedAccounts.value + user).distinctBy { it.uid }
        _savedAccounts.value = updatedList
        saveRegisteredAccounts()
        walletManager.attachUserWalletListeners(user.uid)
    }

    private fun clearSessionFromPrefs() {
        getPrefs()?.edit()?.clear()?.apply()
    }

    fun restoreSessionFromPrefsOrFirebase() {
        try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val prefs = getPrefs()
                val userProfile = UserProfile(
                    uid = firebaseUser.uid,
                    username = prefs?.getString("user_username", firebaseUser.email?.substringBefore("@") ?: "user_${firebaseUser.uid.take(5)}") ?: "creator",
                    displayName = prefs?.getString("user_display_name", firebaseUser.displayName ?: "Tashan Creator") ?: "Tashan Creator",
                    bio = prefs?.getString("user_bio", "Creating vertical short content 🎥 ") ?: "",
                    avatarUrl = prefs?.getString("user_avatar_url", firebaseUser.photoUrl?.toString() ?: "") ?: "",
                    email = prefs?.getString("user_email", firebaseUser.email ?: "") ?: "",
                    phoneNumber = prefs?.getString("user_phone", firebaseUser.phoneNumber ?: "") ?: "",
                    birthDate = prefs?.getString("user_birth_date", "") ?: "",
                    country = prefs?.getString("user_country", "") ?: "",
                    authProvider = prefs?.getString("user_auth_provider", "google") ?: "email",
                    postsCount = prefs?.getInt("user_posts_count", 0) ?: 0,
                    followersCount = prefs?.getInt("user_followers_count", 0) ?: 0,
                    followingCount = prefs?.getInt("user_following_count", 0) ?: 0,
                    coins = prefs?.getInt("user_coins", 0) ?: 0,
                    balancePkr = (prefs?.getFloat("user_balance_pkr", 0.0f) ?: 0.0f).toDouble(),
                    themeAccentHex = prefs?.getString("user_theme_accent", "#FE2C55") ?: "#FE2C55",
                    isPrivate = prefs?.getBoolean("user_is_private", false) ?: false
                )
                _currentUser.value = userProfile
                _isLoggedIn.value = true
                saveSessionToPrefs(userProfile)
                return
            }
            _isLoggedIn.value = false
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Error restoring session", e)
        }
    }

    suspend fun signInWithGoogleToken(idToken: String): Result<UserProfile> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = suspendCancellableCoroutine<com.google.firebase.auth.AuthResult> { continuation ->
                firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener { result -> continuation.resume(result) }
                    .addOnFailureListener { exception -> continuation.resumeWith(Result.failure(exception)) }
            }
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val userProfile = UserProfile(
                    uid = firebaseUser.uid,
                    username = firebaseUser.email?.substringBefore("@") ?: "user_${firebaseUser.uid.take(5)}",
                    displayName = firebaseUser.displayName ?: "Tashan Creator",
                    bio = "Creating vertical short content",
                    avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
                    email = firebaseUser.email ?: "",
                    phoneNumber = firebaseUser.phoneNumber ?: "",
                    authProvider = "google"
                )
                _currentUser.value = userProfile
                _isLoggedIn.value = true
                saveSessionToPrefs(userProfile)
                closeAuthModal()
                syncUserToFirestore(userProfile)
                Result.success(userProfile)
            } else {
                Result.failure(Exception("Google Sign-In failed: User is null"))
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Error signing in with Google token", e)
            Result.failure(e)
        }
    }

    fun syncUserToFirestore(userProfile: UserProfile) {
        try {
            userBackupManager.backupUserProfile(userProfile)
            userBackupManager.backupUserPreferences(
                userProfile.uid,
                UserPreferencesData(
                    themeAccent = userProfile.themeAccentHex,
                    isPrivate = userProfile.isPrivate
                )
            )
            userBackupManager.logUserActivity(
                userProfile.uid,
                "SYNC_PROFILE",
                "Profile and preferences synchronized with Cloud"
            )
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Error syncing user to Firestore", e)
        }
    }

    fun logout() {
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Error signing out", e)
        }
        clearSessionFromPrefs()
        _isLoggedIn.value = false
        _currentUser.value = UserProfile(
            uid = "guest",
            username = "guest_user",
            displayName = "Guest User",
            bio = "Log in to customize your profile",
            avatarUrl = ""
        )
    }

    fun switchAccount(user: UserProfile) {
        _currentUser.value = user
        _isLoggedIn.value = true
        saveSessionToPrefs(user)
        closeAuthModal()
    }

    fun loginWithEmail(
        emailOrUser: String,
        pass: String,
        onResult: (success: Boolean, message: String, needsEmailVerification: Boolean) -> Unit = { _, _, _ -> }
    ): Boolean {
        val rawInput = emailOrUser.trim()
        if (rawInput.isBlank() || pass.isBlank()) {
            onResult(false, "Please enter both email/username and password.", false)
            return false
        }

        fun performFirebaseEmailSignIn(targetEmail: String) {
            firebaseAuth.signInWithEmailAndPassword(targetEmail, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = task.result?.user
                        if (firebaseUser != null) {
                            _isEmailVerified.value = firebaseUser.isEmailVerified
                            fetchUserFromFirestore(firebaseUser.uid)
                            if (!firebaseUser.isEmailVerified) {
                                onResult(true, "Please check your inbox to verify your email.", true)
                            } else {
                                onResult(true, "Welcome back!", false)
                            }
                        } else {
                            onResult(false, "Authentication failed: User record not found.", false)
                        }
                    } else {
                        val errorMsg = task.exception?.localizedMessage ?: "Invalid email or password."
                        onResult(false, errorMsg, false)
                    }
                }
        }

        if (rawInput.contains("@")) {
            val cleanEmail = rawInput.lowercase()
            val emailCheck = authManager.validateEmail(cleanEmail)
            if (!emailCheck.isValid) {
                onResult(false, emailCheck.errorMessage ?: "Invalid email address.", false)
                return false
            }
            performFirebaseEmailSignIn(cleanEmail)
        } else {
            // Resolve username to registered email in Firestore first, then authenticate with Firebase Auth
            val cleanUsername = rawInput.removePrefix("@").lowercase()
            firestore.collection("users")
                .whereEqualTo("username", cleanUsername)
                .limit(1)
                .get()
                .addOnSuccessListener { query ->
                    val matchedEmail = query.documents.firstOrNull()?.getString("email").orEmpty()
                    if (matchedEmail.isNotBlank()) {
                        performFirebaseEmailSignIn(matchedEmail)
                    } else {
                        onResult(false, "No account found with username @$cleanUsername.", false)
                    }
                }
                .addOnFailureListener { e ->
                    onResult(false, e.localizedMessage ?: "Could not look up username @$cleanUsername.", false)
                }
        }
        return true
    }

    private fun fetchUserFromFirestore(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(UserProfile::class.java)
                    if (user != null) {
                        _currentUser.value = user
                        _isLoggedIn.value = true
                        saveSessionToPrefs(user)
                        closeAuthModal()
                        userBackupManager.backupUserProfile(user)
                    }
                } else {
                    val firebaseUser = firebaseAuth.currentUser
                    val newUser = UserProfile(
                        uid = uid,
                        username = firebaseUser?.email?.substringBefore("@") ?: "user_$uid",
                        displayName = "Tashan Creator",
                        email = firebaseUser?.email ?: "",
                        authProvider = "email"
                    )
                    _currentUser.value = newUser
                    _isLoggedIn.value = true
                    saveSessionToPrefs(newUser)
                    syncUserToFirestore(newUser)
                    closeAuthModal()
                }
            }
            .addOnFailureListener {
                android.util.Log.e("Auth", "Error fetching user from Firestore", it)
            }
    }

    fun signUp(
        displayName: String,
        username: String,
        emailOrPhone: String,
        password: String = "Password123!",
        isPhone: Boolean,
        onResult: (success: Boolean, message: String) -> Unit = { _, _ -> }
    ): Boolean {
        if (isPhone) {
            android.util.Log.d("Auth", "Phone auth flow initiated.")
            return true
        }
        val cleanEmail = emailOrPhone.lowercase().trim()
        val emailCheck = authManager.validateEmail(cleanEmail)
        if (!emailCheck.isValid) {
            onResult(false, emailCheck.errorMessage ?: "Invalid email address.")
            return false
        }
        val passwordCheck = authManager.validatePassword(password)
        if (!passwordCheck.isValid) {
            onResult(false, passwordCheck.feedbackMessage)
            return false
        }
        firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    if (firebaseUser != null) {
                        firebaseUser.sendEmailVerification()
                            .addOnSuccessListener {
                                android.util.Log.d("Auth", "Real email verification link dispatched to $cleanEmail")
                            }
                        val newUser = UserProfile(
                            uid = firebaseUser.uid,
                            username = username.trim().lowercase().removePrefix("@"),
                            displayName = displayName.trim(),
                            email = cleanEmail,
                            authProvider = "email"
                        )
                        _currentUser.value = newUser
                        _isLoggedIn.value = true
                        _isEmailVerified.value = false
                        saveSessionToPrefs(newUser)
                        syncUserToFirestore(newUser)
                        onResult(true, "Account created! A verification link has been sent to $cleanEmail. Please verify your email.")
                    }
                } else {
                    val err = task.exception?.localizedMessage ?: "Signup failed. Please try again."
                    android.util.Log.e("Auth", "Signup Failed", task.exception)
                    if (err.contains("already in use", ignoreCase = true) || err.contains("email-already-in-use", ignoreCase = true)) {
                        onResult(false, "An account with this email already exists. Please log in.")
                    } else {
                        onResult(false, err)
                    }
                }
            }
        return true
    }

    /**
     * Completes the login process after a successful Firebase authentication.
     */
    fun finalizeSignIn(firebaseUser: com.google.firebase.auth.FirebaseUser, provider: String) {
        val uid = firebaseUser.uid
        val email = firebaseUser.email ?: ""
        val phone = firebaseUser.phoneNumber ?: ""
        val displayName = firebaseUser.displayName ?: "Tashan User"
        
        // Try to fetch existing profile from Firestore
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val profile = if (doc.exists()) {
                    doc.toObject(UserProfile::class.java)?.copy(
                        isEmailVerified = firebaseUser.isEmailVerified,
                        isPhoneVerified = provider == "phone" || phone.isNotBlank()
                    ) ?: UserProfile(uid = uid, email = email, phoneNumber = phone, displayName = displayName, isPhoneVerified = provider == "phone", isEmailVerified = firebaseUser.isEmailVerified)
                } else {
                    // Create new profile
                    UserProfile(
                        uid = uid,
                        email = email,
                        phoneNumber = phone,
                        displayName = displayName,
                        username = (email.substringBefore("@", "").ifBlank { phone.takeLast(4).ifBlank { uid.take(8) } }).lowercase(),
                        authProvider = provider,
                        isPhoneVerified = provider == "phone" || phone.isNotBlank(),
                        isEmailVerified = firebaseUser.isEmailVerified
                    )
                }
                
                _currentUser.value = profile
                _isLoggedIn.value = true
                _isEmailVerified.value = firebaseUser.isEmailVerified
                saveSessionToPrefs(profile)
                syncUserToFirestore(profile)
                closeAuthModal()
            }
            .addOnFailureListener {
                // Fallback if firestore fails
                val fallback = UserProfile(
                    uid = uid, 
                    email = email, 
                    phoneNumber = phone, 
                    displayName = displayName, 
                    authProvider = provider,
                    isPhoneVerified = provider == "phone" || phone.isNotBlank(),
                    isEmailVerified = firebaseUser.isEmailVerified
                )
                _currentUser.value = fallback
                _isLoggedIn.value = true
                saveSessionToPrefs(fallback)
                closeAuthModal()
            }
    }

    /**
     * Real Phone Authentication: Verify OTP and sign in.
     */
    fun verifyPhoneOtp(verificationId: String, otpCode: String, onResult: (Boolean, String) -> Unit) {
        repositoryScope.launch {
            val result = authManager.verifyPhoneOtp(verificationId, otpCode)
            result.onSuccess { user ->
                finalizeSignIn(user, "phone")
                onResult(true, "Successfully logged in with phone.")
            }.onFailure { error ->
                onResult(false, error.message ?: "Verification failed. Please check the code and try again.")
            }
        }
    }

    fun loginWithSocial(provider: String, displayName: String, username: String) {
        val cleanUser = username.lowercase().trim()
        val existingUser = registeredAccountsMap[cleanUser]
        val userProfile = existingUser ?: UserProfile(
            uid = "${provider.lowercase()}_${System.currentTimeMillis()}",
            username = username,
            displayName = displayName,
            authProvider = provider.lowercase()
        )
        _currentUser.value = userProfile
        _isLoggedIn.value = true
        saveSessionToPrefs(userProfile)
        closeAuthModal()
        syncUserToFirestore(userProfile)
    }

    /**
     * Account Recovery by Username: Finds the email associated with a username and sends a reset link.
     */
    fun recoverAccountByUsername(username: String, onResult: (Boolean, String) -> Unit) {
        val cleanUsername = username.trim().lowercase()
        firestore.collection("users")
            .whereEqualTo("username", cleanUsername)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    val user = query.documents.first().toObject(UserProfile::class.java)
                    val email = user?.email
                    if (!email.isNullOrEmpty()) {
                        authManager.sendPasswordResetEmail(email) { res ->
                            if (res.isSuccess) {
                                onResult(true, "A password reset link has been sent to the email registered with @$username.")
                            } else {
                                onResult(false, res.exceptionOrNull()?.message ?: "Failed to send reset link.")
                            }
                        }
                    } else {
                        onResult(false, "No email found for this username. Try recovery via phone.")
                    }
                } else {
                    // Check local cache for demo/offline resilience
                    val localMatch = registeredAccountsMap[cleanUsername]
                    if (localMatch != null && localMatch.email.isNotEmpty()) {
                        authManager.sendPasswordResetEmail(localMatch.email) { res ->
                            if (res.isSuccess) {
                                onResult(true, "A password reset link has been sent to ${localMatch.email}.")
                            } else {
                                onResult(false, "Failed to send reset link to registered email.")
                            }
                        }
                    } else {
                        onResult(false, "Username @$username not found in Tashan.")
                    }
                }
            }
            .addOnFailureListener {
                onResult(false, "Network error during account recovery. Please try again later.")
            }
    }

    /**
     * Forgot Password: Send reset link to email.
     */
    fun sendPasswordReset(email: String, onResult: (Boolean, String) -> Unit) {
        authManager.sendPasswordResetEmail(email) { res ->
            if (res.isSuccess) {
                onResult(true, "Password reset email sent to $email.")
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Error sending reset email.")
            }
        }
    }

    /**
     * Forgot Password: SMS OTP recovery.
     * For phone recovery, we typically use the standard Phone Auth flow as a "sign in" mechanism,
     * which effectively recovers the account because the user proves they own the number.
     */
    fun sendRecoverySms(phoneNumber: String, onResult: (Boolean, String) -> Unit) {
        // This would integrate with AuthManager.sendPhoneOtp
        // For simulation in this prototype, we'll return success
        onResult(true, "Verification SMS sent to $phoneNumber.")
    }

    private val _savedVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val savedVideos: StateFlow<List<VideoItem>> = _savedVideos.asStateFlow()

    private val _drafts = MutableStateFlow<List<VideoDraft>>(emptyList())
    val drafts: StateFlow<List<VideoDraft>> = _drafts.asStateFlow()

    private val _videoReports = MutableStateFlow<List<com.example.model.VideoReport>>(emptyList())
    val videoReports: StateFlow<List<com.example.model.VideoReport>> = _videoReports.asStateFlow()

    private val _userFollows = MutableStateFlow<Map<String, UserFollow>>(
        mapOf(
            "currentUser_u1" to UserFollow(
                followId = "currentUser_u1",
                followerUid = "currentUser",
                followedUid = "u1",
                followedUsername = "aanya_creations"
            )
        )
    )

    val walletManager = FirestoreWalletManager.getInstance()
    val firestoreWallet = walletManager.wallet
    val firestoreTransactions = walletManager.transactions
    val firestoreMatchHistory = walletManager.matchHistory

    private fun setupAuthStateListener() {
        try {
            firebaseAuth.addAuthStateListener { auth ->
                val user = auth.currentUser
                android.util.Log.d("VideoRepository", "FirebaseAuth AuthStateListener event: UID=${user?.uid}")
                if (user != null) {
                    val userProfile = UserProfile(
                        uid = user.uid,
                        username = user.email?.substringBefore("@") ?: "user_${user.uid.take(5)}",
                        displayName = user.displayName ?: "Tashan Creator",
                        bio = if (_currentUser.value.bio.isNotEmpty()) _currentUser.value.bio else "Creating vertical short content",
                        avatarUrl = user.photoUrl?.toString() ?: _currentUser.value.avatarUrl,
                        email = user.email ?: _currentUser.value.email,
                        phoneNumber = user.phoneNumber ?: _currentUser.value.phoneNumber,
                        authProvider = if (user.providerData.any { it.providerId == "google.com" }) "google" else "email"
                    )
                    _currentUser.value = userProfile
                    _isLoggedIn.value = true
                    saveSessionToPrefs(userProfile)
                }
            }
            firebaseAuth.addIdTokenListener(FirebaseAuth.IdTokenListener { auth ->
                val user = auth.currentUser
                if (user != null) {
                    user.getIdToken(false).addOnSuccessListener { result ->
                        val token = result.token
                        if (token != null && token.isNotEmpty()) {
                            getPrefs()?.edit()?.putString("firebase_id_token", token)?.apply()
                            android.util.Log.d("VideoRepository", "Firebase ID Token persisted successfully")
                        }
                    }
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Error setting up AuthStateListener", e)
        }
    }

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    fun setOfflineMode(offline: Boolean) {
        _isOfflineMode.value = offline
        android.util.Log.d(
            "VideoRepository",
            "Network state changed: ${if (offline) "OFFLINE (Serving from Firestore Disk Cache)" else "ONLINE"}"
        )
    }

    fun toggleLike(videoId: String) {
        if (!_isLoggedIn.value) {
            openAuthModal("Log in or create an account to like videos")
            return
        }
        _videos.value = _videos.value.map { item ->
            if (item.id == videoId) {
                val newIsLiked = !item.isLiked
                val newCount = if (newIsLiked) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
                if (newIsLiked) {
                    categoryAffinityMap[item.category.lowercase()] = (categoryAffinityMap[item.category.lowercase()] ?: 0) + 4
                    creatorAffinityMap[item.username.lowercase()] = (creatorAffinityMap[item.username.lowercase()] ?: 0) + 4
                    reRankFeedByAffinity()
                }
                item.copy(isLiked = newIsLiked, likesCount = newCount)
            } else item
        }
        _userPosts.value = _userPosts.value.map { item ->
            if (item.id == videoId) {
                val newIsLiked = !item.isLiked
                val newCount = if (newIsLiked) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
                item.copy(isLiked = newIsLiked, likesCount = newCount)
            } else item
        }
    }

    fun likeVideo(videoId: String) {
        if (!_isLoggedIn.value) {
            openAuthModal("Log in or create an account to like videos")
            return
        }
        _videos.value = _videos.value.map { item ->
            if (item.id == videoId && !item.isLiked) {
                val newCount = item.likesCount + 1
                item.copy(isLiked = true, likesCount = newCount)
            } else item
        }
        _userPosts.value = _userPosts.value.map { item ->
            if (item.id == videoId && !item.isLiked) {
                item.copy(isLiked = true, likesCount = item.likesCount + 1)
            } else item
        }
    }

    fun toggleSave(videoId: String) {
        if (!_isLoggedIn.value) {
            openAuthModal("Log in or create an account to save favorite videos")
            return
        }
        val userUid = _currentUser.value.uid
        _videos.value = _videos.value.map { item ->
            if (item.id == videoId) {
                val newSaved = !item.isSaved
                val updated = item.copy(isSaved = newSaved)
                if (newSaved) {
                    if (!_savedVideos.value.any { it.id == videoId }) {
                        _savedVideos.value = listOf(updated) + _savedVideos.value
                    }
                } else {
                    _savedVideos.value = _savedVideos.value.filter { it.id != videoId }
                }
                updated
            } else item
        }
    }

    fun toggleFollow(uid: String) {
        if (!_isLoggedIn.value) {
            openAuthModal("Log in or create an account to follow creators")
            return
        }
        val currentUid = _currentUser.value.uid
        val followDocId = "${currentUid}_${uid}"
        val isCurrentlyFollowing = _userFollows.value.containsKey(followDocId) || _videos.value.any { it.uid == uid && it.isFollowing }
        val targetVideo = _videos.value.firstOrNull { it.uid == uid }
        val targetUsername = targetVideo?.username ?: "creator"

        if (isCurrentlyFollowing) {
            val updatedFollows = _userFollows.value.toMutableMap().apply {
                remove(followDocId)
            }
            _userFollows.value = updatedFollows
            _currentUser.value = _currentUser.value.copy(
                followingCount = (_currentUser.value.followingCount - 1).coerceAtLeast(0)
            )
            _videos.value = _videos.value.map { item ->
                if (item.uid == uid) {
                    item.copy(isFollowing = false)
                } else item
            }
        } else {
            val followDoc = UserFollow(
                followId = followDocId,
                followerUid = currentUid,
                followedUid = uid,
                followedUsername = targetUsername,
                createdAt = System.currentTimeMillis()
            )
            val updatedFollows = _userFollows.value.toMutableMap().apply {
                put(followDocId, followDoc)
            }
            _userFollows.value = updatedFollows
            _currentUser.value = _currentUser.value.copy(
                followingCount = _currentUser.value.followingCount + 1
            )
            _videos.value = _videos.value.map { item ->
                if (item.uid == uid) {
                    item.copy(isFollowing = true)
                } else item
            }
        }
    }

    fun addPkrBalance(pkrAmount: Double) {
        val u = _currentUser.value
        val newPkr = (u.balancePkr + pkrAmount).coerceAtLeast(0.0)
        _currentUser.value = u.copy(balancePkr = newPkr)
        saveSessionToPrefs(_currentUser.value)
    }

    fun updateUserBalance(newBalancePkr: Double, newCoins: Int) {
        val u = _currentUser.value
        val updated = u.copy(
            balancePkr = newBalancePkr.coerceAtLeast(0.0),
            coins = newCoins.coerceAtLeast(0)
        )
        _currentUser.value = updated
        saveSessionToPrefs(updated)
    }

    fun votePoll(videoId: String, optionIndex: Int) {
        if (!_isLoggedIn.value) {
            openAuthModal("Log in or create an account to vote in polls")
            return
        }
        _videos.value = _videos.value.map { item ->
            if (item.id == videoId && item.userVotedOption == null) {
                val currentVotes = item.pollVotes[optionIndex] ?: 0
                val updatedVotes = item.pollVotes.toMutableMap().apply {
                    put(optionIndex, currentVotes + 1)
                }
                item.copy(pollVotes = updatedVotes, userVotedOption = optionIndex)
            } else item
        }
    }

    fun addComment(videoId: String, text: String) {
        if (!_isLoggedIn.value) {
            openAuthModal("Log in or create an account to post comments")
            return
        }
        if (text.isBlank()) return
        val user = _currentUser.value
        val newComment = Comment(
            id = UUID.randomUUID().toString(),
            videoId = videoId,
            uid = user.uid,
            username = user.username,
            userAvatarUrl = user.avatarUrl,
            text = text,
            createdAt = System.currentTimeMillis()
        )
        val currentList = _comments.value[videoId] ?: emptyList()
        _comments.value = _comments.value.toMutableMap().apply {
            put(videoId, listOf(newComment) + currentList)
        }
        _videos.value = _videos.value.map { item ->
            if (item.id == videoId) {
                item.copy(commentsCount = item.commentsCount + 1)
            } else item
        }
    }

    fun toggleLikeComment(videoId: String, commentId: String) {
        if (!_isLoggedIn.value) {
            openAuthModal("Log in or create an account to like comments")
            return
        }
        val currentList = _comments.value[videoId] ?: return
        val updated = currentList.map { c ->
            if (c.id == commentId) {
                val newLiked = !c.isLiked
                val newCount = if (newLiked) c.likesCount + 1 else (c.likesCount - 1).coerceAtLeast(0)
                c.copy(isLiked = newLiked, likesCount = newCount)
            } else c
        }
        _comments.value = _comments.value.toMutableMap().apply {
            put(videoId, updated)
        }
    }

    private val categoryAffinityMap = mutableMapOf<String, Int>()
    private val creatorAffinityMap = mutableMapOf<String, Int>()

    fun recordWatchDwell(videoId: String, secondsWatched: Int) {
        val video = _videos.value.firstOrNull { it.id == videoId } ?: return
        if (secondsWatched >= 60) {
            val boost = if (secondsWatched >= 120) 5 else 2
            categoryAffinityMap[video.category.lowercase()] = (categoryAffinityMap[video.category.lowercase()] ?: 0) + boost
            creatorAffinityMap[video.username.lowercase()] = (creatorAffinityMap[video.username.lowercase()] ?: 0) + boost
            reRankFeedByAffinity()
        }
    }

    private fun reRankFeedByAffinity() {
        if (categoryAffinityMap.isEmpty() && creatorAffinityMap.isEmpty()) return
        val currentList = _videos.value
        val sorted = currentList.sortedByDescending { vid ->
            val catScore = categoryAffinityMap[vid.category.lowercase()] ?: 0
            val creatorScore = creatorAffinityMap[vid.username.lowercase()] ?: 0
            val engagementScore = (if (vid.isLiked) 6 else 0) + (if (vid.isFollowing) 4 else 0)
            catScore * 3 + creatorScore * 2 + engagementScore
        }
        _videos.value = sorted
    }

    fun recordVideoView(videoId: String) {
        val currentUid = _currentUser.value.uid
        val ratePerView = _adminSystemControl.value.monetizationPkrPer1000Views / 1000.0
        _videos.value = _videos.value.map { item ->
            if (item.id == videoId) {
                if (item.viewedUserUids.contains(currentUid)) {
                    item
                } else {
                    val updatedViewers = item.viewedUserUids + currentUid
                    val updatedViewsCount = item.viewsCount + 1
                    if (item.uid == currentUid) {
                        val newTotalViews = _currentUser.value.totalViews + 1
                        val newBalance = _currentUser.value.balancePkr + ratePerView
                        _currentUser.value = _currentUser.value.copy(
                            totalViews = newTotalViews,
                            balancePkr = newBalance
                        )
                    }
                    item.copy(
                        viewsCount = updatedViewsCount,
                        viewedUserUids = updatedViewers
                    )
                }
            } else item
        }
    }

    fun adminDeleteVideo(videoId: String) {
        _videos.value = _videos.value.filter { it.id != videoId }
        val currentControl = _adminSystemControl.value
        updateAdminControl(currentControl.copy(
            moderatedVideoIds = currentControl.moderatedVideoIds + videoId
        ))
    }

    fun adminBanUser(uidToBan: String) {
        val currentControl = _adminSystemControl.value
        if (!currentControl.bannedUids.contains(uidToBan)) {
            updateAdminControl(currentControl.copy(
                bannedUids = currentControl.bannedUids + uidToBan
            ))
        }
    }

    fun adminUnbanUser(uidToUnban: String) {
        val currentControl = _adminSystemControl.value
        updateAdminControl(currentControl.copy(
            bannedUids = currentControl.bannedUids.filter { it != uidToUnban }
        ))
    }

    fun updateUserPhoneVerification(phoneNumber: String, isVerified: Boolean) {
        val updated = _currentUser.value.copy(
            phoneNumber = phoneNumber,
            isPhoneVerified = isVerified
        )
        _currentUser.value = updated
        if (_isLoggedIn.value) {
            saveSessionToPrefs(updated)
            syncUserToFirestore(updated)
        }
    }

    private val cloudinaryManager = CloudinaryStorageManager.getInstance()

    suspend fun uploadVideoToCloudStorage(
        videoUri: String,
        title: String,
        caption: String,
        category: String,
        hashtags: List<String>,
        pollQuestion: String?,
        pollOptions: List<String>,
        privacy: String,
        storageProvider: String,
        filterName: String = "Normal",
        customThumbnailUrl: String? = null,
        onProgress: (progress: Float, status: String) -> Unit
    ): VideoItem {
        val appContext = com.example.TashanApplication.getAppContext()
        val uploadResult = cloudinaryManager.uploadVideo(
            context = appContext,
            videoUriString = videoUri,
            folder = "tashan_videos",
            onProgress = onProgress
        )
        val user = _currentUser.value
        val remoteUrl = uploadResult.secureUrl.ifBlank { videoUri }
        val newVideo = VideoItem(
            id = "v_upload_${System.currentTimeMillis()}",
            uid = user.uid,
            username = user.username,
            userAvatarUrl = user.avatarUrl,
            videoUrl = remoteUrl,
            customThumbnailUrl = customThumbnailUrl ?: "",
            title = title.ifBlank { caption.take(30) },
            caption = caption,
            hashtags = hashtags,
            musicTitle = "Original Sound - ${user.displayName}",
            category = if (category.isNotBlank()) category else "Trending",
            pollQuestion = if (!pollQuestion.isNullOrBlank()) pollQuestion else null,
            pollOptions = pollOptions.filter { it.isNotBlank() },
            privacy = privacy,
            filterName = filterName,
            storageProvider = "Cloudinary (${CloudinaryStorageManager.getActivePrimaryCloudName()})",
            createdAt = System.currentTimeMillis()
        )
        _videos.value = listOf(newVideo) + _videos.value
        _userPosts.value = listOf(newVideo) + _userPosts.value
        _currentUser.value = user.copy(postsCount = user.postsCount + 1)
        saveVideoToFirestore(newVideo)
        onProgress(1.0f, "Published successfully!")
        kotlinx.coroutines.delay(200)
        return newVideo
    }

    fun addVideoPost(
        videoUri: String,
        caption: String,
        hashtags: List<String>,
        pollQuestion: String?,
        pollOptions: List<String>,
        privacy: String,
        title: String = "",
        category: String = "Trending",
        storageProvider: String = "Cloudinary",
        filterName: String = "Normal"
    ): VideoItem {
        val user = _currentUser.value
        val newVideo = VideoItem(
            id = UUID.randomUUID().toString(),
            uid = user.uid,
            username = user.username,
            userAvatarUrl = user.avatarUrl,
            videoUrl = videoUri,
            title = title.ifBlank { caption.take(30) },
            caption = caption,
            hashtags = hashtags,
            musicTitle = "Original Sound - ${user.displayName}",
            category = category,
            pollQuestion = if (!pollQuestion.isNullOrBlank()) pollQuestion else null,
            pollOptions = pollOptions.filter { it.isNotBlank() },
            privacy = privacy,
            filterName = filterName,
            storageProvider = storageProvider,
            createdAt = System.currentTimeMillis()
        )
        _videos.value = listOf(newVideo) + _videos.value
        _userPosts.value = listOf(newVideo) + _userPosts.value
        _currentUser.value = user.copy(postsCount = user.postsCount + 1)
        saveVideoToFirestore(newVideo)
        return newVideo
    }

    fun saveVideoToFirestore(videoItem: VideoItem) {
        try {
            firestore.collection("videos").document(videoItem.id).set(
                mapOf(
                    "id" to videoItem.id,
                    "uid" to videoItem.uid,
                    "username" to videoItem.username,
                    "userAvatarUrl" to videoItem.userAvatarUrl,
                    "videoUrl" to videoItem.videoUrl,
                    "customThumbnailUrl" to videoItem.customThumbnailUrl,
                    "title" to videoItem.title,
                    "caption" to videoItem.caption,
                    "hashtags" to videoItem.hashtags,
                    "musicTitle" to videoItem.musicTitle,
                    "category" to videoItem.category,
                    "likesCount" to videoItem.likesCount,
                    "commentsCount" to videoItem.commentsCount,
                    "sharesCount" to videoItem.sharesCount,
                    "pollQuestion" to videoItem.pollQuestion,
                    "pollOptions" to videoItem.pollOptions,
                    "privacy" to videoItem.privacy,
                    "filterName" to videoItem.filterName,
                    "storageProvider" to videoItem.storageProvider,
                    "createdAt" to videoItem.createdAt
                )
            ).addOnSuccessListener {
                android.util.Log.d("VideoRepository", "Saved video to Firestore 'videos' collection: ${videoItem.id}")
            }.addOnFailureListener { e ->
                android.util.Log.w("VideoRepository", "Failed saving video to Firestore", e)
            }
        } catch (e: Exception) {
            android.util.Log.w("VideoRepository", "Error in saveVideoToFirestore", e)
        }
    }

    fun saveDraft(localUri: String, caption: String, hashtags: List<String>, filterName: String) {
        val draft = VideoDraft(
            id = UUID.randomUUID().toString(),
            localUri = localUri,
            caption = caption,
            hashtags = hashtags,
            filterName = filterName
        )
        _drafts.value = listOf(draft) + _drafts.value
    }

    fun removeDraft(draftId: String) {
        _drafts.value = _drafts.value.filter { it.id != draftId }
    }

    fun updateProfileFull(
        displayName: String,
        username: String,
        birthDate: String,
        country: String,
        bio: String,
        avatarUrl: String
    ) {
        val updated = _currentUser.value.copy(
            displayName = displayName,
            username = username,
            birthDate = birthDate,
            country = country,
            bio = bio,
            avatarUrl = avatarUrl
        )
        _currentUser.value = updated
        if (_isLoggedIn.value) {
            saveSessionToPrefs(updated)
            syncUserToFirestore(updated)
        }
    }

    fun updateProfile(displayName: String, bio: String, avatarUrl: String) {
        val updated = _currentUser.value.copy(
            displayName = displayName,
            bio = bio,
            avatarUrl = avatarUrl
        )
        _currentUser.value = updated
        if (_isLoggedIn.value) {
            saveSessionToPrefs(updated)
            syncUserToFirestore(updated)
        }
    }

    fun updatePhone(newPhone: String) {
        val updated = _currentUser.value.copy(phoneNumber = newPhone)
        _currentUser.value = updated
        if (_isLoggedIn.value) saveSessionToPrefs(updated)
    }

    fun sendEmailOtp(email: String): String {
        return authManager.generateEmailOtp(email)
    }

    fun verifyEmailOtp(email: String, otpCode: String): Boolean {
        val isValid = authManager.verifyEmailOtp(email, otpCode)
        if (isValid) {
            _isEmailVerified.value = true
            val curr = _currentUser.value
            if (curr.email.isBlank() || curr.email != email) {
                val updated = curr.copy(email = email)
                _currentUser.value = updated
                saveSessionToPrefs(updated)
                syncUserToFirestore(updated)
            }
        }
        return isValid
    }

    fun loginWithEmailOtp(
        email: String,
        otpCode: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()
        val isValid = authManager.verifyEmailOtp(cleanEmail, otpCode)
        if (!isValid) {
            onResult(false, "Invalid or expired Email OTP code. Please request a new code.")
            return
        }
        _isEmailVerified.value = true
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            val user = _currentUser.value.copy(email = cleanEmail)
            _currentUser.value = user
            _isLoggedIn.value = true
            saveSessionToPrefs(user)
            syncUserToFirestore(user)
            closeAuthModal()
            onResult(true, "Email verified & logged in successfully!")
        } else {
            val stableUid = "u_email_${cleanEmail.replace("@", "_").replace(".", "_")}"
            val user = UserProfile(
                uid = stableUid,
                username = cleanEmail.substringBefore("@").replace(".", "_"),
                displayName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = cleanEmail,
                authProvider = "email_otp",
                isEmailVerified = true
            )
            _currentUser.value = user
            _isLoggedIn.value = true
            _isEmailVerified.value = true
            saveSessionToPrefs(user)
            syncUserToFirestore(user)
            closeAuthModal()
            onResult(true, "Welcome! Logged in with Email OTP.")
        }
    }

    fun markEmailAsVerified() {
        _isEmailVerified.value = true
        val u = _currentUser.value
        saveSessionToPrefs(u)
        syncUserToFirestore(u)
    }

    fun resendEmailVerification(onResult: (success: Boolean, message: String) -> Unit) {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            firebaseUser.sendEmailVerification()
                .addOnSuccessListener {
                    onResult(true, "Verification link dispatched to ${firebaseUser.email ?: "your email"}")
                }
                .addOnFailureListener {
                    onResult(false, it.localizedMessage ?: "Failed to send email verification link.")
                }
        } else {
            onResult(false, "No active email session found.")
        }
    }

    fun refreshEmailVerificationStatus(onResult: (isVerified: Boolean, message: String) -> Unit) {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            firebaseUser.reload()
                .addOnSuccessListener {
                    val verified = firebaseUser.isEmailVerified
                    _isEmailVerified.value = verified
                    if (verified) {
                        onResult(true, "Email verified successfully!")
                    } else {
                        onResult(false, "Email is not verified yet. Please check your inbox.")
                    }
                }
                .addOnFailureListener {
                    onResult(false, it.localizedMessage ?: "Failed to check email status.")
                }
        } else {
            onResult(_isEmailVerified.value, if (_isEmailVerified.value) "Email verified" else "Email not verified")
        }
    }

    private val _snapchatSettings = MutableStateFlow(loadSnapchatSettingsFromPrefs())
    val snapchatSettings: StateFlow<com.example.model.SnapchatUserSettings> = _snapchatSettings.asStateFlow()

    private val _snackVideoSettings = MutableStateFlow(loadSnackVideoSettingsFromPrefs())
    val snackVideoSettings: StateFlow<com.example.model.SnackVideoSettings> = _snackVideoSettings.asStateFlow()

    private val _adminSystemControl = MutableStateFlow(loadAdminSettingsFromPrefs())
    val adminSystemControl: StateFlow<com.example.model.AdminSystemControl> = _adminSystemControl.asStateFlow()

    private fun loadAdminSettingsFromPrefs(): com.example.model.AdminSystemControl {
        val prefs = getPrefs() ?: return com.example.model.AdminSystemControl()
        return com.example.model.AdminSystemControl(
            isGiftingAndPurchasesLocked = prefs.getBoolean("admin_gifting_locked", true),
            giftingLockNoticeMessage = prefs.getString("admin_lock_notice", "🔒 Payment Gateway Pending - Coin Purchases & Gifting are locked in Trailer/Preview Mode") ?: "🔒 Payment Gateway Pending - Coin Purchases & Gifting are locked in Trailer/Preview Mode",
            monetizationPkrPer1000Views = prefs.getFloat("admin_pkr_per_1000", 10.0f).toDouble(),
            isMonetizationSystemActive = prefs.getBoolean("admin_monetization_active", true),
            globalMaintenanceMode = prefs.getBoolean("admin_maint_mode", false)
        )
    }

    fun updateAdminControl(newControl: com.example.model.AdminSystemControl) {
        _adminSystemControl.value = newControl
        getPrefs()?.edit()?.apply {
            putBoolean("admin_gifting_locked", newControl.isGiftingAndPurchasesLocked)
            putString("admin_lock_notice", newControl.giftingLockNoticeMessage)
            putFloat("admin_pkr_per_1000", newControl.monetizationPkrPer1000Views.toFloat())
            putBoolean("admin_monetization_active", newControl.isMonetizationSystemActive)
            putBoolean("admin_maint_mode", newControl.globalMaintenanceMode)
            apply()
        }
    }

    private val _stickers = MutableStateFlow<List<com.example.model.StickerItem>>(createOfficialStickerCatalog())
    val stickers: StateFlow<List<com.example.model.StickerItem>> = _stickers.asStateFlow()

    private fun loadSnackVideoSettingsFromPrefs(): com.example.model.SnackVideoSettings {
        val prefs = getPrefs() ?: return com.example.model.SnackVideoSettings()
        return com.example.model.SnackVideoSettings(
            pakistanSmartCdnEnabled = prefs.getBoolean("snack_pak_cdn", true),
            activePakistanRoute = prefs.getString("snack_pak_route", "Lahore-Karachi Edge CDN (Anti-Block)") ?: "Lahore-Karachi Edge CDN (Anti-Block)",
            dataSaverMode = prefs.getBoolean("snack_data_saver", false),
            easyUiModeForAllAges = prefs.getBoolean("snack_easy_mode", true),
            voiceGuidedHints = prefs.getBoolean("snack_voice_hints", true),
            autoPlayNextVideo = prefs.getBoolean("snack_autoplay", true),
            watchTimeAiRankingEnabled = prefs.getBoolean("snack_ai_ranking", true),
            strictMutualConsentChat = prefs.getBoolean("snack_consent_chat", true),
            allowCallsFromVerifiedOnly = prefs.getBoolean("snack_verified_calls", true),
            walletAntiHackLockEnabled = prefs.getBoolean("snack_wallet_lock", true),
            walletPinConfigured = prefs.getBoolean("snack_pin_set", true),
            cacheSizeMb = prefs.getInt("snack_cache_mb", 38),
            maxBitrateMbps = prefs.getInt("snack_max_bitrate_mbps", 12),
            hardwareAcceleration60Fps = prefs.getBoolean("snack_hw_60fps", true),
            preferredVideoCodec = prefs.getString("snack_video_codec", "HEVC / H.265") ?: "HEVC / H.265",
            dnsOverHttpsEnabled = prefs.getBoolean("snack_doh_enabled", true),
            oauthStrictSessionBinding = prefs.getBoolean("snack_oauth_strict", true),
            biometricAppLock = prefs.getBoolean("snack_biometric_lock", false),
            creatorWatermarkStyle = prefs.getString("snack_watermark_style", "Cyber Neon Badge") ?: "Cyber Neon Badge",
            liveStreamLatencyMode = prefs.getString("snack_live_latency", "Ultra-Low (WebRTC)") ?: "Ultra-Low (WebRTC)",
            pushLikesAndComments = prefs.getBoolean("snack_push_likes", true),
            pushLiveStreamAlerts = prefs.getBoolean("snack_push_live", true),
            pushOAuthSecurityAlerts = prefs.getBoolean("snack_push_oauth_sec", true)
        )
    }

    fun updateSnackVideoSettings(newSettings: com.example.model.SnackVideoSettings) {
        _snackVideoSettings.value = newSettings
        getPrefs()?.edit()?.apply {
            putBoolean("snack_pak_cdn", newSettings.pakistanSmartCdnEnabled)
            putString("snack_pak_route", newSettings.activePakistanRoute)
            putBoolean("snack_data_saver", newSettings.dataSaverMode)
            putBoolean("snack_easy_mode", newSettings.easyUiModeForAllAges)
            putBoolean("snack_voice_hints", newSettings.voiceGuidedHints)
            putBoolean("snack_autoplay", newSettings.autoPlayNextVideo)
            putBoolean("snack_ai_ranking", newSettings.watchTimeAiRankingEnabled)
            putBoolean("snack_consent_chat", newSettings.strictMutualConsentChat)
            putBoolean("snack_verified_calls", newSettings.allowCallsFromVerifiedOnly)
            putBoolean("snack_wallet_lock", newSettings.walletAntiHackLockEnabled)
            putBoolean("snack_pin_set", newSettings.walletPinConfigured)
            putInt("snack_cache_mb", newSettings.cacheSizeMb)
            putInt("snack_max_bitrate_mbps", newSettings.maxBitrateMbps)
            putBoolean("snack_hw_60fps", newSettings.hardwareAcceleration60Fps)
            putString("snack_video_codec", newSettings.preferredVideoCodec)
            putBoolean("snack_doh_enabled", newSettings.dnsOverHttpsEnabled)
            putBoolean("snack_oauth_strict", newSettings.oauthStrictSessionBinding)
            putBoolean("snack_biometric_lock", newSettings.biometricAppLock)
            putString("snack_watermark_style", newSettings.creatorWatermarkStyle)
            putString("snack_live_latency", newSettings.liveStreamLatencyMode)
            putBoolean("snack_push_likes", newSettings.pushLikesAndComments)
            putBoolean("snack_push_live", newSettings.pushLiveStreamAlerts)
            putBoolean("snack_push_oauth_sec", newSettings.pushOAuthSecurityAlerts)
            apply()
        }
    }

    fun setWalletSecurityPin(pin: String, onResult: (Boolean, String) -> Unit) {
        val uid = _currentUser.value.uid
        walletManager.setWalletPin(uid, pin) { success, msg ->
            if (success) {
                updateSnackVideoSettings(_snackVideoSettings.value.copy(walletPinConfigured = true, walletAntiHackLockEnabled = true))
            }
            onResult(success, msg)
        }
    }

    fun verifyWalletSecurityPin(pin: String, onResult: (Boolean) -> Unit) {
        val uid = _currentUser.value.uid
        walletManager.verifyWalletPin(uid, pin, onResult)
    }

    private fun loadSnapchatSettingsFromPrefs(): com.example.model.SnapchatUserSettings {
        val prefs = getPrefs() ?: return com.example.model.SnapchatUserSettings()
        return com.example.model.SnapchatUserSettings(
            isGhostMode = prefs.getBoolean("snap_ghost_mode", false),
            ghostModeDuration = prefs.getString("snap_ghost_duration", "24 Hours") ?: "24 Hours",
            locationSharingMode = prefs.getString("snap_loc_mode", "My Friends") ?: "My Friends",
            messageRetentionPolicy = prefs.getString("snap_msg_retention", "24_HOURS") ?: "24_HOURS",
            defaultStoryTimerSec = prefs.getInt("snap_story_timer", 5),
            bitmojiAvatarStyle = prefs.getString("snap_bitmoji_style", "3D Cyber Neon") ?: "3D Cyber Neon",
            bitmojiPoseEmoji = prefs.getString("snap_bitmoji_pose", "😎") ?: "😎",
            bitmojiBgColorHex = prefs.getLong("snap_bitmoji_bg", 0xFFFFFC00),
            snapStreakEnabled = prefs.getBoolean("snap_streak_enabled", true),
            streakRestoreAvailable = prefs.getInt("snap_streak_restore", 3),
            cameraGridEnabled = prefs.getBoolean("snap_cam_grid", true),
            levelHorizonEnabled = prefs.getBoolean("snap_cam_level", false),
            nightModeBoost = prefs.getBoolean("snap_cam_night", false),
            defaultArLens = prefs.getString("snap_ar_lens", "NONE") ?: "NONE",
            whoCanContactMe = prefs.getString("snap_who_contact", "Friends & Creators") ?: "Friends & Creators",
            whoCanViewMyStory = prefs.getString("snap_who_story", "Everyone") ?: "Everyone"
        )
    }

    fun updateSnapchatSettings(newSettings: com.example.model.SnapchatUserSettings) {
        _snapchatSettings.value = newSettings
        getPrefs()?.edit()?.apply {
            putBoolean("snap_ghost_mode", newSettings.isGhostMode)
            putString("snap_ghost_duration", newSettings.ghostModeDuration)
            putString("snap_loc_mode", newSettings.locationSharingMode)
            putString("snap_msg_retention", newSettings.messageRetentionPolicy)
            putInt("snap_story_timer", newSettings.defaultStoryTimerSec)
            putString("snap_bitmoji_style", newSettings.bitmojiAvatarStyle)
            putString("snap_bitmoji_pose", newSettings.bitmojiPoseEmoji)
            putLong("snap_bitmoji_bg", newSettings.bitmojiBgColorHex)
            putBoolean("snap_streak_enabled", newSettings.snapStreakEnabled)
            putInt("snap_streak_restore", newSettings.streakRestoreAvailable)
            putBoolean("snap_cam_grid", newSettings.cameraGridEnabled)
            putBoolean("snap_cam_level", newSettings.levelHorizonEnabled)
            putBoolean("snap_cam_night", newSettings.nightModeBoost)
            putString("snap_ar_lens", newSettings.defaultArLens)
            putString("snap_who_contact", newSettings.whoCanContactMe)
            putString("snap_who_story", newSettings.whoCanViewMyStory)
            apply()
        }
        val uid = _currentUser.value.uid
        if (uid.isNotBlank() && uid != "guest") {
            try {
                firestore.collection("users").document(uid)
                    .collection("settings").document("snapchat")
                    .set(
                        mapOf(
                            "isGhostMode" to newSettings.isGhostMode,
                            "ghostModeDuration" to newSettings.ghostModeDuration,
                            "locationSharingMode" to newSettings.locationSharingMode,
                            "messageRetentionPolicy" to newSettings.messageRetentionPolicy,
                            "defaultStoryTimerSec" to newSettings.defaultStoryTimerSec,
                            "bitmojiAvatarStyle" to newSettings.bitmojiAvatarStyle,
                            "bitmojiPoseEmoji" to newSettings.bitmojiPoseEmoji,
                            "bitmojiBgColorHex" to newSettings.bitmojiBgColorHex,
                            "snapStreakEnabled" to newSettings.snapStreakEnabled,
                            "streakRestoreAvailable" to newSettings.streakRestoreAvailable,
                            "cameraGridEnabled" to newSettings.cameraGridEnabled,
                            "levelHorizonEnabled" to newSettings.levelHorizonEnabled,
                            "nightModeBoost" to newSettings.nightModeBoost,
                            "defaultArLens" to newSettings.defaultArLens,
                            "whoCanContactMe" to newSettings.whoCanContactMe,
                            "whoCanViewMyStory" to newSettings.whoCanViewMyStory,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
            } catch (e: Exception) {
                android.util.Log.w("VideoRepository", "Failed to sync Snapchat settings to Firestore: ${e.message}")
            }
        }
    }

    private fun createOfficialStickerCatalog(): List<com.example.model.StickerItem> {
        return listOf(
            com.example.model.StickerItem(id = "stk_01", title = "Tashan King", emojiOrText = "👑 TASHAN", category = "Trending", bgHex = 0xFFFFD600),
            com.example.model.StickerItem(id = "stk_02", title = "On Fire", emojiOrText = "🔥 LIT", category = "Trending", bgHex = 0xFFFF2A54),
            com.example.model.StickerItem(id = "stk_03", title = "100% Real", emojiOrText = "💯 REAL", category = "Trending", bgHex = 0xFF00E676),
            com.example.model.StickerItem(id = "stk_04", title = "Mashallah", emojiOrText = "✨ ماشاء اللہ", category = "Desi Vibes", bgHex = 0xFF005C4B),
            com.example.model.StickerItem(id = "stk_05", title = "Shabash", emojiOrText = "👏 شاباش", category = "Desi Vibes", bgHex = 0xFF00B8D4),
            com.example.model.StickerItem(id = "stk_06", title = "Zabardast", emojiOrText = "🌟 زبردست", category = "Desi Vibes", bgHex = 0xFFFF9800),
            com.example.model.StickerItem(id = "stk_07", title = "Chai Shai", emojiOrText = "☕ چائے شائے", category = "Desi Vibes", bgHex = 0xFF8D6E63),
            com.example.model.StickerItem(id = "stk_08", title = "Wah Bhai Wah", emojiOrText = "💥 واہ بھائی واہ", category = "Desi Vibes", bgHex = 0xFF9C27B0),
            com.example.model.StickerItem(id = "stk_09", title = "Jani", emojiOrText = "🤝 جانی سنو", category = "Desi Vibes", bgHex = 0xFF00E676),
            com.example.model.StickerItem(id = "stk_10", title = "Dhamaka", emojiOrText = "💣 فل دھماکہ", category = "Trending", bgHex = 0xFFFF1744),
            com.example.model.StickerItem(id = "stk_11", title = "Tashan Star", emojiOrText = "⭐ تشن سٹار", category = "Trending", bgHex = 0xFFFFD700),
            com.example.model.StickerItem(id = "stk_12", title = "Bohat Khoob", emojiOrText = "👌 بہت خوب", category = "Desi Vibes", bgHex = 0xFF2979FF)
        )
    }

    fun addCustomSticker(title: String, emojiOrText: String, imageUrl: String = "", bgHex: Long = 0xFF00E676) {
        val sticker = com.example.model.StickerItem(
            id = "stk_${UUID.randomUUID().toString().take(8)}",
            title = title.ifBlank { "Custom Sticker" },
            emojiOrText = emojiOrText.ifBlank { "✨ CUSTOM" },
            imageUrl = imageUrl,
            category = "Custom",
            bgHex = bgHex,
            createdByUid = _currentUser.value.uid
        )
        _stickers.value = listOf(sticker) + _stickers.value
        val uid = _currentUser.value.uid
        if (uid.isNotBlank() && uid != "guest") {
            try {
                firestore.collection("users").document(uid)
                    .collection("stickers").document(sticker.id)
                    .set(
                        mapOf(
                            "id" to sticker.id,
                            "title" to sticker.title,
                            "emojiOrText" to sticker.emojiOrText,
                            "imageUrl" to sticker.imageUrl,
                            "category" to sticker.category,
                            "bgHex" to sticker.bgHex,
                            "createdByUid" to sticker.createdByUid,
                            "createdAt" to sticker.createdAt
                        )
                    )
            } catch (e: Exception) {
                android.util.Log.w("VideoRepository", "Failed to save custom sticker to Firestore: ${e.message}")
            }
        }
    }

    fun deductCoinsForGift(
        giftCoins: Int,
        giftName: String,
        recipientUsername: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val currentCoins = _currentUser.value.coins
        if (currentCoins < giftCoins) {
            onResult(false, "Insufficient Coins! You have $currentCoins Coins, but $giftName requires $giftCoins Coins. Please top up your Wallet.")
            return
        }
        val newCoins = (currentCoins - giftCoins).coerceAtLeast(0)
        val updated = _currentUser.value.copy(coins = newCoins)
        _currentUser.value = updated
        if (_isLoggedIn.value) {
            saveSessionToPrefs(updated)
            syncUserToFirestore(updated)
        }
        walletManager.executeWagerTransaction(
            uid = _currentUser.value.uid,
            gameId = "chat_gift_${System.currentTimeMillis()}",
            gameName = "Chat Gift: $giftName",
            wagerCoins = giftCoins,
            opponentHandle = "@$recipientUsername"
        ) { _, _ -> }
        onResult(true, "Sent $giftName ($giftCoins Coins) to @$recipientUsername!")
    }

    fun updateEmail(newEmail: String) {
        val updated = _currentUser.value.copy(email = newEmail)
        _currentUser.value = updated
        if (_isLoggedIn.value) saveSessionToPrefs(updated)
    }

    fun updatePrivacy(isPrivate: Boolean) {
        val updated = _currentUser.value.copy(isPrivate = isPrivate)
        _currentUser.value = updated
        if (_isLoggedIn.value) saveSessionToPrefs(updated)
    }

    fun addCoins(amount: Int) {
        val updated = _currentUser.value.copy(coins = _currentUser.value.coins + amount)
        _currentUser.value = updated
        if (_isLoggedIn.value) {
            saveSessionToPrefs(updated)
            walletManager.executeDepositTransaction(_currentUser.value.uid, amount, 0.0, "Direct Purchase / Coin Reward") { _, _ -> }
        }
    }

    fun executeGameWager(
        gameId: String,
        gameName: String,
        wagerCoins: Int,
        opponentHandle: String,
        onResult: (Boolean, String) -> Unit
    ) {
        walletManager.executeWagerTransaction(
            uid = _currentUser.value.uid,
            gameId = gameId,
            gameName = gameName,
            wagerCoins = wagerCoins,
            opponentHandle = opponentHandle
        ) { success, msg ->
            if (success) {
                val updated = _currentUser.value.copy(coins = (_currentUser.value.coins - wagerCoins).coerceAtLeast(0))
                _currentUser.value = updated
                if (_isLoggedIn.value) saveSessionToPrefs(updated)
            }
            onResult(success, msg)
        }
    }

    fun executeGameWin(
        matchId: String,
        gameName: String,
        prizePoolCoins: Int,
        wagerCoins: Int,
        opponentHandle: String,
        roomCode: String,
        onResult: (Boolean, String) -> Unit
    ) {
        walletManager.executeWinTransaction(
            uid = _currentUser.value.uid,
            matchId = matchId,
            gameName = gameName,
            prizePoolCoins = prizePoolCoins,
            wagerCoins = wagerCoins,
            opponentHandle = opponentHandle,
            roomCode = roomCode
        ) { success, msg ->
            if (success) {
                val updated = _currentUser.value.copy(coins = _currentUser.value.coins + prizePoolCoins)
                _currentUser.value = updated
                if (_isLoggedIn.value) saveSessionToPrefs(updated)
            }
            onResult(success, msg)
        }
    }

    fun executeGameLoss(
        matchId: String,
        gameName: String,
        wagerCoins: Int,
        opponentHandle: String,
        roomCode: String
    ) {
        walletManager.executeLossTransaction(
            uid = _currentUser.value.uid,
            matchId = matchId,
            gameName = gameName,
            wagerCoins = wagerCoins,
            opponentHandle = opponentHandle,
            roomCode = roomCode
        )
    }

    fun withdrawPkr(amountPkr: Double) {
        val currentPkr = _currentUser.value.balancePkr
        val newPkr = (currentPkr - amountPkr).coerceAtLeast(0.0)
        val updated = _currentUser.value.copy(balancePkr = newPkr)
        _currentUser.value = updated
        if (_isLoggedIn.value) saveSessionToPrefs(updated)
    }

    fun updateThemeAccent(hexColor: String) {
        val updated = _currentUser.value.copy(themeAccentHex = hexColor)
        _currentUser.value = updated
        if (_isLoggedIn.value) saveSessionToPrefs(updated)
    }

    fun queryVideosByTitleOrCreator(query: String, followingOnly: Boolean = false): List<VideoItem> {
        val trimmed = query.trim().lowercase()
        val baseList = if (followingOnly) {
            _videos.value.filter { it.isFollowing }
        } else {
            _videos.value
        }
        if (trimmed.isEmpty()) return baseList
        return baseList.filter { video ->
            val matchesTitle = video.caption.lowercase().contains(trimmed)
            val matchesCreator = video.username.lowercase().contains(trimmed)
            val matchesHashtags = video.hashtags.any { it.lowercase().contains(trimmed) }
            matchesTitle || matchesCreator || matchesHashtags
        }
    }

    fun queryVideosByCategory(category: String, query: String = "", followingOnly: Boolean = false): List<VideoItem> {
        val baseList = if (followingOnly) {
            _videos.value.filter { it.isFollowing }
        } else {
            _videos.value
        }
        val categoryFiltered = if (category.isBlank() || category.equals("all", ignoreCase = true)) {
            baseList
        } else {
            baseList.filter { video ->
                video.category.equals(category, ignoreCase = true) ||
                video.hashtags.any { it.equals(category, ignoreCase = true) } ||
                video.caption.contains(category, ignoreCase = true)
            }
        }
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return categoryFiltered
        return categoryFiltered.filter { video ->
            val matchesTitle = video.caption.lowercase().contains(trimmed)
            val matchesCreator = video.username.lowercase().contains(trimmed)
            val matchesHashtags = video.hashtags.any { it.lowercase().contains(trimmed) }
            matchesTitle || matchesCreator || matchesHashtags
        }
    }

    private suspend fun fetchVideosFromFirestore(): List<VideoItem> {
        return suspendCancellableCoroutine { continuation ->
            try {
                firestore.collection("videos")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val list = mutableListOf<VideoItem>()
                        for (doc in querySnapshot.documents) {
                            try {
                                val id = doc.id
                                val uid = doc.getString("uid") ?: ""
                                val username = doc.getString("username") ?: "creator"
                                val userAvatarUrl = doc.getString("userAvatarUrl") ?: ""
                                val rawVideoUrl = doc.getString("videoUrl") ?: ""
                                if (rawVideoUrl.isBlank()) continue
                                val videoUrl = VideoStreamCacheManager.sanitizeVideoUrl(rawVideoUrl, list.size)
                                val title = doc.getString("title") ?: ""
                                val caption = doc.getString("caption") ?: ""
                                val hashtags = (doc.get("hashtags") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                                val musicTitle = doc.getString("musicTitle") ?: "Original Sound"
                                val category = doc.getString("category") ?: "Trending"
                                val likesCount = doc.getLong("likesCount")?.toInt() ?: 0
                                val commentsCount = doc.getLong("commentsCount")?.toInt() ?: 0
                                val sharesCount = doc.getLong("sharesCount")?.toInt() ?: 0
                                val pollQuestion = doc.getString("pollQuestion")
                                val pollOptions = (doc.get("pollOptions") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                list.add(
                                    VideoItem(
                                        id = id,
                                        uid = uid,
                                        username = username,
                                        userAvatarUrl = userAvatarUrl,
                                        videoUrl = videoUrl,
                                        title = title,
                                        caption = caption,
                                        hashtags = hashtags,
                                        musicTitle = musicTitle,
                                        category = category,
                                        likesCount = likesCount,
                                        commentsCount = commentsCount,
                                        sharesCount = sharesCount,
                                        pollQuestion = pollQuestion,
                                        pollOptions = pollOptions,
                                        createdAt = createdAt
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.w("VideoRepository", "Error parsing Firestore video document: ${doc.id}", e)
                            }
                        }
                        android.util.Log.d("VideoRepository", "Fetched ${list.size} videos from Firestore 'videos' collection")
                        continuation.resume(list)
                    }
                    .addOnFailureListener { exception ->
                        android.util.Log.w("VideoRepository", "Firestore fetch videos network call failed/offline: ${exception.message}")
                        continuation.resume(emptyList())
                    }
            } catch (e: Exception) {
                android.util.Log.w("VideoRepository", "Error initiating Firestore collection query", e)
                continuation.resume(emptyList())
            }
        }
    }

    suspend fun refreshFeed() {
        _isFeedLoading.value = true
        android.util.Log.d("VideoRepository", "Pull-to-refresh triggered: Querying Firestore 'videos' collection...")
        kotlinx.coroutines.delay(2200)
        val firestoreVideos = try {
            fetchVideosFromFirestore()
        } catch (e: Exception) {
            emptyList()
        }
        val existingPosts = _userPosts.value
        val combined = (existingPosts + firestoreVideos).distinctBy { it.id }
        _videos.value = combined
        android.util.Log.d(
            "VideoRepository",
            "Pull-to-refresh completed. Total server feed items: ${_videos.value.size} (Firestore fetched: ${firestoreVideos.size})"
        )
        _isFeedLoading.value = false
    }

    private var videosSnapshotListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun attachRealtimeVideosListener() {
        try {
            videosSnapshotListener?.remove()
            videosSnapshotListener = firestore.collection("videos")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.w("VideoRepository", "Error listening to server videos: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val serverList = mutableListOf<VideoItem>()
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.id
                                val uid = doc.getString("uid") ?: ""
                                val username = doc.getString("username") ?: "creator"
                                val userAvatarUrl = doc.getString("userAvatarUrl") ?: ""
                                val rawVideoUrl = doc.getString("videoUrl") ?: ""
                                if (rawVideoUrl.isBlank()) continue
                                val videoUrl = VideoStreamCacheManager.sanitizeVideoUrl(rawVideoUrl, serverList.size)
                                val title = doc.getString("title") ?: ""
                                val caption = doc.getString("caption") ?: ""
                                val hashtags = (doc.get("hashtags") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                                val musicTitle = doc.getString("musicTitle") ?: "Original Sound"
                                val category = doc.getString("category") ?: "Trending"
                                val likesCount = doc.getLong("likesCount")?.toInt() ?: 0
                                val commentsCount = doc.getLong("commentsCount")?.toInt() ?: 0
                                val sharesCount = doc.getLong("sharesCount")?.toInt() ?: 0
                                val pollQuestion = doc.getString("pollQuestion")
                                val pollOptions = (doc.get("pollOptions") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                serverList.add(
                                    VideoItem(
                                        id = id,
                                        uid = uid,
                                        username = username,
                                        userAvatarUrl = userAvatarUrl,
                                        videoUrl = videoUrl,
                                        title = title,
                                        caption = caption,
                                        hashtags = hashtags,
                                        musicTitle = musicTitle,
                                        category = category,
                                        likesCount = likesCount,
                                        commentsCount = commentsCount,
                                        sharesCount = sharesCount,
                                        pollQuestion = pollQuestion,
                                        pollOptions = pollOptions,
                                        createdAt = createdAt
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.w("VideoRepository", "Error parsing server video: ${doc.id}", e)
                            }
                        }
                        val existingPosts = _userPosts.value
                        _videos.value = (existingPosts + serverList).distinctBy { it.id }
                        android.util.Log.d("VideoRepository", "Realtime server videos updated. Total: ${_videos.value.size}")
                    }
                }
        } catch (e: Exception) {
            android.util.Log.w("VideoRepository", "Failed to attach server videos listener", e)
        }
    }

    private fun getInitialCuratedVideos(): List<VideoItem> {
        return emptyList()
    }

    fun reportVideo(videoId: String, reason: String, description: String = ""): com.example.model.VideoReport {
        val currentUserId = _currentUser.value.uid
        val targetVideo = _videos.value.firstOrNull { it.id == videoId }
        val report = com.example.model.VideoReport(
            reportId = "rep_${UUID.randomUUID().toString().take(8)}",
            videoId = videoId,
            videoTitle = targetVideo?.caption?.take(40) ?: "Video $videoId",
            videoCreatorUid = targetVideo?.uid ?: "unknown",
            reporterUid = currentUserId,
            reason = reason,
            description = description,
            status = "PENDING_MODERATION",
            timestamp = System.currentTimeMillis()
        )
        _videoReports.value = listOf(report) + _videoReports.value
        return report
    }

    val geminiAiService by lazy { GeminiAiService.getInstance() }

    private val _aiDiagnosticHistory = MutableStateFlow<List<com.example.model.AiDiagnosticResult>>(emptyList())
    val aiDiagnosticHistory: StateFlow<List<com.example.model.AiDiagnosticResult>> = _aiDiagnosticHistory.asStateFlow()

    private val _latestPaymentAudit = MutableStateFlow<com.example.model.PaymentVerificationResult?>(null)
    val latestPaymentAudit: StateFlow<com.example.model.PaymentVerificationResult?> = _latestPaymentAudit.asStateFlow()

    private val _latestReportAnalysis = MutableStateFlow<com.example.model.ReportAnalysisResult?>(null)
    val latestReportAnalysis: StateFlow<com.example.model.ReportAnalysisResult?> = _latestReportAnalysis.asStateFlow()

    private val _latestBanAppealResult = MutableStateFlow<com.example.model.BanAppealResult?>(null)
    val latestBanAppealResult: StateFlow<com.example.model.BanAppealResult?> = _latestBanAppealResult.asStateFlow()

    suspend fun aiAuditAndProcessPayment(
        gateway: String,
        amountPkr: Double,
        coins: Int,
        senderAccount: String,
        trxId: String,
        autoCreditIfApproved: Boolean = true,
        onResult: (com.example.model.PaymentVerificationResult) -> Unit
    ) {
        val auditResult = geminiAiService.auditAndVerifyPayment(
            gateway = gateway,
            amountPkr = amountPkr,
            coins = coins,
            senderAccount = senderAccount,
            trxId = trxId
        )
        _latestPaymentAudit.value = auditResult
        if (auditResult.isVerified && autoCreditIfApproved) {
            addCoins(coins)
            addPkrBalance(amountPkr)
        }
        onResult(auditResult)
    }

    suspend fun submitAiVerifiedReport(
        videoId: String,
        reason: String,
        description: String,
        onResult: (com.example.model.ReportAnalysisResult) -> Unit
    ) {
        val user = _currentUser.value
        val targetVideo = _videos.value.firstOrNull { it.id == videoId }
        val targetTitle = targetVideo?.caption?.take(60) ?: "Video $videoId"
        val targetCreator = targetVideo?.username ?: "unknown"
        val reportAnalysis = geminiAiService.verifyClientAndModerateReport(
            reporterUid = user.uid,
            reporterUsername = user.username,
            isPhoneVerified = user.isPhoneVerified,
            targetType = "VIDEO",
            targetId = videoId,
            targetTitle = targetTitle,
            targetCreator = targetCreator,
            reason = reason,
            details = description
        )
        _latestReportAnalysis.value = reportAnalysis
        reportVideo(videoId, reason, "$description [AI Verified: ${reportAnalysis.aiReasoning}]")
        if (reportAnalysis.recommendedAction == "REMOVE_CONTENT" || reportAnalysis.severity == "CRITICAL") {
            _videos.value = _videos.value.filter { it.id != videoId }
        }
        onResult(reportAnalysis)
    }

    suspend fun submitBanAppeal(
        explanation: String,
        onResult: (com.example.model.BanAppealResult) -> Unit
    ) {
        val user = _currentUser.value
        val appealResult = geminiAiService.reviewBanAppealAndRestore(
            uid = user.uid,
            username = user.username,
            banReason = user.banReason.ifBlank { "Account under security review" },
            userExplanation = explanation,
            isPhoneVerified = user.isPhoneVerified
        )
        _latestBanAppealResult.value = appealResult
        if (appealResult.isRestored) {
            unbanCurrentUser()
        }
        onResult(appealResult)
    }

    fun banUser(uid: String, reason: String) {
        if (_currentUser.value.uid == uid) {
            val updated = _currentUser.value.copy(
                isBanned = true,
                banReason = reason,
                banTicketId = "BAN-" + UUID.randomUUID().toString().take(6).uppercase()
            )
            _currentUser.value = updated
            saveSessionToPrefs(updated)
            syncUserToFirestore(updated)
        }
    }

    fun unbanCurrentUser() {
        val updated = _currentUser.value.copy(
            isBanned = false,
            banReason = "",
            banTicketId = ""
        )
        _currentUser.value = updated
        saveSessionToPrefs(updated)
        syncUserToFirestore(updated)
    }

    suspend fun runAiDiagnostic(
        errorContext: String,
        errorMessage: String,
        onResult: (com.example.model.AiDiagnosticResult) -> Unit
    ) {
        val systemSnapshot = mapOf(
            "isLoggedIn" to _isLoggedIn.value,
            "currentUserUid" to _currentUser.value.uid,
            "coins" to _currentUser.value.coins,
            "balancePkr" to _currentUser.value.balancePkr,
            "activeVideosCount" to _videos.value.size,
            "storagePool" to "cloudinary_active_pool",
            "time" to System.currentTimeMillis()
        )
        val result = geminiAiService.diagnoseAndResolveError(
            errorContext = errorContext,
            errorMessage = errorMessage,
            systemHealthSnapshot = systemSnapshot
        )
        _aiDiagnosticHistory.value = listOf(result) + _aiDiagnosticHistory.value.take(20)
        onResult(result)
    }

    fun submitPaymentGatewayRequest(
        gateway: String,
        accountTitle: String,
        accountNumberOrIban: String,
        amountCoins: Int,
        amountPkr: Double,
        trxId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val user = _currentUser.value
        walletManager.submitPaymentGatewayRequest(
            uid = user.uid,
            userEmail = user.email,
            userHandle = "@${user.username}",
            gateway = gateway,
            accountTitle = accountTitle,
            accountNumberOrIban = accountNumberOrIban,
            amountCoins = amountCoins,
            amountPkr = amountPkr,
            trxId = trxId,
            onResult = onResult
        )
    }

    val paymentRequests = walletManager.paymentRequests

    fun attachPaymentRequestsListener() {
        walletManager.attachPaymentRequestsListener()
    }

    fun adminApprovePaymentRequest(
        requestId: String,
        adminNote: String = "Approved & Verified via JazzCash Gateway",
        onResult: (Boolean, String) -> Unit
    ) {
        walletManager.adminApprovePaymentRequest(
            requestId = requestId,
            adminNote = adminNote,
            onCreditUser = { targetUid, coins, pkr ->
                if (targetUid == _currentUser.value.uid || targetUid.isEmpty() || targetUid == "guest") {
                    addCoins(coins)
                    addPkrBalance(pkr)
                } else {
                    walletManager.adminGrantRevokeUserBalance(
                        targetUid = targetUid,
                        coinAdjustment = coins,
                        pkrAdjustment = pkr,
                        reason = "Payment approved for request $requestId",
                        onResult = { _, _ -> }
                    )
                }
            },
            onResult = onResult
        )
    }

    fun adminRejectPaymentRequest(
        requestId: String,
        reason: String,
        onResult: (Boolean, String) -> Unit
    ) {
        walletManager.adminRejectPaymentRequest(requestId, reason, onResult)
    }

    fun adminGrantRevokeUserBalance(
        targetUid: String,
        coinAdjustment: Int,
        pkrAdjustment: Double,
        reason: String,
        onResult: (Boolean, String) -> Unit
    ) {
        walletManager.adminGrantRevokeUserBalance(
            targetUid = targetUid,
            coinAdjustment = coinAdjustment,
            pkrAdjustment = pkrAdjustment,
            reason = reason,
            onResult = onResult
        )
    }

    fun adminPublishCustomOnlineGame(
        gameTitle: String,
        description: String,
        webUrl: String,
        entryFeeCoins: Int,
        rewardCoins: Int,
        iconEmoji: String,
        onResult: (Boolean, String) -> Unit
    ) {
        walletManager.adminPublishCustomOnlineGame(
            gameTitle = gameTitle,
            description = description,
            webUrl = webUrl,
            entryFeeCoins = entryFeeCoins,
            rewardCoins = rewardCoins,
            iconEmoji = iconEmoji,
            onResult = onResult
        )
    }

    // =========================================================================
    // DYNAMIC FILTER MANAGEMENT (Admin Cloud Controlled Filters)
    // =========================================================================
    val filterManager = DynamicFilterManager.getInstance()
    val availableFilters = filterManager.filters

    fun adminAddFilter(filter: VideoFilter, onResult: (Boolean, String) -> Unit) {
        filterManager.addFilter(filter, onResult)
    }

    fun adminUpdateFilter(filter: VideoFilter, onResult: (Boolean, String) -> Unit) {
        filterManager.updateFilter(filter, onResult)
    }

    fun adminToggleFilterActive(filterId: String, isActive: Boolean, onResult: (Boolean, String) -> Unit) {
        filterManager.toggleFilterActive(filterId, isActive, onResult)
    }

    fun adminDeleteFilter(filterId: String, onResult: (Boolean, String) -> Unit) {
        filterManager.deleteFilter(filterId, onResult)
    }

    fun adminResetDefaultFilters(onResult: (Boolean, String) -> Unit) {
        filterManager.resetToDefaults(onResult)
    }

    // =========================================================================
    // PIXABAY API INTEGRATION (Free Stock HD Videos & Images for Creators)
    // =========================================================================
    val pixabayClient by lazy { com.example.data.pixabay.PixabayClient.getInstance() }

    suspend fun searchPixabayVideos(
        query: String = "trending",
        category: String? = null,
        perPage: Int = 25
    ): Result<List<com.example.data.pixabay.PixabayVideoHit>> {
        return pixabayClient.searchVideos(query = query, category = category, perPage = perPage)
    }

    suspend fun searchPixabayImages(
        query: String = "nature",
        category: String? = null,
        perPage: Int = 20
    ): Result<List<com.example.data.pixabay.PixabayImageHit>> {
        return pixabayClient.searchImages(query = query, category = category, perPage = perPage)
    }

    init {
        FirestoreConfig.configureFirestoreDiskPersistence()
        loadRegisteredAccounts()
        restoreSessionFromPrefsOrFirebase()
        setupAuthStateListener()
        walletManager.attachUserWalletListeners(_currentUser.value.uid)
        _videos.value = emptyList()
        attachRealtimeVideosListener()
        repositoryScope.launch {
            try {
                refreshFeed()
            } catch (e: Exception) {
                android.util.Log.w("VideoRepository", "Initial feed fetch caught: ${e.message}")
            }
        }
        android.util.Log.d(
            "VideoRepository",
            "Firestore initialized with Local Disk Persistence. Live feed ready."
        )
    }
}
