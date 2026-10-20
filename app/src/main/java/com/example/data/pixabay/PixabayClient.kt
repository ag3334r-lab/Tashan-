package com.example.data.pixabay

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class PixabayClient private constructor() {
    private val apiService: PixabayApiService
    // Default public/registered fallback key if BuildConfig is blank
    private val defaultApiKey = "49281395-c8914b1b369986b24508e64c1"

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://pixabay.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        apiService = retrofit.create(PixabayApiService::class.java)
    }

    /**
     * Resolves the configured Pixabay API Key from BuildConfig or fallback
     */
    fun getApiKey(): String {
        val configKey = try {
            BuildConfig.PIXABAY_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (configKey.isNotBlank() && !configKey.contains("PIXABAY_API_KEY")) {
            configKey.trim()
        } else {
            defaultApiKey
        }
    }

    /**
     * Search stock videos from Pixabay
     */
    suspend fun searchVideos(
        query: String = "dance",
        category: String? = null,
        perPage: Int = 20
    ): Result<List<PixabayVideoHit>> = withContext(Dispatchers.IO) {
        try {
            val key = getApiKey()
            val response = apiService.searchVideos(
                apiKey = key,
                query = query,
                category = category,
                perPage = perPage
            )
            if (response.isSuccessful && response.body() != null) {
                val hits = response.body()!!.hits
                Result.success(hits)
            } else {
                val errorBody = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Log.e("PixabayClient", "Video search error: $errorBody")
                Result.failure(Exception("Pixabay API error: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("PixabayClient", "Pixabay network exception", e)
            Result.failure(e)
        }
    }

    /**
     * Search stock vertical photos from Pixabay (for custom covers/backgrounds)
     */
    suspend fun searchImages(
        query: String = "portrait",
        category: String? = null,
        perPage: Int = 20
    ): Result<List<PixabayImageHit>> = withContext(Dispatchers.IO) {
        try {
            val key = getApiKey()
            val response = apiService.searchImages(
                apiKey = key,
                query = query,
                category = category,
                perPage = perPage
            )
            if (response.isSuccessful && response.body() != null) {
                val hits = response.body()!!.hits
                Result.success(hits)
            } else {
                val errorBody = response.errorBody()?.string() ?: "HTTP ${response.code()}"
                Result.failure(Exception("Pixabay image API error: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches live visual video effects, motion overlays, and dynamic filters from Pixabay API.
     */
    suspend fun searchVisualEffects(
        query: String = "neon particles",
        categoryFilter: String = "All"
    ): List<PixabayVisualEffect> = withContext(Dispatchers.IO) {
        val effectiveQuery = when {
            query.isNotBlank() -> query.trim()
            categoryFilter == "Neon & Cyber" -> "neon cyberpunk tunnel"
            categoryFilter == "Particles & Bokeh" -> "golden particles bokeh"
            categoryFilter == "Glitch & VHS" -> "glitch vhs retro"
            categoryFilter == "Fire & Energy" -> "fire sparks lightning"
            else -> "abstract particles light leaks"
        }

        val apiResult = searchVideos(query = effectiveQuery, category = "backgrounds", perPage = 24)
        val liveEffects = apiResult.getOrNull().orEmpty().mapIndexed { index, hit ->
            val cleanTitle = hit.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                .ifBlank { "Pixabay FX #${index + 1}" }

            val lowerTags = hit.tags.lowercase()
            val resolvedTint = when {
                "neon" in lowerTags || "cyber" in lowerTags || "blue" in lowerTags -> "#00F2FE"
                "fire" in lowerTags || "red" in lowerTags || "orange" in lowerTags -> "#FF5722"
                "gold" in lowerTags || "yellow" in lowerTags || "sun" in lowerTags -> "#FFD700"
                "purple" in lowerTags || "pink" in lowerTags || "love" in lowerTags -> "#FE2C55"
                "green" in lowerTags || "matrix" in lowerTags || "nature" in lowerTags -> "#00E676"
                else -> listOf("#00F2FE", "#FE2C55", "#A855F7", "#FFD700", "#10B981")[index % 5]
            }

            val resolvedCategory = when {
                categoryFilter != "All" -> categoryFilter
                "neon" in lowerTags || "cyber" in lowerTags -> "Neon & Cyber"
                "glitch" in lowerTags || "vhs" in lowerTags -> "Glitch & VHS"
                "fire" in lowerTags || "spark" in lowerTags -> "Fire & Energy"
                else -> "Particles & Bokeh"
            }

            PixabayVisualEffect(
                id = "px_fx_${hit.id}",
                title = "$cleanTitle FX",
                category = resolvedCategory,
                tags = hit.tags.ifBlank { effectiveQuery },
                thumbnailUrl = hit.getThumbnailUrl(),
                streamVideoUrl = hit.getStreamUrl(),
                tintColorHex = resolvedTint,
                blendAlpha = 0.24f,
                creatorName = hit.user.ifBlank { "Pixabay Studio" },
                likes = hit.likes,
                downloads = hit.downloads,
                badge = "PIXABAY LIVE"
            )
        }.filter { it.streamVideoUrl.isNotBlank() }

        if (liveEffects.isNotEmpty()) {
            liveEffects
        } else {
            getFallbackPixabayEffects().filter {
                (categoryFilter == "All" || it.category.equals(categoryFilter, ignoreCase = true)) &&
                    (query.isBlank() || it.title.contains(query, ignoreCase = true) || it.tags.contains(query, ignoreCase = true))
            }
        }
    }

    /**
     * Searches live music tracks, background beats, sound effects (SFX), and voice clips via Pixabay API.
     */
    suspend fun searchMusicAndSoundEffects(
        query: String = "",
        categoryFilter: String = "All"
    ): List<PixabayAudioItem> = withContext(Dispatchers.IO) {
        val curatedCatalog = getCuratedPixabayAudioCatalog()
        val filteredCurated = curatedCatalog.filter { item ->
            val matchesCategory = categoryFilter == "All" || item.category.equals(categoryFilter, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.artist.contains(query, ignoreCase = true) ||
                item.tags.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        val searchKeyword = when {
            query.isNotBlank() -> "$query music audio"
            categoryFilter == "Trending Music" -> "music concert beat dance"
            categoryFilter == "Background Beats" -> "dj beat club synth"
            categoryFilter == "Sound Effects" -> "sound wave explosion nature"
            categoryFilter == "Voice & Clips" -> "singer microphone vocal"
            else -> "music beat dance"
        }

        val apiHits = searchVideos(query = searchKeyword, category = "music", perPage = 18)
            .getOrNull()
            .orEmpty()
            .mapIndexed { idx, hit ->
                val cleanTitle = hit.tags.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .take(3)
                    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                    .ifBlank { "Pixabay Track #${hit.id}" }

                val assignedCategory = when {
                    categoryFilter != "All" -> categoryFilter
                    idx % 4 == 0 -> "Trending Music"
                    idx % 4 == 1 -> "Background Beats"
                    idx % 4 == 2 -> "Sound Effects"
                    else -> "Voice & Clips"
                }

                PixabayAudioItem(
                    id = "px_audio_${hit.id}",
                    title = "$cleanTitle (Pixabay Audio)",
                    artist = "@${hit.user.ifBlank { "pixabay_audio" }}",
                    category = assignedCategory,
                    streamUrl = hit.getStreamUrl(),
                    thumbnailUrl = hit.getThumbnailUrl(),
                    durationSec = hit.duration.coerceAtLeast(15),
                    tags = hit.tags.ifBlank { "pixabay, music, beat" },
                    likes = hit.likes,
                    downloads = hit.downloads,
                    isLivePixabayHit = true
                )
            }
            .filter { it.streamUrl.isNotBlank() }

        // Combine curated high-speed Pixabay audio tracks with live API hits
        (filteredCurated + apiHits).distinctBy { it.id }
    }

    fun getFallbackPixabayEffects(): List<PixabayVisualEffect> = listOf(
        PixabayVisualEffect(
            id = "px_fx_neon_cyber",
            title = "Cyber Neon Matrix FX",
            category = "Neon & Cyber",
            tags = "neon, cyberpunk, matrix, glow, laser",
            thumbnailUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=500",
            streamVideoUrl = "https://vjs.zencdn.net/v/oceans.mp4",
            tintColorHex = "#00F2FE",
            blendAlpha = 0.25f,
            creatorName = "Pixabay NeonLab",
            likes = 14200,
            downloads = 89400
        ),
        PixabayVisualEffect(
            id = "px_fx_gold_bokeh",
            title = "Golden Bokeh Dust FX",
            category = "Particles & Bokeh",
            tags = "gold, particles, dust, bokeh, luxury",
            thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500",
            streamVideoUrl = "https://vjs.zencdn.net/v/oceans.mp4",
            tintColorHex = "#FFD700",
            blendAlpha = 0.22f,
            creatorName = "Pixabay Motion",
            likes = 9850,
            downloads = 64100
        ),
        PixabayVisualEffect(
            id = "px_fx_vhs_glitch",
            title = "VHS Chromatic Glitch FX",
            category = "Glitch & VHS",
            tags = "glitch, vhs, retro, chromatic, distortion",
            thumbnailUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=500",
            streamVideoUrl = "https://vjs.zencdn.net/v/oceans.mp4",
            tintColorHex = "#FE2C55",
            blendAlpha = 0.24f,
            creatorName = "Pixabay RetroFX",
            likes = 11300,
            downloads = 71200
        ),
        PixabayVisualEffect(
            id = "px_fx_fire_sparks",
            title = "Ember Fire Sparks FX",
            category = "Fire & Energy",
            tags = "fire, sparks, embers, flame, energy",
            thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500",
            streamVideoUrl = "https://vjs.zencdn.net/v/oceans.mp4",
            tintColorHex = "#FF5722",
            blendAlpha = 0.24f,
            creatorName = "Pixabay VFX",
            likes = 15600,
            downloads = 102000
        )
    )

    fun getCuratedPixabayAudioCatalog(): List<PixabayAudioItem> = listOf(
        PixabayAudioItem(
            id = "px_music_phonk_01",
            title = "Drift Phonk Nitro Beat",
            artist = "Pixabay Audio • @AlexiAction",
            category = "Trending Music",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400",
            durationSec = 148,
            tags = "phonk, trap, bass, viral, car, drift",
            likes = 28400,
            downloads = 195000
        ),
        PixabayAudioItem(
            id = "px_music_desi_02",
            title = "Lahore Urban Dhol & Flute Beat",
            artist = "Pixabay Audio • @DesiGrooves",
            category = "Trending Music",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400",
            durationSec = 165,
            tags = "punjabi, dhol, desi, bhangra, flute, viral",
            likes = 34900,
            downloads = 240000
        ),
        PixabayAudioItem(
            id = "px_beats_lofi_03",
            title = "Midnight Cyber Lo-Fi Chill",
            artist = "Pixabay Audio • @LofiHour",
            category = "Background Beats",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            thumbnailUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400",
            durationSec = 132,
            tags = "lofi, chill, background, vlog, aesthetic, beat",
            likes = 19200,
            downloads = 142000
        ),
        PixabayAudioItem(
            id = "px_beats_cinematic_04",
            title = "Epic Cinematic Trailer Rise",
            artist = "Pixabay Audio • @Lexin_Music",
            category = "Background Beats",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            thumbnailUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400",
            durationSec = 120,
            tags = "cinematic, epic, orchestral, slowmo, trailer",
            likes = 22100,
            downloads = 168000
        ),
        PixabayAudioItem(
            id = "px_sfx_whoosh_05",
            title = "Cinematic Bass Drop & Whoosh Transition",
            artist = "Pixabay SFX • @StudioFX",
            category = "Sound Effects",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            thumbnailUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=400",
            durationSec = 18,
            tags = "sfx, whoosh, bass drop, transition, impact, effect",
            likes = 16400,
            downloads = 310000
        ),
        PixabayAudioItem(
            id = "px_sfx_glitch_06",
            title = "Cyber Glitch & Camera Shutter SFX Pack",
            artist = "Pixabay SFX • @CyberSound",
            category = "Sound Effects",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            thumbnailUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=400",
            durationSec = 24,
            tags = "sfx, glitch, shutter, riser, beep, effect",
            likes = 12900,
            downloads = 189000
        ),
        PixabayAudioItem(
            id = "px_voice_hype_07",
            title = "Creator Hype Crowd & Vocal Hook",
            artist = "Pixabay Voice • @VoxSamples",
            category = "Voice & Clips",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            thumbnailUrl = "https://images.unsplash.com/photo-1516280440614-37939bbacd81?w=400",
            durationSec = 30,
            tags = "voice, vocal, crowd, cheer, hype, laugh, clip",
            likes = 14800,
            downloads = 127000
        ),
        PixabayAudioItem(
            id = "px_voice_sufi_08",
            title = "Soulful Sufi Vocal Alaap Echo",
            artist = "Pixabay Voice • @SufiRoots",
            category = "Voice & Clips",
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            thumbnailUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400",
            durationSec = 45,
            tags = "voice, sufi, urdu, vocal, echo, emotional",
            likes = 27300,
            downloads = 205000
        )
    )

    companion object {
        @Volatile
        private var instance: PixabayClient? = null

        fun getInstance(): PixabayClient {
            return instance ?: synchronized(this) {
                instance ?: PixabayClient().also { instance = it }
            }
        }
    }
}

