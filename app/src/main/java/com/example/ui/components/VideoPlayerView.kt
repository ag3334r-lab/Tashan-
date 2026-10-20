package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.view.Surface
import android.view.TextureView
import com.example.data.VideoStreamCacheManager
import com.example.model.VideoItem
import com.example.model.VideoResolution
import com.example.ui.theme.*
import java.io.FileInputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class HeartBurst(
    val id: Long = System.currentTimeMillis() + Random.nextLong(100000),
    val offset: Offset,
    val rotation: Float = (Random.nextFloat() * 34f) - 17f
)

private fun applyCenterCropTransform(textureView: TextureView, videoWidth: Int, videoHeight: Int) {
    val viewWidth = textureView.width
    val viewHeight = textureView.height
    if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return
    val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
    val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()
    val scaleX: Float
    val scaleY: Float
    if (videoRatio > viewRatio) {
        scaleX = videoRatio / viewRatio
        scaleY = 1f
    } else {
        scaleX = 1f
        scaleY = viewRatio / videoRatio
    }
    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerView(
    videoItem: VideoItem,
    isActive: Boolean,
    onLikeToggle: () -> Unit,
    onSaveToggle: () -> Unit,
    onFollowToggle: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: () -> Unit = {},
    onVotePoll: (Int) -> Unit,
    onViewWatched: () -> Unit = {},
    onWatchDwell: (String, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isMuted by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(1.0f) }
    var isPlaying by remember { mutableStateOf(true) }
    var heartBursts by remember { mutableStateOf<List<HeartBurst>>(emptyList()) }
    var isCaptionExpanded by remember { mutableStateOf(false) }
    var showPauseIndicator by remember { mutableStateOf(false) }

    // Continuous Watch-Time AI Affinity Tracker (60s and 120s dwell)
    LaunchedEffect(isActive, isPlaying, videoItem.id) {
        if (isActive && isPlaying) {
            var elapsedSeconds = 0
            while (isActive && isPlaying) {
                kotlinx.coroutines.delay(1000)
                elapsedSeconds++
                if (elapsedSeconds == 60 || elapsedSeconds == 120) {
                    onWatchDwell(videoItem.id, elapsedSeconds)
                }
            }
        }
    }

    // Video Quality & Multi-Resolution Storage Stream State
    var selectedQualityKey by remember(videoItem.id) { mutableStateOf("auto") }
    var activeStreamUrl by remember(videoItem.id, videoItem.videoUrl) {
        mutableStateOf(VideoStreamCacheManager.sanitizeVideoUrl(videoItem.videoUrl))
    }
    var reloadTrigger by remember { mutableIntStateOf(0) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var qualityToastMessage by remember { mutableStateOf<String?>(null) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var activeSurface by remember { mutableStateOf<Surface?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var isVideoReady by remember { mutableStateOf(false) }
    var hasPlaybackError by remember { mutableStateOf(false) }
    var hasRecordedView by remember(videoItem.id) { mutableStateOf(false) }

    LaunchedEffect(isActive, activeStreamUrl, reloadTrigger) {
        if (isActive && activeStreamUrl.isNotBlank()) {
            isVideoReady = false
            hasPlaybackError = false
            if (!hasRecordedView) {
                hasRecordedView = true
                onViewWatched()
            }

            val cachedFile = VideoStreamCacheManager.resolvePlayableFile(context, activeStreamUrl)
            if (cachedFile == null || !cachedFile.exists()) {
                hasPlaybackError = true
                return@LaunchedEffect
            }

            // Release any previous player instance
            mediaPlayer?.let { old ->
                runCatching {
                    old.setOnPreparedListener(null)
                    old.setOnErrorListener(null)
                    old.setSurface(null)
                    old.reset()
                    old.release()
                }
            }
            mediaPlayer = null

            val player = try {
                MediaPlayer().apply {
                    FileInputStream(cachedFile).use { fis ->
                        setDataSource(fis.fd)
                    }
                    isLooping = true
                    val vol = if (isMuted) 0f else 1f
                    setVolume(vol, vol)
                    activeSurface?.let { surf ->
                        if (surf.isValid) setSurface(surf)
                    }
                    setOnVideoSizeChangedListener { _, w, h ->
                        videoWidth = w
                        videoHeight = h
                        textureViewRef?.let { tv -> applyCenterCropTransform(tv, w, h) }
                    }
                    setOnPreparedListener { mp ->
                        coroutineScope.launch {
                            // Artificial network/buffering delay as requested by user for "TikTok feel"
                            delay((400..1200).random().toLong())
                            isVideoReady = true
                            hasPlaybackError = false
                            val v = if (isMuted) 0f else 1f
                            mp.setVolume(v, v)
                            runCatching {
                                if (speed != 1.0f) {
                                    mp.playbackParams = PlaybackParams().setSpeed(speed.coerceIn(0.25f, 3.0f))
                                }
                            }
                            if (isPlaying) {
                                runCatching { mp.start() }
                            } else {
                                runCatching { mp.pause() }
                            }
                        }
                    }
                    setOnErrorListener { _, _, _ ->
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                android.util.Log.w("VideoPlayerView", "MediaPlayer setup fallback: ${e.message}")
                hasPlaybackError = true
                null
            }
            mediaPlayer = player
        } else {
            mediaPlayer?.let { old ->
                runCatching {
                    old.setOnPreparedListener(null)
                    old.setOnErrorListener(null)
                    old.setSurface(null)
                    old.reset()
                    old.release()
                }
            }
            mediaPlayer = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.let { old ->
                runCatching {
                    old.setOnPreparedListener(null)
                    old.setOnErrorListener(null)
                    old.setSurface(null)
                    old.reset()
                    old.release()
                }
            }
            mediaPlayer = null
            activeSurface?.let { surf ->
                runCatching { surf.release() }
            }
            activeSurface = null
        }
    }

    LaunchedEffect(activeSurface, mediaPlayer) {
        val player = mediaPlayer ?: return@LaunchedEffect
        val surf = activeSurface
        runCatching {
            if (surf != null && surf.isValid) {
                player.setSurface(surf)
            }
        }
    }

    LaunchedEffect(isMuted, speed, isPlaying, isVideoReady) {
        val player = mediaPlayer ?: return@LaunchedEffect
        if (!isVideoReady) return@LaunchedEffect
        runCatching {
            val vol = if (isMuted) 0f else 1f
            player.setVolume(vol, vol)
            if (speed != 1.0f) {
                player.playbackParams = PlaybackParams().setSpeed(speed.coerceIn(0.25f, 3.0f))
            }
            if (isPlaying) {
                if (!player.isPlaying) player.start()
            } else {
                if (player.isPlaying) player.pause()
            }
        }
    }

    // Vinyl rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val vinylRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "vinylRotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        heartBursts = heartBursts + HeartBurst(offset = offset)
                        if (!videoItem.isLiked) {
                            onLikeToggle()
                        }
                    },
                    onTap = {
                        isPlaying = !isPlaying
                        showPauseIndicator = true
                    }
                )
            }
    ) {
        // Poster / Thumbnail during initial buffering or if player is waiting
        val posterUrl = videoItem.customThumbnailUrl.ifBlank { videoItem.userAvatarUrl }
        if ((!isVideoReady || hasPlaybackError) && posterUrl.isNotBlank()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = videoItem.title.ifBlank { videoItem.caption },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // TikTok-style Buffering Indicator
        if (!isVideoReady && !hasPlaybackError) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White.copy(alpha = 0.7f),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading feed...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Video Texture Surface View
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    isOpaque = false
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            activeSurface?.let { runCatching { it.release() } }
                            val newSurf = Surface(surface)
                            activeSurface = newSurf
                            applyCenterCropTransform(this@apply, videoWidth, videoHeight)
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                            applyCenterCropTransform(this@apply, videoWidth, videoHeight)
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            runCatching { mediaPlayer?.setSurface(null) }
                            activeSurface?.let { runCatching { it.release() } }
                            activeSurface = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                    if (isAvailable && surfaceTexture != null && activeSurface == null) {
                        activeSurface = Surface(surfaceTexture)
                    }
                    textureViewRef = this
                }
            },
            update = { tv ->
                textureViewRef = tv
                if (tv.isAvailable && tv.surfaceTexture != null && (activeSurface == null || activeSurface?.isValid == false)) {
                    activeSurface = Surface(tv.surfaceTexture)
                }
                if (videoWidth > 0 && videoHeight > 0) {
                    applyCenterCropTransform(tv, videoWidth, videoHeight)
                }
            },
            onReset = { tv ->
                if (textureViewRef === tv) textureViewRef = null
            },
            onRelease = { tv ->
                if (textureViewRef === tv) textureViewRef = null
                runCatching { mediaPlayer?.setSurface(null) }
                activeSurface?.let { runCatching { it.release() } }
                activeSurface = null
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (isVideoReady && !hasPlaybackError) 1f else 0f }
        )

        // Error / Retry Banner if persistent playback error occurred
        if (hasPlaybackError) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TashanCyan.copy(alpha = 0.8f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable {
                        hasPlaybackError = false
                        isVideoReady = false
                        activeStreamUrl = VideoStreamCacheManager.VERIFIED_STREAM_URLS.first()
                        reloadTrigger++
                    }
                    .padding(16.dp)
                    .testTag("video_player_retry_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Retry Video", tint = TashanCyan)
                    Text("Tap to reload video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Gradient Scrim Overlay for Readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f)
                        )
                    )
                )
        )

        // OFFICIAL APPLICATION WATERMARK WITH CREATOR NAME ON TOP
        Surface(
            color = Color.Black.copy(alpha = 0.35f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 60.dp)
                .testTag("video_watermark_overlay_${videoItem.id}")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // 1. Creator Name ON TOP
                Text(
                    text = "@${videoItem.username}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.4.sp
                )
                
                // 2. Official Application Watermark
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(TashanCyan, TashanPink)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "T",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "Tashan",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Pause/Play Indicator Toast in Center
        LaunchedEffect(showPauseIndicator) {
            if (showPauseIndicator) {
                delay(600)
                showPauseIndicator = false
            }
        }
        if (showPauseIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Floating Double-Tap Hearts
        heartBursts.forEach { heart ->
            key(heart.id) {
                FloatingHeartBurst(
                    offset = heart.offset,
                    rotation = heart.rotation,
                    onAnimationEnd = {
                        heartBursts = heartBursts.filter { it.id != heart.id }
                    }
                )
            }
        }

        // Poll Sticker (If video has an active poll)
        videoItem.pollQuestion?.let { question ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, TashanCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📊 $question",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val totalVotes = (videoItem.pollVotes.values.sum()).coerceAtLeast(1)
                    videoItem.pollOptions.forEachIndexed { idx, option ->
                        val votes = videoItem.pollVotes[idx] ?: 0
                        val percent = (votes * 100) / totalVotes
                        val isVoted = videoItem.userVotedOption == idx
                        Button(
                            onClick = { onVotePoll(idx) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVoted) TashanTeal else Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(option, color = Color.White, fontSize = 13.sp)
                                if (videoItem.userVotedOption != null) {
                                    Text("$percent%", color = TashanCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Left Bottom Caption & Metadata Block (Locked at bottom like TikTok across all devices)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, end = 76.dp, bottom = 12.dp)
        ) {
            // Username + Verified + Follow button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "@${videoItem.username}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(TashanCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                FollowButton(
                    isFollowing = videoItem.isFollowing,
                    onFollowToggle = onFollowToggle,
                    modifier = Modifier.testTag("feed_follow_button_${videoItem.uid}")
                )
            }

            Spacer(modifier = Modifier.height(5.dp))
            if (videoItem.title.isNotBlank()) {
                Text(
                    text = videoItem.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
            }

            // Expandable Caption
            Text(
                text = videoItem.caption,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                maxLines = if (isCaptionExpanded) 8 else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { isCaptionExpanded = !isCaptionExpanded }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Hashtags Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                videoItem.hashtags.take(4).forEach { tag ->
                    Text(
                        text = "#$tag",
                        color = TashanCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Music track title pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = videoItem.musicTitle,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Right Side Action Rail — Permanently Locked at Bottom-Right (TikTok Standard)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(60.dp)
                .padding(end = 8.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Creator Avatar with + Follow badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onFollowToggle() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(TashanTeal, TashanCyan)))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = videoItem.username.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
                if (!videoItem.isFollowing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 4.dp)
                            .size(19.dp)
                            .clip(CircleShape)
                            .background(NotificationGreen)
                            .border(1.5.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 2. Like Action Button
            val likeScale = remember { Animatable(1f) }
            LaunchedEffect(videoItem.isLiked) {
                if (videoItem.isLiked) {
                    likeScale.animateTo(1.35f, tween(100, easing = FastOutSlowInEasing))
                    likeScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                }
            }
            SideActionButton(
                icon = Icons.Filled.Favorite,
                countText = formatCount(videoItem.likesCount),
                tint = if (videoItem.isLiked) TashanPink else Color.White,
                testTag = "feed_like_button_${videoItem.id}",
                scale = likeScale.value,
                onClick = onLikeToggle
            )

            // 3. Comment Action Button
            SideActionButton(
                icon = Icons.Filled.ModeComment,
                countText = formatCount(videoItem.commentsCount),
                tint = Color.White,
                onClick = onCommentClick
            )

            // 4. Bookmark / Save Button
            SideActionButton(
                icon = Icons.Filled.Bookmark,
                countText = if (videoItem.isSaved) "Saved" else "Save",
                tint = if (videoItem.isSaved) WarningYellow else Color.White,
                testTag = "save_video_button_${videoItem.id}",
                onClick = onSaveToggle
            )

            // 5. Share Button
            SideActionButton(
                icon = Icons.Filled.Share,
                countText = formatCount(videoItem.sharesCount),
                tint = Color.White,
                onClick = onShareClick
            )

            // 6. More Options (...) Button
            var showMoreSheet by remember { mutableStateOf(false) }
            SideActionButton(
                icon = Icons.Filled.MoreHoriz,
                countText = "More",
                tint = Color.White,
                testTag = "more_video_options_button_${videoItem.id}",
                onClick = { showMoreSheet = true }
            )

            if (showMoreSheet) {
                VideoMoreOptionsModalSheet(
                    speed = speed,
                    onSpeedChange = { speed = it },
                    isMuted = isMuted,
                    onMuteToggle = { isMuted = !isMuted },
                    selectedQualityKey = selectedQualityKey,
                    onOpenQualitySelect = {
                        showMoreSheet = false
                        showQualitySheet = true
                    },
                    onReportClick = {
                        showMoreSheet = false
                        onReportClick()
                    },
                    onDismiss = { showMoreSheet = false }
                )
            }

            // 7. Spinning Vinyl Disc (Locked at Bottom-Right Corner)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .graphicsLayer { rotationZ = vinylRotation }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF2E2E3A), Color(0xFF121218), Color.Black)
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(TashanTeal, NotificationGreen)))
                )
            }
        }

        // Bottom-most Locked 2.dp TikTok Playback Progress Line
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isPlaying) 0.65f else 0.35f)
                    .fillMaxHeight()
                    .background(NotificationGreen)
            )
        }

        // Quality Switch Notification Toast
        LaunchedEffect(qualityToastMessage) {
            if (qualityToastMessage != null) {
                delay(2400)
                qualityToastMessage = null
            }
        }
        AnimatedVisibility(
            visible = qualityToastMessage != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF161626).copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NotificationGreen.copy(alpha = 0.8f)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = NotificationGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = qualityToastMessage ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Quality Selection Bottom Sheet
        if (showQualitySheet) {
            VideoQualityBottomSheet(
                videoItem = videoItem,
                currentQualityKey = selectedQualityKey,
                onSelectQuality = { res ->
                    selectedQualityKey = res.qualityKey
                    activeStreamUrl = VideoStreamCacheManager.sanitizeVideoUrl(res.videoUrl)
                    reloadTrigger++
                    qualityToastMessage = "Switched to ${res.label} (${res.bitrate})"
                },
                onDismiss = { showQualitySheet = false }
            )
        }
    }
}

