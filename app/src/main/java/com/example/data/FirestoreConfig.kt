package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Utility to configure Firebase Firestore with local disk persistence.
 * Enables offline caching with unlimited cache size so that the video feed,
 * user profile, and comments remain accessible when the network is poor or offline.
 */
object FirestoreConfig {
    private const val TAG = "FirestoreConfig"
    var isPersistenceConfigured: Boolean = false
        private set

    /**
     * Configures Firestore settings to use persistent disk cache.
     */
    fun configureFirestoreDiskPersistence(context: Context? = null) {
        try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val db = FirebaseFirestore.getInstance()
            // Configure local disk persistence settings (unlimited cache size)
            val cacheSettings = PersistentCacheSettings.newBuilder()
                .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()
            db.firestoreSettings = settings
            isPersistenceConfigured = true
            Log.d(TAG, "Firebase Firestore local disk persistence enabled (Cache size: UNLIMITED).")
        } catch (e: IllegalStateException) {
            // Already initialized or FirebaseApp not yet available in current runtime
            Log.w(TAG, "Firestore settings note: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure Firestore persistence: ${e.message}", e)
        }
    }
}
