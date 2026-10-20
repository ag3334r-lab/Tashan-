package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.VideoRepository

import com.example.model.VideoFilter
import com.example.model.VideoItem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val tiktokStockSounds = listOf(
    Pair("🎵 Tashan Viral Beat 2024", "Original Sound - DJ Tashan"),
    Pair("🔥 Trending Bass Drop", "Trending - Bass Official"),
    Pair("✨ Aesthetic Lofi Chill", "Slowed + Reverb Vibe"),
    Pair("⚡ Speed Up Club Anthem", "Dance & Remix Edits")
)

val tiktokFilters = listOf(
    Pair("Original", "✨"),
    Pair("Portrait", "🌸"),
    Pair("Cyberpunk", "⚡"),
    Pair("Golden Hour", "🌅"),
    Pair("Vintage 90s", "📼"),
    Pair("Noir Mono", "🖤"),
    Pair("Neon Pop", "💖")
)

val tiktokSpeedOptions = listOf(0.3f, 0.5f, 1.0f, 2.0f, 3.0f)
val tiktokDurationOptions = listOf("15s", "60s", "10m")
val availableTikTokCategories = listOf("Trending", "Music", "Comedy", "Dance", "Gaming", "Food", "Fashion", "Tech")

enum class CameraMode {
    VIDEO,
    LIVE
}