@Composable
fun SideActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconText: String? = null,
    countText: String,
    tint: Color = Color.White,
    testTag: String? = null,
    scale: Float = 1.0f,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(52.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = countText,
                    tint = tint,
                    modifier = Modifier.size(26.dp)
                )
            } else if (iconText != null) {
                Text(
                    text = iconText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = countText,
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun FloatingHeartBurst(
    offset: Offset,
    rotation: Float = 0f,
    onAnimationEnd: () -> Unit
) {
    val density = LocalDensity.current
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val floatUp = remember { Animatable(0f) }
    val particleRadius = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            particleRadius.animateTo(
                targetValue = 68f,
                animationSpec = tween(420, easing = FastOutSlowInEasing)
            )
        }
        scale.animateTo(
            targetValue = 1.45f,
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(120, easing = LinearOutSlowInEasing)
        )
        launch {
            floatUp.animateTo(
                targetValue = -95f,
                animationSpec = tween(450, easing = FastOutLinearInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(450, easing = FastOutLinearInEasing)
            )
        }
        delay(460)
        onAnimationEnd()
    }

    val xPosDp = with(density) { (offset.x).toDp() - 45.dp }
    val yPosDp = with(density) { (offset.y).toDp() - 45.dp + floatUp.value.dp }

    Box(
        modifier = Modifier
            .offset(x = xPosDp, y = yPosDp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = rotation
                this.alpha = alpha.value
            }
            .testTag("floating_heart_burst"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TashanPink.copy(alpha = 0.55f * alpha.value),
                            Color(0xFFFF2E63).copy(alpha = 0.25f * alpha.value),
                            Color.Transparent
                        )
                    )
                )
        )

        val angles = listOf(30.0, 90.0, 150.0, 210.0, 270.0, 330.0)
        angles.forEachIndexed { i, deg ->
            val rad = Math.toRadians(deg)
            val pDist = particleRadius.value
            val pXDp = (pDist * Math.cos(rad)).toFloat().dp
            val pYDp = (pDist * Math.sin(rad)).toFloat().dp
            val particleColor = if (i % 2 == 0) TashanPink else Color(0xFFFF9EAA)
            Box(
                modifier = Modifier
                    .offset(x = pXDp, y = pYDp)
                    .size(if (i % 2 == 0) 10.dp else 6.dp)
                    .clip(CircleShape)
                    .background(particleColor.copy(alpha = alpha.value * 0.9f))
            )
        }

        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Heart Like Animation",
            tint = Color.Black.copy(alpha = 0.45f * alpha.value),
            modifier = Modifier
                .size(80.dp)
                .offset(y = 2.dp)
        )

        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Heart Like Animation",
            tint = TashanPink.copy(alpha = alpha.value),
            modifier = Modifier.size(76.dp)
        )

        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f * alpha.value),
            modifier = Modifier
                .size(18.dp)
                .offset(x = (-14).dp, y = (-14).dp)
        )
    }
}

