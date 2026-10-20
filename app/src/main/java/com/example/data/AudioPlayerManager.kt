package com.example.data

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioTrack(
    val id: String,
    val title: String,
    val artist: String,
    val category: String, // "NAAT", "PUNJABI", "URDU_HINDI"
    val audioUrl: String,
    val coverUrl: String,
    val durationSec: Int = 180
)

data class AudioPlaybackState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val isShuffle: Boolean = false,
    val isLooping: Boolean = false
)

/**
 * Dedicated Audio Pipeline for streaming Naats, Punjabi Songs,
 * and Urdu/Hindi music tracks smoothly.
 */
class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _playlist = MutableStateFlow<List<AudioTrack>>(emptyList())
    val playlist: StateFlow<List<AudioTrack>> = _playlist.asStateFlow()

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var currentTrackIndex: Int = 0

    init {
        initializeCuratedTracks()
        // Do not eagerly instantiate ExoPlayer; instantiate lazily on play to prevent codec exhaustion
    }

    private fun initializeCuratedTracks() {
        val tracks = listOf(
            AudioTrack(
                id = "naat_01",
                title = "Faslon Ko Takalluf Hai Humse Agar",
                artist = "Qari Waheed Zafar Qasmi",
                category = "NAAT",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                coverUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=500",
                durationSec = 372
            ),
            AudioTrack(
                id = "naat_02",
                title = "Tajdar-e-Haram Ae Shehenshah-e-Deen",
                artist = "Sabri Brothers Sufi Heritage",
                category = "NAAT",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                coverUrl = "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=500",
                durationSec = 420
            ),
            AudioTrack(
                id = "punjabi_01",
                title = "Pasoori (Coke Studio Official)",
                artist = "Ali Sethi & Shae Gill",
                category = "PUNJABI",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500",
                durationSec = 224
            ),
            AudioTrack(
                id = "punjabi_02",
                title = "Amplifier Bass Mix",
                artist = "Imran Khan Urban Punjabi",
                category = "PUNJABI",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500",
                durationSec = 210
            ),
            AudioTrack(
                id = "urdu_01",
                title = "Tu Hai Kahan - Soulful Strings",
                artist = "AUR Band Official",
                category = "URDU_HINDI",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500",
                durationSec = 255
            ),
            AudioTrack(
                id = "urdu_02",
                title = "Kahani Suno 2.0 Acoustic",
                artist = "Kaifi Khalil",
                category = "URDU_HINDI",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500",
                durationSec = 290
            )
        )
        _playlist.value = tracks
    }

    fun playTrack(track: AudioTrack) {
        val index = _playlist.value.indexOf(track)
        if (index != -1) currentTrackIndex = index

        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            isBuffering = true,
            isPlaying = false
        )

        isPrepared = false
        mediaPlayer?.let { old ->
            runCatching {
                old.setOnPreparedListener(null)
                old.setOnCompletionListener(null)
                old.setOnErrorListener(null)
                old.reset()
                old.release()
            }
        }
        mediaPlayer = null

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(track.audioUrl)
                setOnPreparedListener { mp ->
                    isPrepared = true
                    val dur = if (mp.duration > 0) mp.duration.toLong() else track.durationSec * 1000L
                    runCatching { mp.start() }
                    _playbackState.value = _playbackState.value.copy(
                        isBuffering = false,
                        isPlaying = true,
                        totalDurationMs = dur
                    )
                    startProgressTracker()
                }
                setOnCompletionListener {
                    nextTrack()
                }
                setOnErrorListener { _, _, _ ->
                    isPrepared = false
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        isBuffering = false
                    )
                    stopProgressTracker()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            android.util.Log.w("AudioPlayerManager", "Audio setup warning: ${e.message}")
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                isBuffering = false
            )
        }
    }

    fun playTrack(index: Int) {
        val tracks = _playlist.value
        if (index in tracks.indices) {
            playTrack(tracks[index])
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player == null || !isPrepared) {
            val track = _playbackState.value.currentTrack ?: _playlist.value.firstOrNull()
            if (track != null) playTrack(track)
            return
        }
        runCatching {
            if (player.isPlaying) {
                player.pause()
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
                stopProgressTracker()
            } else {
                player.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                startProgressTracker()
            }
        }
    }

    fun nextTrack() {
        if (_playlist.value.isEmpty()) return
        currentTrackIndex = (currentTrackIndex + 1) % _playlist.value.size
        playTrack(_playlist.value[currentTrackIndex])
    }

    fun previousTrack() {
        if (_playlist.value.isEmpty()) return
        currentTrackIndex = if (currentTrackIndex - 1 < 0) _playlist.value.size - 1 else currentTrackIndex - 1
        playTrack(_playlist.value[currentTrackIndex])
    }

    fun seekTo(positionMs: Long) {
        if (isPrepared) {
            runCatching { mediaPlayer?.seekTo(positionMs.toInt()) }
        }
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = mediaPlayer
                if (player != null && isPrepared) {
                    runCatching {
                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = player.currentPosition.toLong(),
                            totalDurationMs = if (player.duration > 0) player.duration.toLong() else _playbackState.value.totalDurationMs
                        )
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    fun release() {
        stopProgressTracker()
        isPrepared = false
        mediaPlayer?.let { old ->
            runCatching {
                old.reset()
                old.release()
            }
        }
        mediaPlayer = null
    }
}
