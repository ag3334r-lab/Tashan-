package com.example.data.pixabay

import com.squareup.moshi.Json

data class PixabayVideoResponse(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "totalHits") val totalHits: Int = 0,
    @Json(name = "hits") val hits: List<PixabayVideoHit> = emptyList()
)

data class PixabayVideoHit(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "pageURL") val pageURL: String = "",
    @Json(name = "type") val type: String = "film",
    @Json(name = "tags") val tags: String = "",
    @Json(name = "duration") val duration: Int = 0,
    @Json(name = "picture_id") val pictureId: String = "",
    @Json(name = "videos") val videos: PixabayVideoFormats = PixabayVideoFormats(),
    @Json(name = "views") val views: Long = 0,
    @Json(name = "downloads") val downloads: Long = 0,
    @Json(name = "likes") val likes: Long = 0,
    @Json(name = "comments") val comments: Long = 0,
    @Json(name = "user_id") val userId: Long = 0,
    @Json(name = "user") val user: String = "",
    @Json(name = "userImageURL") val userImageURL: String = ""
) {
    /**
     * Resolves the best streamable video URL (prioritizing medium/small MP4 for mobile bandwidth)
     */
    fun getStreamUrl(): String {
        return videos.medium.url.ifBlank {
            videos.small.url.ifBlank {
                videos.large.url.ifBlank {
                    videos.tiny.url
                }
            }
        }
    }

    /**
     * Thumbnail image for the video preview
     */
    fun getThumbnailUrl(): String {
        return if (pictureId.isNotBlank()) {
            "https://i.vimeocdn.com/video/${pictureId}_640x360.jpg"
        } else {
            "https://cdn.pixabay.com/photo/2015/04/23/22/00/tree-736885_1280.jpg"
        }
    }
}

data class PixabayVideoFormats(
    @Json(name = "large") val large: PixabayVideoFormat = PixabayVideoFormat(),
    @Json(name = "medium") val medium: PixabayVideoFormat = PixabayVideoFormat(),
    @Json(name = "small") val small: PixabayVideoFormat = PixabayVideoFormat(),
    @Json(name = "tiny") val tiny: PixabayVideoFormat = PixabayVideoFormat()
)

data class PixabayVideoFormat(
    @Json(name = "url") val url: String = "",
    @Json(name = "width") val width: Int = 0,
    @Json(name = "height") val height: Int = 0,
    @Json(name = "size") val size: Long = 0
)

data class PixabayImageResponse(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "totalHits") val totalHits: Int = 0,
    @Json(name = "hits") val hits: List<PixabayImageHit> = emptyList()
)

data class PixabayImageHit(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "pageURL") val pageURL: String = "",
    @Json(name = "tags") val tags: String = "",
    @Json(name = "previewURL") val previewURL: String = "",
    @Json(name = "webformatURL") val webformatURL: String = "",
    @Json(name = "largeImageURL") val largeImageURL: String = "",
    @Json(name = "views") val views: Long = 0,
    @Json(name = "downloads") val downloads: Long = 0,
    @Json(name = "likes") val likes: Long = 0,
    @Json(name = "user") val user: String = "",
    @Json(name = "userImageURL") val userImageURL: String = ""
)

/**
 * Represents a live visual effect, motion overlay, or camera filter fetched from Pixabay.
 */
data class PixabayVisualEffect(
    val id: String,
    val title: String,
    val category: String, // "Motion Overlay", "Neon & Cyber", "Particles & Bokeh", "Glitch & VHS", "Nature & Fire"
    val tags: String,
    val thumbnailUrl: String,
    val streamVideoUrl: String,
    val tintColorHex: String = "#00F2FE",
    val blendAlpha: Float = 0.22f,
    val creatorName: String = "Pixabay Creator",
    val likes: Long = 0,
    val downloads: Long = 0,
    val badge: String = "PIXABAY FX"
)

/**
 * Represents a music track, background beat, sound effect (SFX), or voice clip from Pixabay.
 */
data class PixabayAudioItem(
    val id: String,
    val title: String,
    val artist: String,
    val category: String, // "Trending Music", "Background Beats", "Sound Effects", "Voice & Clips"
    val streamUrl: String,
    val thumbnailUrl: String,
    val durationSec: Int,
    val tags: String,
    val likes: Long = 0,
    val downloads: Long = 0,
    val isLivePixabayHit: Boolean = true
)