@Composable
fun FollowButton(
    isFollowing: Boolean,
    onFollowToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBgColor by animateColorAsState(
        targetValue = if (isFollowing) Color(0xFF1E1E2E).copy(alpha = 0.85f) else TashanCyan,
        animationSpec = tween(250),
        label = "followBtnBg"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isFollowing) TashanCyan else Color.Black,
        animationSpec = tween(250),
        label = "followBtnText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(animatedBgColor)
            .border(
                width = 1.dp,
                color = if (isFollowing) TashanCyan.copy(alpha = 0.7f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onFollowToggle)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (isFollowing) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = TashanCyan,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = if (isFollowing) "Following" else "Follow",
                color = animatedTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoMoreOptionsModalSheet(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    selectedQualityKey: String,
    onOpenQualitySelect: () -> Unit,
    onReportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141422),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Video Settings & Options",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Speed Control Row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Playback Speed", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(1.0f, 1.5f, 2.0f).forEach { s ->
                        Surface(
                            onClick = { onSpeedChange(s) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (speed == s) TashanCyan else Color(0xFF222238),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${s}x",
                                    color = if (speed == s) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Mute / Audio Option Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onMuteToggle() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = TashanCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text("Audio Sound", color = Color.White, fontSize = 14.sp)
                }
                Text(
                    text = if (isMuted) "Muted" else "Playing",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Video Quality Option Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenQualitySelect() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HighQuality,
                        contentDescription = null,
                        tint = TashanCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text("Resolution Quality", color = Color.White, fontSize = 14.sp)
                }
                Text(
                    text = when (selectedQualityKey) {
                        "1080p" -> "1080p HD"
                        "720p" -> "720p HD"
                        "480p" -> "480p SD"
                        else -> "Auto Stream"
                    },
                    color = TashanCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Report Option Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onReportClick() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = TashanPink,
                    modifier = Modifier.size(22.dp)
                )
                Text("Report Video", color = TashanPink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
