package com.example.model

/**
 * Represents a saved video document reference stored in Firestore under
 * users/{uid}/SavedVideos/{videoId}
 */
data class SavedVideoReference(
    val videoId: String = "",
    val uid: String = "",
    val authorUsername: String = "",
    val videoUrl: String = "",
    val caption: String = "",
    val savedAt: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val musicTitle: String = ""
)
