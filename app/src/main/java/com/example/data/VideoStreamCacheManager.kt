package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resilient MP4 Stream Cache & URL Sanitizer.
 * - Replaces any broken/403 URLs (such as commondatastorage.googleapis.com or test-videos.co.uk)
 *   with verified HTTP 200 fast-start H.264 MP4 streams.
 * - Downloads and caches streams via OkHttp to local cacheDir before media playback so the native
 *   media pipeline never encounters HTTP 403/404 responses.
 */
object VideoStreamCacheManager {
    private const val TAG = "VideoStreamCache"

    val VERIFIED_STREAM_URLS = listOf(
        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
        "https://media.w3.org/2010/05/video/movie_300.mp4",
        "https://media.w3.org/2010/05/sintel/trailer.mp4",
        "https://media.w3.org/2010/05/video/movie_90.mp4",
        "https://media.w3.org/2010/05/bunny/trailer.mp4",
        "https://vjs.zencdn.net/v/oceans.mp4"
    )

    private const val FAST_FALLBACK_URL = "https://media.w3.org/2010/05/video/movie_90.mp4"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val urlLocks = ConcurrentHashMap<String, Mutex>()

    fun sanitizeVideoUrl(rawUrl: String, index: Int = 0): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return VERIFIED_STREAM_URLS[kotlin.math.abs(index) % VERIFIED_STREAM_URLS.size]
        }
        val lower = trimmed.lowercase()
        val isLegacyOrForbidden = lower.contains("commondatastorage.googleapis.com") ||
            lower.contains("test-videos.co.uk") ||
            lower.contains("w3schools.com") ||
            lower.contains("res.cloudinary.com/tashan_media")
        return if (isLegacyOrForbidden) {
            val hashIndex = if (index != 0) index else kotlin.math.abs(trimmed.hashCode())
            VERIFIED_STREAM_URLS[kotlin.math.abs(hashIndex) % VERIFIED_STREAM_URLS.size]
        } else {
            trimmed
        }
    }

    suspend fun resolvePlayableFile(context: Context, rawUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            val cacheFolder = File(context.cacheDir, "tashan_mp4_cache").apply {
                if (!exists()) mkdirs()
            }

            // 1. Direct local file path
            if (rawUrl.startsWith("/")) {
                val localFile = File(rawUrl)
                if (localFile.exists() && localFile.length() > 0L) return@withContext localFile
            }

            // 2. file:// or content:// URI
            if (rawUrl.startsWith("file://") || rawUrl.startsWith("content://")) {
                val uri = Uri.parse(rawUrl)
                if (rawUrl.startsWith("file://")) {
                    val path = uri.path
                    if (!path.isNullOrBlank()) {
                        val file = File(path)
                        if (file.exists() && file.length() > 0L) return@withContext file
                    }
                }
                val localCopy = File(cacheFolder, "local_${rawUrl.hashCode().toUInt()}.mp4")
                if (localCopy.exists() && localCopy.length() > 1024L) {
                    return@withContext localCopy
                }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(localCopy).use { output ->
                        input.copyTo(output)
                    }
                }
                if (localCopy.exists() && localCopy.length() > 0L) {
                    return@withContext localCopy
                }
            }

            // 3. Remote HTTP/HTTPS URL -> sanitize & download via OkHttp
            val cleanUrl = sanitizeVideoUrl(rawUrl)
            downloadToCache(cacheFolder, cleanUrl)
                ?: downloadToCache(cacheFolder, FAST_FALLBACK_URL)
                ?: downloadToCache(cacheFolder, VERIFIED_STREAM_URLS.first())
        } catch (e: Exception) {
            Log.w(TAG, "Failed resolving playable file for $rawUrl: ${e.message}")
            null
        }
    }

    private suspend fun downloadToCache(cacheFolder: File, url: String): File? {
        val key = url.hashCode().toUInt().toString()
        val targetFile = File(cacheFolder, "stream_$key.mp4")
        if (targetFile.exists() && targetFile.length() > 10_000L) {
            return targetFile
        }

        val mutex = urlLocks.getOrPut(key) { Mutex() }
        return mutex.withLock {
            if (targetFile.exists() && targetFile.length() > 10_000L) {
                return@withLock targetFile
            }
            val tempFile = File(cacheFolder, "stream_${key}_${System.nanoTime()}.tmp")
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/120.0.0.0 Mobile")
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Stream HTTP ${response.code} for $url, using fallback")
                        return@withLock null
                    }
                    val body = response.body ?: return@withLock null
                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                if (tempFile.exists() && tempFile.length() > 10_000L) {
                    tempFile.renameTo(targetFile)
                    return@withLock if (targetFile.exists()) targetFile else tempFile
                } else {
                    tempFile.delete()
                    null
                }
            } catch (e: Exception) {
                tempFile.delete()
                Log.w(TAG, "Stream download warning for $url: ${e.message}")
                null
            }
        }
    }
}
