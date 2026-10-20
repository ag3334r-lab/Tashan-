package com.example.data

import android.net.Uri
import android.util.Log
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

enum class BackupSyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

data class UserPreferencesData(
    val themeAccent: String = "#FE2C55",
    val isPrivate: Boolean = false,
    val autoPlayVideos: Boolean = true,
    val faceTrackingSensitivity: Float = 1.0f,
    val audioStreamingQuality: String = "HIGH",
    val notificationsEnabled: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class UserActivityLog(
    val activityType: String, // "LIKE_VIDEO", "SAVE_VIDEO", "WATCH_VIDEO", "POST_COMMENT", "GAME_MATCH"
    val targetId: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val readableTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
)

/**
 * Real-time User Data Backup System syncing user profile, preferences,
 * and activity history to Firebase Firestore & Cloud Storage.
 */
class UserBackupManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _syncStatus = MutableStateFlow(BackupSyncStatus.IDLE)
    val syncStatus: StateFlow<BackupSyncStatus> = _syncStatus.asStateFlow()

    private val _lastBackupTime = MutableStateFlow<String?>("Never")
    val lastBackupTime: StateFlow<String?> = _lastBackupTime.asStateFlow()

    /**
     * Real-time backup of User Profile to /users/{uid}
     */
    fun backupUserProfile(profile: UserProfile, onComplete: ((Boolean) -> Unit)? = null) {
        if (profile.uid.isBlank() || profile.uid == "guest") return
        scope.launch {
            try {
                _syncStatus.value = BackupSyncStatus.SYNCING
                val profileData = hashMapOf(
                    "uid" to profile.uid,
                    "username" to profile.username,
                    "displayName" to profile.displayName,
                    "bio" to profile.bio,
                    "avatarUrl" to profile.avatarUrl,
                    "email" to profile.email,
                    "phoneNumber" to profile.phoneNumber,
                    "country" to profile.country,
                    "authProvider" to profile.authProvider,
                    "coins" to profile.coins,
                    "balancePkr" to profile.balancePkr,
                    "postsCount" to profile.postsCount,
                    "followersCount" to profile.followersCount,
                    "followingCount" to profile.followingCount,
                    "themeAccentHex" to profile.themeAccentHex,
                    "isPrivate" to profile.isPrivate,
                    "lastBackupTimestamp" to System.currentTimeMillis()
                )
                firestore.collection("users").document(profile.uid)
                    .set(profileData, SetOptions.merge())
                    .await()

                val timeStr = SimpleDateFormat("HH:mm:ss a", Locale.getDefault()).format(Date())
                _lastBackupTime.value = timeStr
                _syncStatus.value = BackupSyncStatus.SUCCESS
                Log.d("UserBackupManager", "Profile backed up to Firestore for uid: ${profile.uid}")
                onComplete?.invoke(true)
            } catch (e: Exception) {
                Log.e("UserBackupManager", "Failed to backup profile to Firestore", e)
                _syncStatus.value = BackupSyncStatus.ERROR
                onComplete?.invoke(false)
            }
        }
    }

    /**
     * Real-time backup of User App Preferences to /users/{uid}/preferences/settings
     */
    fun backupUserPreferences(uid: String, preferences: UserPreferencesData) {
        if (uid.isBlank() || uid == "guest") return
        scope.launch {
            try {
                firestore.collection("users").document(uid)
                    .collection("preferences").document("settings")
                    .set(preferences, SetOptions.merge())
                    .await()
                Log.d("UserBackupManager", "User preferences backed up for uid: $uid")
            } catch (e: Exception) {
                Log.w("UserBackupManager", "Error syncing preferences", e)
            }
        }
    }

    /**
     * Real-time logging of user activity to /users/{uid}/activity/
     */
    fun logUserActivity(uid: String, activityType: String, targetId: String, details: String = "") {
        if (uid.isBlank() || uid == "guest") return
        scope.launch {
            try {
                val log = UserActivityLog(
                    activityType = activityType,
                    targetId = targetId,
                    details = details
                )
                firestore.collection("users").document(uid)
                    .collection("activity")
                    .add(log)
                    .await()
            } catch (e: Exception) {
                Log.w("UserBackupManager", "Error logging user activity to Firestore", e)
            }
        }
    }

    /**
     * Upload and backup user avatar or media asset to Firebase Cloud Storage.
     */
    suspend fun uploadAvatarToCloudStorage(uid: String, fileUri: Uri): Result<String> {
        return try {
            val storageRef = storage.reference.child("users/$uid/avatars/avatar_${System.currentTimeMillis()}.jpg")
            storageRef.putFile(fileUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            firestore.collection("users").document(uid)
                .update("avatarUrl", downloadUrl)
                .await()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("UserBackupManager", "Error uploading avatar to Firebase Storage", e)
            Result.failure(e)
        }
    }
}