@Composable
fun CameraUploadScreen(
    repository: VideoRepository,
    onUploadComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableIntStateOf(1) }

    // Camera Recording State
    var isFrontCamera by remember { mutableStateOf(true) }
    var isFlashOn by remember { mutableStateOf(false) }
    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }
    var selectedDurationMode by remember { mutableStateOf("60s") }
    var maxSeconds by remember { mutableIntStateOf(60) }
    var selectedSound by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf("Original") }
    val dynamicFilters by repository.availableFilters.collectAsState()
    val activeFilters = remember(dynamicFilters) { dynamicFilters.filter { it.isActive } }
    var isCountingDown by remember { mutableStateOf(false) }
    var timerCountdown by remember { mutableIntStateOf(0) }
    var cameraMode by remember { mutableStateOf(CameraMode.VIDEO) }
    var isLiveStarted by remember { mutableStateOf(false) }
    var liveViewerCount by remember { mutableIntStateOf(0) }
    var beautyLevelOn by remember { mutableStateOf(true) }
    var voiceEffectSelected by remember { mutableStateOf("None") }
    var faceLiftLevel by remember { mutableFloatStateOf(0.5f) }

    // Selected / Recorded Video Metadata
    var selectedVideoUri by remember { mutableStateOf<String?>(null) }
    var selectedVideoTitle by remember { mutableStateOf("Tashan Video") }

    // Editor State & CapCut Multi-Track Project
    var videoTextOverlay by remember { mutableStateOf("") }
    var videoSticker by remember { mutableStateOf<String?>(null) }
    var isAudioMuted by remember { mutableStateOf(false) }
    var capCutProject by remember { mutableStateOf(com.example.model.CapCutEditProject()) }


    val snapSettings by repository.snapchatSettings.collectAsState()
    val stickersCatalog by repository.stickers.collectAsState()

    // Post Screen State
    var postCaption by remember { mutableStateOf("") }
    var postHashtags by remember { mutableStateOf(listOf("fyp", "viral", "tashan", "trending")) }
    var postCategory by remember { mutableStateOf(availableTikTokCategories[0]) }
    var allowComments by remember { mutableStateOf(true) }
    var allowDuet by remember { mutableStateOf(true) }
    var allowStitch by remember { mutableStateOf(true) }
    var postPrivacy by remember { mutableStateOf("Public") }
    var selectedStorageProvider by remember { mutableStateOf("Cloudinary") }
    var selectedCustomThumbnailUri by remember { mutableStateOf<String?>(null) }
    var showPhoneVerificationDialog by remember { mutableStateOf(false) }
    var inputPhoneNumber by remember { mutableStateOf("") }
    var inputOtpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var isVerifyingPhone by remember { mutableStateOf(false) }

    val currentUserProfile by repository.currentUser.collectAsState()

    val thumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (currentUserProfile.isPhoneVerified) {
                selectedCustomThumbnailUri = it.toString()
            } else {
                showPhoneVerificationDialog = true
            }
        }
    }

    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var uploadStatusMessage by remember { mutableStateOf("Uploading to Cloudinary...") }
    var uploadCompletedSuccessfully by remember { mutableStateOf(false) }

    var showSoundSelectorDialog by remember { mutableStateOf(false) }
    var showPixabayEffectsDialog by remember { mutableStateOf(false) }
    var activePixabayOverlay by remember { mutableStateOf<com.example.data.pixabay.PixabayVisualEffect?>(null) }

    LaunchedEffect(Unit) {
        com.example.data.DynamicFilterManager.getInstance().syncLivePixabayEffects("neon particles")
    }


    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedVideoUri = it.toString()
            selectedVideoTitle = "Gallery Video"
            currentScreen = 2
        }
    }

    LaunchedEffect(selectedDurationMode) {
        maxSeconds = when (selectedDurationMode) {
            "15s" -> 15
            "60s" -> 60
            else -> 180
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingSeconds++
                if (recordingSeconds >= maxSeconds) {
                    isRecording = false
                    selectedVideoUri = "https://vjs.zencdn.net/v/oceans.mp4"
                    selectedVideoTitle = "Live TikTok Clip (${recordingSeconds}s)"
                    currentScreen = 2
                }
            }
        }
    }

    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            while (timerCountdown > 0) {
                delay(1000)
                timerCountdown--
            }
            isCountingDown = false
            isRecording = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("tiktok_camera_upload_screen")
    ) {
        when (currentScreen) {
            1 -> {
                TikTokCameraView(
                    isFrontCamera = isFrontCamera,
                    isFlashOn = isFlashOn,
                    selectedFilter = selectedFilter,
                    availableFilters = activeFilters,
                    selectedSpeed = selectedSpeed,
                    selectedDurationMode = selectedDurationMode,
                    selectedSound = selectedSound,
                    isRecording = isRecording,
                    recordingSeconds = recordingSeconds,
                    maxSeconds = maxSeconds,
                    beautyLevelOn = beautyLevelOn,
                    isCountingDown = isCountingDown,
                    timerCountdown = timerCountdown,
                    cameraGridEnabled = snapSettings.cameraGridEnabled,
                    levelHorizonEnabled = snapSettings.levelHorizonEnabled,
                    nightModeBoost = snapSettings.nightModeBoost,
                    defaultArLensCode = snapSettings.defaultArLens,
                    onToggleCameraGrid = {
                        repository.updateSnapchatSettings(
                            snapSettings.copy(cameraGridEnabled = !snapSettings.cameraGridEnabled)
                        )
                    },

                    onClose = onUploadComplete,
                    onFlipCamera = { isFrontCamera = !isFrontCamera },
                    onToggleFlash = { isFlashOn = !isFlashOn },
                    onSelectSpeed = {
                        selectedSpeed = it
                        capCutProject = capCutProject.copy(playbackSpeed = it)
                    },
                    onSelectFilter = { selectedFilter = it },
                    onToggleBeauty = { beautyLevelOn = !beautyLevelOn },
                    onStartTimer = {
                        timerCountdown = 3
                        isCountingDown = true
                    },
                    onOpenSoundSelector = { showSoundSelectorDialog = true },
                    onSelectDurationMode = { selectedDurationMode = it },
                    onToggleRecord = {
                        if (isRecording) {
                            isRecording = false
                            selectedVideoUri = "https://vjs.zencdn.net/v/oceans.mp4"
                            selectedVideoTitle = "Live Clip (${recordingSeconds}s)"
                            currentScreen = 2
                        } else {
                            if (!isCountingDown) {
                                isRecording = true
                            }
                        }
                    },
                    onOpenGallery = { videoPickerLauncher.launch("video/*") },
                    cameraMode = cameraMode,
                    onSelectCameraMode = { cameraMode = it },
                    isLiveStarted = isLiveStarted,
                    liveViewerCount = liveViewerCount,
                    voiceEffectSelected = voiceEffectSelected,
                    onSelectVoiceEffect = { voiceEffectSelected = it },
                    faceLiftLevel = faceLiftLevel,
                    onUpdateFaceLift = { faceLiftLevel = it },
                    onToggleLive = {
                        if (!currentUserProfile.isPhoneVerified) {
                            inputOtpCode = ""
                            otpSent = false
                            showPhoneVerificationDialog = true
                            return@TikTokCameraView
                        }
                        
                        isLiveStarted = !isLiveStarted
                        if (isLiveStarted) {
                            liveViewerCount = (100..5000).random()
                            coroutineScope.launch {
                                while(isLiveStarted) {
                                    delay(3000)
                                    liveViewerCount += (-15..20).random()
                                    if (liveViewerCount < 50) liveViewerCount = 50
                                }
                            }
                        } else {
                            liveViewerCount = 0
                        }
                    },
                    activePixabayOverlay = activePixabayOverlay,
                    onOpenPixabayEffects = { showPixabayEffectsDialog = true },
                    onClearPixabayOverlay = { activePixabayOverlay = null }
                )
            }
            2 -> {
                SimpleVideoPreview(
                    videoUri = selectedVideoUri ?: "",
                    selectedSound = selectedSound,
                    activePixabayOverlay = activePixabayOverlay,
                    selectedFilter = selectedFilter,
                    onOpenSoundSelector = { showSoundSelectorDialog = true },
                    onOpenPixabayEffects = { showPixabayEffectsDialog = true },
                    onClearPixabayOverlay = { activePixabayOverlay = null },
                    onBack = { currentScreen = 1 },
                    onNext = { currentScreen = 3 }
                )
            }
            3 -> {
                TikTokPostView(
                    caption = postCaption,
                    onCaptionChange = { postCaption = it },
                    hashtags = postHashtags,
                    onAddHashtag = { tag -> if (!postHashtags.contains(tag)) postHashtags = postHashtags + tag },
                    onRemoveHashtag = { tag -> postHashtags = postHashtags.filter { it != tag } },
                    selectedCategory = postCategory,
                    onSelectCategory = { postCategory = it },
                    privacy = postPrivacy,
                    onPrivacyChange = { postPrivacy = it },
                    allowComments = allowComments,
                    onToggleAllowComments = { allowComments = it },
                    allowDuet = allowDuet,
                    onToggleAllowDuet = { allowDuet = it },
                    allowStitch = allowStitch,
                    onToggleAllowStitch = { allowStitch = it },
                    selectedStorageProvider = selectedStorageProvider,
                    onSelectStorageProvider = { selectedStorageProvider = it },
                    customThumbnailUri = selectedCustomThumbnailUri,
                    isPhoneVerified = currentUserProfile.isPhoneVerified,
                    onSelectCoverClick = {
                        if (currentUserProfile.isPhoneVerified) {
                            thumbnailPickerLauncher.launch("image/*")
                        } else {
                            showPhoneVerificationDialog = true
                        }
                    },
                    onBack = { currentScreen = 2 },
                    onSaveDraft = {
                        selectedVideoUri?.let { uri ->
                            repository.saveDraft(
                                localUri = uri,
                                caption = postCaption.ifBlank { "TikTok Draft #$postCategory" },
                                hashtags = postHashtags,
                                filterName = selectedFilter
                            )
                        }
                        onUploadComplete()
                    },
                    onPostNow = {
                        selectedVideoUri?.let { uri ->
                            isUploading = true
                            uploadProgress = 0f
                            coroutineScope.launch {
                                repository.uploadVideoToCloudStorage(
                                    videoUri = uri,
                                    title = postCaption.take(30).ifBlank { "TikTok Video" },
                                    caption = postCaption,
                                    category = postCategory,
                                    hashtags = postHashtags,
                                    pollQuestion = null,
                                    pollOptions = emptyList(),
                                    privacy = postPrivacy.lowercase(),
                                    storageProvider = selectedStorageProvider,
                                    filterName = selectedFilter,
                                    customThumbnailUrl = selectedCustomThumbnailUri,
                                    onProgress = { progress, status ->
                                        uploadProgress = progress
                                        uploadStatusMessage = status
                                    }
                                )
                                uploadCompletedSuccessfully = true
                            }
                        }
                    }
                )
            }
        }

        if (showSoundSelectorDialog) {
            TikTokSoundSelectorDialog(
                repository = repository,
                currentSound = selectedSound,
                onSelectSound = { sound ->
                    selectedSound = sound
                    showSoundSelectorDialog = false
                },
                onDismiss = { showSoundSelectorDialog = false }
            )
        }

        if (showPixabayEffectsDialog) {
            PixabayVisualEffectsDialog(
                selectedEffect = activePixabayOverlay,
                onSelectEffect = { effect ->
                    activePixabayOverlay = effect
                    if (effect != null) {
                        selectedFilter = effect.title
                    }
                    showPixabayEffectsDialog = false
                },
                onDismiss = { showPixabayEffectsDialog = false }
            )
        }

        if (isUploading) {
            Dialog(onDismissRequest = {}) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF161622),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, TashanCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("tiktok_upload_progress_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!uploadCompletedSuccessfully) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(TashanPink.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { uploadProgress },
                                    modifier = Modifier.size(60.dp),
                                    color = TashanPink,
                                    strokeWidth = 5.dp
                                )
                                Text(
                                    text = "${(uploadProgress * 100).toInt()}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "Posting to TikTok Feed",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = uploadStatusMessage,
                                color = TashanCyan,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            LinearProgressIndicator(
                                progress = { uploadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = TashanTeal,
                                trackColor = Color(0xFF242436)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Text(
                                text = "Posted Successfully!",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Your TikTok video is now live on the Tashan feed!",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    isUploading = false
                                    onUploadComplete()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TashanPink),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("tiktok_post_done_button")
                            ) {
                                Text("Watch on Feed 🚀", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        if (showPhoneVerificationDialog) {
            val ctx = LocalContext.current
            var cameraPhoneVerificationId by remember { mutableStateOf("") }
            var phoneOtpError by remember { mutableStateOf<String?>(null) }

            fun android.content.Context.findActivity(): android.app.Activity? {
                var c = this
                while (c is android.content.ContextWrapper) {
                    if (c is android.app.Activity) return c
                    c = c.baseContext
                }
                return null
            }

            AlertDialog(
                onDismissRequest = { showPhoneVerificationDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(TashanCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.VerifiedUser, contentDescription = null, tint = TashanCyan, modifier = Modifier.size(32.dp))
                    }
                },
                title = {
                    Text(
                        text = "Phone Number Verification",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Security Verification: Verify your mobile phone number via Firebase SMS OTP to unlock Live Streaming and Custom Thumbnails.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        OutlinedTextField(
                            value = inputPhoneNumber,
                            onValueChange = { inputPhoneNumber = it },
                            label = { Text("Mobile Number (e.g. +923001234567)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TashanCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedLabelColor = TashanCyan,
                                cursorColor = TashanCyan
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("phone_number_input")
                        )
                        if (phoneOtpError != null) {
                            Text(
                                text = phoneOtpError ?: "",
                                color = TashanPink,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (!otpSent) {
                            Button(
                                onClick = {
                                    val cleanPhone = inputPhoneNumber.trim()
                                    if (cleanPhone.length < 10) {
                                        phoneOtpError = "Please enter a valid phone number with country code (e.g. +92...)"
                                        return@Button
                                    }
                                    val act = ctx.findActivity()
                                    if (act == null) {
                                        phoneOtpError = "Activity context unavailable."
                                        return@Button
                                    }
                                    inputOtpCode = ""
                                    phoneOtpError = null
                                    isVerifyingPhone = true
                                    val formattedPhone = if (cleanPhone.startsWith("+")) cleanPhone else "+92${cleanPhone.removePrefix("0")}"
                                    repository.authManager.sendPhoneOtp(
                                        phoneNumber = formattedPhone,
                                        activity = act,
                                        onCodeSent = { vid ->
                                            isVerifyingPhone = false
                                            cameraPhoneVerificationId = vid
                                            inputOtpCode = ""
                                            otpSent = true
                                        },
                                        onVerificationCompleted = { credential ->
                                            coroutineScope.launch {
                                                repository.authManager.signInWithSocialCredential(credential)
                                                    .onSuccess { user ->
                                                        repository.finalizeSignIn(user, "phone")
                                                        isVerifyingPhone = false
                                                        showPhoneVerificationDialog = false
                                                    }
                                                    .onFailure { e ->
                                                        isVerifyingPhone = false
                                                        phoneOtpError = e.localizedMessage ?: "Verification failed"
                                                    }
                                            }
                                        },
                                        onVerificationFailed = { e ->
                                            isVerifyingPhone = false
                                            phoneOtpError = e.localizedMessage ?: "Failed to send SMS OTP"
                                        }
                                    )
                                },
                                enabled = !isVerifyingPhone,
                                colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("send_otp_button")
                            ) {
                                Text(
                                    text = if (isVerifyingPhone) "Requesting Firebase SMS..." else "Send SMS OTP Code 📩",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Surface(
                                color = TashanCyan.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TashanCyan.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Verification SMS sent to $inputPhoneNumber. Enter the 6-digit code received on your device.",
                                    color = TashanCyan,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            OutlinedTextField(
                                value = inputOtpCode,
                                onValueChange = { if (it.length <= 6 && it.all { ch -> ch.isDigit() }) inputOtpCode = it },
                                label = { Text("Enter 6-Digit OTP Code") },
                                singleLine = true,
                                textStyle = TextStyle(
                                    letterSpacing = 4.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TashanYellow,
                                    textAlign = TextAlign.Center
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TashanYellow,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedLabelColor = TashanYellow,
                                    cursorColor = TashanYellow
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("otp_code_input")
                            )
                        }
                    }
                },
                confirmButton = {
                    if (otpSent) {
                        Button(
                            onClick = {
                                if (inputOtpCode.trim().length == 6 && cameraPhoneVerificationId.isNotBlank()) {
                                    isVerifyingPhone = true
                                    phoneOtpError = null
                                    repository.verifyPhoneOtp(cameraPhoneVerificationId, inputOtpCode.trim()) { success, msg ->
                                        isVerifyingPhone = false
                                        if (success) {
                                            showPhoneVerificationDialog = false
                                            android.widget.Toast.makeText(ctx, "Mobile Verified via Firebase! 🚀", android.widget.Toast.LENGTH_LONG).show()
                                        } else {
                                            phoneOtpError = msg
                                        }
                                    }
                                } else {
                                    phoneOtpError = "Please enter the valid 6-digit SMS code."
                                }
                            },
                            enabled = !isVerifyingPhone,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("verify_otp_confirm_button")
                        ) {
                            Text("Verify & Continue", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPhoneVerificationDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = Color(0xFF1E2138)
            )
        }


    }
}

@Composable
fun TikTokCameraView(
    isFrontCamera: Boolean,
    isFlashOn: Boolean,
    selectedFilter: String,
    availableFilters: List<VideoFilter> = emptyList(),
    selectedSpeed: Float,
    selectedDurationMode: String,
    selectedSound: Pair<String, String>?,
    isRecording: Boolean,
    recordingSeconds: Int,
    maxSeconds: Int,
    beautyLevelOn: Boolean,
    isCountingDown: Boolean,
    timerCountdown: Int,
    cameraGridEnabled: Boolean = true,
    levelHorizonEnabled: Boolean = false,
    nightModeBoost: Boolean = false,
    defaultArLensCode: String = "NONE",
    onToggleCameraGrid: () -> Unit = {},
    onOpenSnapchatSettings: () -> Unit = {},
    onClose: () -> Unit,
    onFlipCamera: () -> Unit,
    onToggleFlash: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectFilter: (String) -> Unit,
    onToggleBeauty: () -> Unit,
    onStartTimer: () -> Unit,
    onOpenSoundSelector: () -> Unit,
    onSelectDurationMode: (String) -> Unit,
    onToggleRecord: () -> Unit,
    onOpenGallery: () -> Unit,
    cameraMode: CameraMode = CameraMode.VIDEO,
    onSelectCameraMode: (CameraMode) -> Unit = {},
    isLiveStarted: Boolean = false,
    liveViewerCount: Int = 0,
    voiceEffectSelected: String = "None",
    onSelectVoiceEffect: (String) -> Unit = {},
    faceLiftLevel: Float = 0.5f,
    onUpdateFaceLift: (Float) -> Unit = {},
    onToggleLive: () -> Unit = {},
    activePixabayOverlay: com.example.data.pixabay.PixabayVisualEffect? = null,
    onOpenPixabayEffects: () -> Unit = {},
    onClearPixabayOverlay: () -> Unit = {}
) {
    var showSpeedSelector by remember { mutableStateOf(false) }
    var showFiltersRow by remember { mutableStateOf(false) }
    var showVoiceEffectsRow by remember { mutableStateOf(false) }
    var showBeautyRow by remember { mutableStateOf(false) }
    var showInlinePixabayFxModal by remember { mutableStateOf(false) }

    val voiceEffects = listOf("None", "Chipmunk", "Deep", "Echo", "Robot", "Helium", "Giant", "Vibrato")
    val beautyOptions = listOf("None", "Smooth", "Brighten", "Whiten", "Slim", "Enlarge")

    val currentFilterObj = remember(availableFilters, selectedFilter) {
        availableFilters.find { it.name.equals(selectedFilter, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraRecordPreview(
            isFrontCamera = isFrontCamera
        )

        // Low-Light Night Mode Boost Overlay
        if (nightModeBoost) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x26FFF9C4))
            )
        }

        // Snapchat 3x3 Camera Composition Grid & Horizon Level Stabilizer
        if (cameraGridEnabled || levelHorizonEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (cameraGridEnabled) {
                    val gridColor = Color.White.copy(alpha = 0.28f)
                    drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.5f)
                    drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 1.5f)
                    drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.5f)
                    drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 1.5f)
                }
                if (levelHorizonEnabled) {
                    drawLine(
                        color = Color(0xFFFFFC00).copy(alpha = 0.75f),
                        start = Offset(w * 0.25f, h / 2f),
                        end = Offset(w * 0.75f, h / 2f),
                        strokeWidth = 3f
                    )
                }
            }
        }



        if (currentFilterObj != null && currentFilterObj.colorTintHex != "#00000000" && currentFilterObj.tintAlpha > 0f) {
            val tintColor = try { Color(android.graphics.Color.parseColor(currentFilterObj.colorTintHex)) } catch (e: Exception) { Color.Transparent }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tintColor.copy(alpha = currentFilterObj.tintAlpha))
            )
        }

        // Live Pixabay Motion Overlay & Visual Effect Layer
        if (activePixabayOverlay != null) {
            val fxTint = runCatching { Color(android.graphics.Color.parseColor(activePixabayOverlay.tintColorHex)) }.getOrDefault(TashanCyan)
            AsyncImage(
                model = activePixabayOverlay.thumbnailUrl,
                contentDescription = activePixabayOverlay.title,
                contentScale = ContentScale.Crop,
                alpha = activePixabayOverlay.blendAlpha,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fxTint.copy(alpha = 0.16f))
            )
            Surface(
                onClick = onClearPixabayOverlay,
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, fxTint),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 56.dp, start = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = fxTint, modifier = Modifier.size(13.dp))
                    Text(
                        text = "${activePixabayOverlay.title} • Tap to remove",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Filled.Close, contentDescription = "Remove FX", tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }

        // Live Viewer Count Overlay
        if (cameraMode == CameraMode.LIVE && isLiveStarted) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 60.dp, start = 16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(TashanPink))
                Spacer(modifier = Modifier.width(4.dp))
                Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "$liveViewerCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .testTag("tiktok_camera_close")
            ) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }

            Surface(
                onClick = onOpenSoundSelector,
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.testTag("tiktok_add_sound_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "Sound",
                        tint = TashanPink,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = selectedSound?.first ?: "Add Sound",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = onToggleFlash,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Flash",
                    tint = if (isFlashOn) TashanYellow else Color.White
                )
            }
        }

        // Right Sidebar
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TikTokSideButton(
                icon = Icons.Filled.Cameraswitch,
                label = "Flip",
                onClick = onFlipCamera,
                testTag = "tiktok_tool_flip"
            )
            TikTokSideButton(
                icon = Icons.Filled.Speed,
                label = "Speed",
                active = showSpeedSelector,
                onClick = { showSpeedSelector = !showSpeedSelector },
                testTag = "tiktok_tool_speed"
            )
            TikTokSideButton(
                icon = Icons.Filled.Face,
                label = "Beauty",
                active = showBeautyRow,
                onClick = { 
                    showBeautyRow = !showBeautyRow
                    if (showBeautyRow) {
                        showSpeedSelector = false
                        showFiltersRow = false
                        showVoiceEffectsRow = false
                    }
                },
                testTag = "tiktok_tool_beauty"
            )

            TikTokSideButton(
                icon = Icons.Filled.GridOn,
                label = "3x3 Grid",
                active = cameraGridEnabled,
                onClick = onToggleCameraGrid,
                testTag = "tiktok_tool_grid"
            )
            TikTokSideButton(
                icon = Icons.Filled.ColorLens,
                label = "Filters",
                active = showFiltersRow,
                onClick = { 
                    showFiltersRow = !showFiltersRow
                    if (showFiltersRow) {
                        showSpeedSelector = false
                        showBeautyRow = false
                        showVoiceEffectsRow = false
                    }
                },
                testTag = "tiktok_tool_filters"
            )
            TikTokSideButton(
                icon = Icons.Filled.Timer,
                label = "Timer",
                onClick = onStartTimer,
                testTag = "tiktok_tool_timer"
            )

            if (cameraMode == CameraMode.LIVE || true) {
                TikTokSideButton(
                    icon = Icons.Filled.AutoAwesome,
                    label = "Pixabay FX",
                    active = activePixabayOverlay != null || showInlinePixabayFxModal,
                    onClick = { onOpenPixabayEffects() },
                    testTag = "tiktok_tool_pixabay_fx"
                )
                TikTokSideButton(
                    icon = Icons.Filled.FaceRetouchingNatural,
                    label = "Face Lift",
                    active = true,
                    onClick = { onUpdateFaceLift(if (faceLiftLevel > 0.8f) 0.2f else faceLiftLevel + 0.2f) },
                    testTag = "tiktok_tool_facelift"
                )
                TikTokSideButton(
                    icon = Icons.Filled.GraphicEq,
                    label = "Voice",
                    active = showVoiceEffectsRow,
                    onClick = { 
                        showVoiceEffectsRow = !showVoiceEffectsRow
                        if (showVoiceEffectsRow) {
                            showSpeedSelector = false
                            showFiltersRow = false
                            showBeautyRow = false
                        }
                    },
                    testTag = "tiktok_tool_voice"
                )
            }

        }

        if (showInlinePixabayFxModal) {
            PixabayVisualEffectsDialog(
                selectedEffect = null,
                onSelectEffect = { effect ->
                    if (effect != null) {
                        onSelectFilter(effect.title)
                    }
                    showInlinePixabayFxModal = false
                },
                onDismiss = { showInlinePixabayFxModal = false }
            )
        }




        if (isCountingDown) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$timerCountdown",
                    color = TashanPink,
                    fontWeight = FontWeight.Black,
                    fontSize = 120.sp
                )
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = showSpeedSelector) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    tiktokSpeedOptions.forEach { speed ->
                        Text(
                            text = "${speed}x",
                            color = if (selectedSpeed == speed) TashanPink else Color.White,
                            fontWeight = if (selectedSpeed == speed) FontWeight.Black else FontWeight.Normal,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { onSelectSpeed(speed) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showFiltersRow) {
                LazyRow(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(availableFilters, key = { it.id }) { filterItem ->
                        val isSelected = selectedFilter.equals(filterItem.name, ignoreCase = true)
                        Surface(
                            onClick = { onSelectFilter(filterItem.name) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) TashanPink else Color.Black.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color.White else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(filterItem.emoji, fontSize = 12.sp)
                                Text(
                                    filterItem.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (filterItem.badge.isNotBlank()) {
                                    Surface(
                                        color = if (isSelected) Color.White else TashanPink,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = filterItem.badge,
                                            color = if (isSelected) TashanPink else Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showBeautyRow) {
                LazyRow(
                    modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(beautyOptions) { beauty ->
                        val isSelected = (beauty == "None" && !beautyLevelOn) || (beauty != "None" && beautyLevelOn)
                        Surface(
                            onClick = { onToggleBeauty() },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected && beauty != "None") TashanPink else Color.Black.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, if (isSelected && beauty != "None") Color.White else Color.Transparent)
                        ) {
                            Text(
                                text = beauty,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showVoiceEffectsRow) {
                LazyRow(
                    modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(voiceEffects) { effect ->
                        val isSelected = voiceEffectSelected == effect
                        Surface(
                            onClick = { onSelectVoiceEffect(effect) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) TashanPink else Color.Black.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, if (isSelected) Color.White else Color.Transparent)
                        ) {
                            Text(
                                text = effect,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    Surface(
                        onClick = { onOpenPixabayEffects() },
                        shape = RoundedCornerShape(14.dp),
                        color = TashanCyan.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TashanCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = TashanCyan, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Pixabay Effects & Overlays ✨",
                                color = TashanCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                item {
                    Surface(
                        onClick = { onOpenSoundSelector() },
                        shape = RoundedCornerShape(14.dp),
                        color = TashanPink.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TashanPink)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = TashanPink, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Pixabay Music & Sound FX 🎵",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                CameraMode.entries.forEach { mode ->
                    val isSelected = cameraMode == mode
                    Text(
                        text = mode.name,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { onSelectCameraMode(mode) }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onOpenPixabayEffects() }
                        .testTag("tiktok_effects_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = "Effects", tint = TashanCyan)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Effects", color = Color.White, fontSize = 11.sp)
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(4.dp)
                        .clickable {
                            if (cameraMode == CameraMode.VIDEO) {
                                onToggleRecord()
                            } else {
                                onToggleLive()
                            }
                        }
                        .testTag("tiktok_shutter_record_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraMode == CameraMode.VIDEO) {
                        Box(
                            modifier = Modifier
                                .size(if (isRecording) 34.dp else 64.dp)
                                .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                                .background(TashanPink)
                        )
                    } else {
                        Text(
                            text = if (isLiveStarted) "END" else "LIVE",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onOpenGallery() }
                        .testTag("tiktok_gallery_upload_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = "Upload", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Upload", color = Color.White, fontSize = 11.sp)
                }
            }

            if (isRecording) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = String.format("REC 00:%02d / 00:%02d", recordingSeconds, maxSeconds),
                    color = TashanPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TikTokSideButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (active) TashanPink else Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TikTokEditorView(
    videoTitle: String,
    selectedSound: Pair<String, String>?,
    selectedFilter: String,
    availableFilters: List<VideoFilter> = emptyList(),
    onSelectFilter: (String) -> Unit,
    textOverlay: String,
    onTextOverlayChange: (String) -> Unit,
    sticker: String?,
    onSelectSticker: (String?) -> Unit,
    isAudioMuted: Boolean,
    onToggleAudioMute: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var showTextDialog by remember { mutableStateOf(false) }
    var tempTextInput by remember { mutableStateOf(textOverlay) }
    val stickersList = listOf("🔥", "❤️", "😂", "🎉", "👑", "🚀", "💯", "🇵🇰")

    val editorFilterObj = remember(availableFilters, selectedFilter) {
        availableFilters.find { it.name.equals(selectedFilter, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("tiktok_editor_view")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1E2C), Color(0xFF101018))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (editorFilterObj != null && editorFilterObj.colorTintHex != "#00000000" && editorFilterObj.tintAlpha > 0f) {
                val tintColor = try { Color(android.graphics.Color.parseColor(editorFilterObj.colorTintHex)) } catch (e: Exception) { Color.Transparent }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tintColor.copy(alpha = editorFilterObj.tintAlpha))
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.MovieCreation,
                    contentDescription = null,
                    tint = TashanTeal,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = videoTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Filter applied: $selectedFilter",
                    color = TashanCyan,
                    fontSize = 12.sp
                )
                if (selectedSound != null) {
                    Text(
                        text = "🎵 ${selectedSound.first}",
                        color = TashanPink,
                        fontSize = 12.sp
                    )
                }
            }

            if (textOverlay.isNotBlank()) {
                Surface(
                    color = TashanPink.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = textOverlay,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            if (sticker != null) {
                Text(
                    text = sticker,
                    fontSize = 48.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 100.dp, end = 40.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Preview & Edit",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(38.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp, top = 60.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TikTokSideButton(
                icon = Icons.Filled.TextFields,
                label = "Text",
                onClick = {
                    tempTextInput = textOverlay
                    showTextDialog = true
                },
                testTag = "tiktok_edit_text"
            )
            TikTokSideButton(
                icon = Icons.Filled.InsertEmoticon,
                label = "Stickers",
                onClick = {
                    val nextSticker = stickersList.random()
                    onSelectSticker(if (sticker == nextSticker) null else nextSticker)
                },
                testTag = "tiktok_edit_stickers"
            )
            TikTokSideButton(
                icon = if (isAudioMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                label = if (isAudioMuted) "Unmute" else "Mute",
                active = isAudioMuted,
                onClick = onToggleAudioMute,
                testTag = "tiktok_edit_audio"
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableFilters, key = { it.id }) { filterItem ->
                    val isSelected = selectedFilter.equals(filterItem.name, ignoreCase = true)
                    Surface(
                        onClick = { onSelectFilter(filterItem.name) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) TashanTeal else Color.Black.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color.White else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(filterItem.emoji, fontSize = 12.sp)
                            Text(
                                filterItem.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = NotificationGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("tiktok_editor_next_button")
            ) {
                Text("Next ➔", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }

    if (showTextDialog) {
        Dialog(onDismissRequest = { showTextDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E2C),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add Text on Video", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    OutlinedTextField(
                        value = tempTextInput,
                        onValueChange = { tempTextInput = it },
                        placeholder = { Text("e.g. Wait for it... 🔥", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NotificationGreen,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showTextDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onTextOverlayChange(tempTextInput)
                                showTextDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NotificationGreen)
                        ) {
                            Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TikTokPostView(
    caption: String,
    onCaptionChange: (String) -> Unit,
    hashtags: List<String>,
    onAddHashtag: (String) -> Unit,
    onRemoveHashtag: (String) -> Unit,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    privacy: String,
    onPrivacyChange: (String) -> Unit,
    allowComments: Boolean,
    onToggleAllowComments: (Boolean) -> Unit,
    allowDuet: Boolean,
    onToggleAllowDuet: (Boolean) -> Unit,
    allowStitch: Boolean,
    onToggleAllowStitch: (Boolean) -> Unit,
    selectedStorageProvider: String,
    onSelectStorageProvider: (String) -> Unit,
    customThumbnailUri: String?,
    isPhoneVerified: Boolean,
    onSelectCoverClick: () -> Unit,
    onBack: () -> Unit,
    onSaveDraft: () -> Unit,
    onPostNow: () -> Unit
) {
    val quickHashtags = listOf("fyp", "viral", "foryou", "trending", "dance", "comedy", "tashan")
    val friendsList = listOf("ayesha_vibes", "hamza_pk", "sarah_dance", "ali_tech")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F17))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .testTag("tiktok_post_view")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Post",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(36.dp))
        }

        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp)
                        .testTag("tiktok_caption_input"),
                    decorationBox = { innerTextField ->
                        if (caption.isEmpty()) {
                            Text(
                                text = "Describe your video, add #hashtags, or mention @friends...",
                                color = TextTertiary,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = { onAddHashtag(quickHashtags.random()) },
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "# Hashtags",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        onClick = {
                            val friend = friendsList.random()
                            onCaptionChange(if (caption.isBlank()) "@$friend " else "$caption @$friend ")
                        },
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "@ Mention",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF222232))
                    .border(
                        1.5.dp,
                        if (customThumbnailUri != null) TashanCyan else if (isPhoneVerified) TashanPink.copy(alpha = 0.6f) else Color(0xFFFF9800).copy(alpha = 0.6f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectCoverClick() }
                    .testTag("select_cover_button"),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (customThumbnailUri != null) {
                    AsyncImage(
                        model = customThumbnailUri,
                        contentDescription = "Custom Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPhoneVerified) Icons.Filled.AddPhotoAlternate else Icons.Filled.PhoneAndroid,
                            contentDescription = "Cover",
                            tint = if (isPhoneVerified) TashanPink else Color(0xFFFF9800),
                            modifier = Modifier.size(28.dp)
                        )
                        if (!isPhoneVerified) {
                            Text(
                                text = "Verify Phone",
                                color = Color(0xFFFF9800),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (customThumbnailUri != null) "✓ Cover Set" else "Select Cover",
                            color = Color.White,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(hashtags) { tag ->
                Surface(
                    onClick = { onRemoveHashtag(tag) },
                    color = TashanPink.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TashanPink.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("#$tag", color = TashanPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text("Category", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableTikTokCategories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        onClick = { onSelectCategory(cat) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) TashanTeal else Color.White.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = cat,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Text("Who can watch this video", color = Color.White, fontSize = 14.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Public", "Friends", "Only me").forEach { priv ->
                        val isSelected = privacy == priv
                        Text(
                            text = priv,
                            color = if (isSelected) TashanPink else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { onPrivacyChange(priv) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            TikTokSettingToggle(
                icon = Icons.Filled.ChatBubbleOutline,
                title = "Allow comments",
                checked = allowComments,
                onCheckedChange = onToggleAllowComments
            )

            TikTokSettingToggle(
                icon = Icons.Filled.CompareArrows,
                title = "Allow Duet",
                checked = allowDuet,
                onCheckedChange = onToggleAllowDuet
            )

            TikTokSettingToggle(
                icon = Icons.Filled.ContentCut,
                title = "Allow Stitch",
                checked = allowStitch,
                onCheckedChange = onToggleAllowStitch
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSaveDraft,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("tiktok_save_draft_button"),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(imageVector = Icons.Filled.Drafts, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Drafts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Button(
                onClick = onPostNow,
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("tiktok_post_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NotificationGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Filled.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Post", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun TikTokSettingToggle(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Text(title, color = Color.White, fontSize = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NotificationGreen,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color(0xFF222232)
            )
        )
    }
}

@Composable
fun TikTokSoundSelectorDialog(
    repository: VideoRepository? = null,
    currentSound: Pair<String, String>?,
    onSelectSound: (Pair<String, String>?) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(false) }
    var pixabayTracks by remember {
        mutableStateOf(com.example.data.pixabay.PixabayClient.getInstance().getCuratedPixabayAudioCatalog())
    }
    var previewingTrackId by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "All",
        "Trending Music",
        "Background Beats",
        "Sound Effects",
        "Voice & Clips"
    )

    fun refreshPixabayAudio() {
        coroutineScope.launch {
            isLoading = true
            val results = com.example.data.pixabay.PixabayClient.getInstance()
                .searchMusicAndSoundEffects(query = searchQuery, categoryFilter = selectedCategory)
            pixabayTracks = results
            isLoading = false
        }
    }

    LaunchedEffect(selectedCategory) {
        refreshPixabayAudio()
    }

    Dialog(
        onDismissRequest = {
            repository?.audioPlayerManager?.release()
            onDismiss()
        }
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF14141F),
            border = BorderStroke(1.dp, TashanCyan.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .padding(horizontal = 6.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎵 Pixabay Music & Sound FX Hub",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Live Trending Songs, Beats, SFX & Voice Clips",
                            color = TashanCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = {
                            repository?.audioPlayerManager?.release()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Live Search Bar for Pixabay Audio/Songs/SFX
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Pixabay songs, beats, SFX, voice...", color = TextTertiary, fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = TashanCyan, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        TextButton(onClick = { refreshPixabayAudio() }) {
                            Text("Search", color = TashanCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TashanCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = Color(0xFF1E1E2E),
                        unfocusedContainerColor = Color(0xFF1E1E2E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pixabay_audio_search_input")
                )

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val active = selectedCategory == cat
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(16.dp),
                            color = if (active) TashanPink else Color(0xFF222232),
                            border = BorderStroke(1.dp, if (active) Color.White else BorderSubtle)
                        ) {
                            Text(
                                text = cat,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (currentSound != null) {
                    Surface(
                        onClick = {
                            repository?.audioPlayerManager?.release()
                            onSelectSound(null)
                        },
                        color = TashanPink.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TashanPink.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected: ${currentSound.first} (Tap to remove)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = TashanPink, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TashanCyan, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Fetching tracks & sound effects from Pixabay...", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pixabayTracks.forEach { item ->
                            val soundPair = item.title to "${item.artist} • ${item.durationSec}s"
                            val isSelected = currentSound?.first == item.title
                            val isPreviewing = previewingTrackId == item.id

                            Surface(
                                onClick = {
                                    repository?.audioPlayerManager?.release()
                                    onSelectSound(soundPair)
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) TashanPink.copy(alpha = 0.22f) else Color(0xFF1E1E2E),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) TashanPink else if (isPreviewing) TashanCyan else BorderSubtle
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Play / Stop Audio Preview Button
                                    IconButton(
                                        onClick = {
                                            if (isPreviewing) {
                                                repository?.audioPlayerManager?.release()
                                                previewingTrackId = null
                                            } else {
                                                previewingTrackId = item.id
                                                repository?.audioPlayerManager?.playTrack(
                                                    com.example.data.AudioTrack(
                                                        id = item.id,
                                                        title = item.title,
                                                        artist = item.artist,
                                                        category = item.category,
                                                        audioUrl = item.streamUrl,
                                                        coverUrl = item.thumbnailUrl,
                                                        durationSec = item.durationSec
                                                    )
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isPreviewing) TashanCyan else TashanPink)
                                    ) {
                                        Icon(
                                            imageVector = if (isPreviewing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                            contentDescription = "Preview Audio",
                                            tint = if (isPreviewing) Color.Black else Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = "${item.artist} • ${item.category} • ${item.durationSec}s",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "🏷️ ${item.tags}",
                                            color = TashanCyan.copy(alpha = 0.85f),
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            repository?.audioPlayerManager?.release()
                                            onSelectSound(soundPair)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) SuccessGreen else TashanTeal
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = if (isSelected) "Used" else "Use",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PixabayVisualEffectsDialog(
    selectedEffect: com.example.data.pixabay.PixabayVisualEffect?,
    onSelectEffect: (com.example.data.pixabay.PixabayVisualEffect?) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(false) }
    var effectsList by remember {
        mutableStateOf(com.example.data.pixabay.PixabayClient.getInstance().getFallbackPixabayEffects())
    }

    val categories = listOf(
        "All",
        "Neon & Cyber",
        "Particles & Bokeh",
        "Glitch & VHS",
        "Fire & Energy"
    )

    fun loadPixabayEffects() {
        coroutineScope.launch {
            isLoading = true
            val live = com.example.data.pixabay.PixabayClient.getInstance()
                .searchVisualEffects(
                    query = searchQuery.ifBlank { "neon particles" },
                    categoryFilter = selectedCategory
                )
            effectsList = live
            com.example.data.DynamicFilterManager.getInstance()
                .syncLivePixabayEffects(searchQuery.ifBlank { "neon particles" })
            isLoading = false
        }
    }

    LaunchedEffect(selectedCategory) {
        loadPixabayEffects()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF14141F),
            border = BorderStroke(1.dp, TashanPink.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .padding(horizontal = 6.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "✨ Pixabay Live Video Effects & Overlays",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Real-time motion overlays, glitch, bokeh & neon filters",
                            color = TashanCyan,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Pixabay FX (neon, fire, glitch, bokeh)...", color = TextTertiary, fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = TashanPink, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        TextButton(onClick = { loadPixabayEffects() }) {
                            Text("Load FX", color = TashanCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TashanPink,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = Color(0xFF1E1E2E),
                        unfocusedContainerColor = Color(0xFF1E1E2E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val active = selectedCategory == cat
                        Surface(
                            onClick = { selectedCategory = cat },
                            shape = RoundedCornerShape(16.dp),
                            color = if (active) TashanCyan else Color(0xFF222232),
                            border = BorderStroke(1.dp, if (active) Color.White else BorderSubtle)
                        ) {
                            Text(
                                text = cat,
                                color = if (active) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TashanPink, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Syncing live visual effects from Pixabay API...", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        effectsList.forEach { fx ->
                            val isSelected = selectedEffect?.id == fx.id
                            val tintColor = runCatching { Color(android.graphics.Color.parseColor(fx.tintColorHex)) }
                                .getOrDefault(TashanCyan)

                            Surface(
                                onClick = { onSelectEffect(fx) },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E1E2E),
                                border = BorderStroke(1.5.dp, if (isSelected) TashanCyan else tintColor.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(tintColor.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = fx.thumbnailUrl,
                                            contentDescription = fx.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(tintColor.copy(alpha = 0.28f))
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = fx.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1
                                            )
                                            Surface(
                                                color = tintColor.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = fx.badge,
                                                    color = tintColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${fx.category} • By ${fx.creatorName}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "❤️ ${fx.likes} • ⬇️ ${fx.downloads} • ${fx.tags}",
                                            color = TextTertiary,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }

                                    Button(
                                        onClick = { onSelectEffect(fx) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TashanPink),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Apply FX", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraRecordPreview(
    isFrontCamera: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraError by remember { mutableStateOf(false) }
    val cameraExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            cameraError = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner, isFrontCamera) {
        onDispose {
            try {
                cameraProviderRef?.unbindAll()
                cameraExecutor.shutdown()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    }
                    try {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                cameraProviderRef = cameraProvider
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val preferredSelector = if (isFrontCamera) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                                val cameraSelector = if (cameraProvider.hasCamera(preferredSelector)) {
                                    preferredSelector
                                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                } else {
                                    null
                                }
                                if (cameraSelector != null) {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                                    cameraError = false
                                } else {
                                    cameraError = true
                                }
                            } catch (e: Exception) {
                                cameraError = true
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    } catch (e: Exception) {
                        cameraError = true
                    }
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (cameraError || !hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF101018)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = TashanPink,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "TikTok Camera Live Ready",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the red shutter button to start recording or select a video from device gallery.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleVideoPreview(
    videoUri: String,
    selectedSound: Pair<String, String>? = null,
    activePixabayOverlay: com.example.data.pixabay.PixabayVisualEffect? = null,
    selectedFilter: String = "Original",
    onOpenSoundSelector: () -> Unit = {},
    onOpenPixabayEffects: () -> Unit = {},
    onClearPixabayOverlay: () -> Unit = {},
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (activePixabayOverlay != null) {
            val fxColor = runCatching { Color(android.graphics.Color.parseColor(activePixabayOverlay.tintColorHex)) }.getOrDefault(TashanCyan)
            AsyncImage(
                model = activePixabayOverlay.thumbnailUrl,
                contentDescription = activePixabayOverlay.title,
                contentScale = ContentScale.Crop,
                alpha = 0.35f,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fxColor.copy(alpha = 0.18f))
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = null,
                tint = TashanCyan,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Video Studio Editor",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Filter: $selectedFilter • Sound: ${selectedSound?.first ?: "Original Audio"}",
                color = TashanCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (activePixabayOverlay != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    onClick = onClearPixabayOverlay,
                    color = TashanPink.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TashanPink)
                ) {
                    Text(
                        text = "✨ Active Pixabay FX: ${activePixabayOverlay.title} (Tap to clear)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenPixabayEffects,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TashanCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TashanCyan)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pixabay FX ✨", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onOpenSoundSelector,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TashanPink),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = TashanPink, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pixabay Music 🎵", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Next", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
