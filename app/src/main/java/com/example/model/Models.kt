package com.example.model

data class VideoResolution(
    val label: String,
    val qualityKey: String,
    val videoUrl: String,
    val bitrate: String = "",
    val dimensions: String = "",
    val isHd: Boolean = false,
    val dataSaving: String = ""
)

data class VideoItem(
    val id: String,
    val uid: String,
    val username: String,
    val userAvatarUrl: String = "",
    val videoUrl: String,
    val customThumbnailUrl: String = "",
    val resolutions: List<VideoResolution> = emptyList(),
    val title: String = "",
    val caption: String,
    val hashtags: List<String> = emptyList(),
    val musicTitle: String = "Original Sound - $username",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val viewsCount: Int = 0,
    val viewedUserUids: List<String> = emptyList(),
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isFollowing: Boolean = false,
    val pollQuestion: String? = null,
    val pollOptions: List<String> = emptyList(),
    val pollVotes: Map<Int, Int> = emptyMap(), // Option index -> vote count
    val userVotedOption: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val privacy: String = "public", // public, friends, private
    val sensitiveContent: Boolean = false,
    val speed: Float = 1.0f,
    val filterName: String = "Normal",
    val category: String = "Trending",
    val storageProvider: String = "Cloudinary" // Cloudinary, Firebase Storage, Multi-Cloud
) {
    fun getResolutionVariants(): List<VideoResolution> {
        if (resolutions.isNotEmpty()) return resolutions
        return listOf(
            VideoResolution(
                label = "Auto (Adaptive)",
                qualityKey = "auto",
                videoUrl = videoUrl,
                bitrate = "Adaptive Bitrate",
                dimensions = "Dynamic Network Auto",
                isHd = true,
                dataSaving = "Balanced for Battery & Speed"
            ),
            VideoResolution(
                label = "1080p Full HD",
                qualityKey = "1080p",
                videoUrl = videoUrl,
                bitrate = "4.5 Mbps (60fps)",
                dimensions = "1080 x 1920",
                isHd = true,
                dataSaving = "Highest Clarity (Wi-Fi Recommended)"
            ),
            VideoResolution(
                label = "720p HD",
                qualityKey = "720p",
                videoUrl = videoUrl,
                bitrate = "2.2 Mbps",
                dimensions = "720 x 1280",
                isHd = true,
                dataSaving = "Smooth Streaming"
            ),
            VideoResolution(
                label = "480p SD",
                qualityKey = "480p",
                videoUrl = videoUrl,
                bitrate = "850 Kbps",
                dimensions = "480 x 854",
                isHd = false,
                dataSaving = "Data Saver Mode"
            )
        )
    }
}

data class Comment(
    val id: String,
    val videoId: String,
    val uid: String,
    val username: String,
    val userAvatarUrl: String = "",
    val text: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val birthDate: String = "",
    val country: String = "",
    val isPhoneVerified: Boolean = false,
    val isEmailVerified: Boolean = false,
    val authProvider: String = "guest",
    val postsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val totalLikes: Int = 0,
    val totalViews: Int = 0,
    val isFollowing: Boolean = false,
    val isPrivate: Boolean = false,
    val isBanned: Boolean = false,
    val banReason: String = "",
    val banTicketId: String = "",
    val coins: Int = 0,
    val balancePkr: Double = 0.0,
    val themeAccentHex: String = "#FE2C55",
    val level: Int = 1,
    val walletPinHash: String = "",
    val walletLedgerSignature: String = "",
    val achievements: List<String> = listOf("Tashan Pioneer", "Rising Creator")
) {
    val isLoggedIn: Boolean
        get() = uid.isNotBlank() && uid != "guest"

    fun starRatingBadgeLabel(): String = starTierLabel()

    /**
     * Progressive 1-to-5 Star Creator ID badge rating:
     * 1★ = Verified Starter ID
     * 2★ = Rising Star (1,000+ followers or 50K+ views)
     * 3★ = Silver Star ID (5,000+ followers)
     * 4★ = Pro Star ID (7,500+ followers & 250,000+ views)
     * 5★ = Gold Monetized Star ID (10,000+ followers & 500,000+ views, or 5K+ followers & 500K+ views)
     */
    fun computedStarRating(): Int {
        return when {
            followersCount >= 10_000 && totalViews >= 500_000 -> 5
            followersCount >= 5_000 && totalViews >= 500_000 -> 5
            followersCount >= 7_500 && totalViews >= 250_000 -> 4
            followersCount >= 5_000 -> 3
            followersCount >= 1_000 || totalViews >= 50_000 -> 2
            else -> 1
        }
    }

    fun starStarsVisual(): String {
        val count = computedStarRating()
        return "⭐".repeat(count)
    }

    fun starTierLabel(): String {
        return when (computedStarRating()) {
            5 -> "5★ Gold Monetized ID"
            4 -> "4★ Diamond Creator ID"
            3 -> "3★ Silver Star ID (5K+)"
            2 -> "2★ Rising Star ID"
            else -> "1★ Tashan Star ID"
        }
    }

    /**
     * Original Monetization Gate:
     * Requires at least 5,000 followers (with 10,000 for Gold Tier) AND 500,000 (500K) video views.
     */
    fun isMonetizationUnlocked(): Boolean {
        return followersCount >= 5_000 && totalViews >= 500_000
    }

    fun isGoldMonetizationTier(): Boolean {
        return followersCount >= 10_000 && totalViews >= 500_000
    }

    fun isVerifiedForChat(): Boolean {
        if (uid.isBlank() || uid == "guest") return false
        val provider = authProvider.lowercase()
        return isEmailVerified || isPhoneVerified ||
            provider in listOf("google", "apple", "facebook", "phone", "email_otp")
    }
}

data class SnackVideoSettings(
    val pakistanSmartCdnEnabled: Boolean = true,
    val activePakistanRoute: String = "Lahore-Karachi Edge CDN (Anti-Block)",
    val dataSaverMode: Boolean = false,
    val easyUiModeForAllAges: Boolean = true,
    val voiceGuidedHints: Boolean = true,
    val autoPlayNextVideo: Boolean = true,
    val watchTimeAiRankingEnabled: Boolean = true,
    val strictMutualConsentChat: Boolean = true,
    val allowCallsFromVerifiedOnly: Boolean = true,
    val walletAntiHackLockEnabled: Boolean = true,
    val walletPinConfigured: Boolean = true,
    val cacheSizeMb: Int = 38,
    val maxBitrateMbps: Int = 12,
    val hardwareAcceleration60Fps: Boolean = true,
    val preferredVideoCodec: String = "HEVC / H.265",
    val dnsOverHttpsEnabled: Boolean = true,
    val oauthStrictSessionBinding: Boolean = true,
    val biometricAppLock: Boolean = false,
    val creatorWatermarkStyle: String = "Cyber Neon Badge",
    val liveStreamLatencyMode: String = "Ultra-Low (WebRTC)",
    val pushLikesAndComments: Boolean = true,
    val pushLiveStreamAlerts: Boolean = true,
    val pushOAuthSecurityAlerts: Boolean = true
)

data class AdminSystemControl(
    val isGiftingAndPurchasesLocked: Boolean = true, // Default locked for trailer/preview mode
    val giftingLockNoticeMessage: String = "🔒 Payment Gateway Not Configured Yet - Coin Purchases & Gifting are locked in Trailer/Preview Mode",
    val monetizationPkrPer1000Views: Double = 10.0, // Default PKR 10 per 1,000 views (PKR 100 per 10,000 views)
    val isMonetizationSystemActive: Boolean = true,
    val globalMaintenanceMode: Boolean = false,
    val maintenanceReason: String = "System undergoing scheduled admin maintenance",
    val adminUidList: List<String> = listOf("admin", "master_admin", "com.tashon.app.admin"),
    val bannedUids: List<String> = emptyList(),
    val moderatedVideoIds: List<String> = emptyList(),
    val totalRevenueDistributedPkr: Double = 12500.0,
    val totalViewsLogged: Long = 1250000L
)

data class PaymentVerificationResult(
    val isVerified: Boolean,
    val status: String, // "APPROVED", "NEEDS_MANUAL_REVIEW", "REJECTED"
    val confidenceScore: Int, // 0 - 100%
    val trxId: String,
    val gateway: String,
    val amountPkr: Double,
    val aiExplanation: String,
    val fraudRiskScore: Int, // 0 (low) - 100 (high)
    val verifiedAt: Long = System.currentTimeMillis()
)

data class ReportAnalysisResult(
    val isClientVerified: Boolean,
    val clientTrustScore: Int, // 0 - 100%
    val targetType: String, // "VIDEO", "USER", "COMMENT"
    val targetId: String,
    val severity: String, // "CRITICAL", "HIGH", "MEDIUM", "LOW", "FALSE_ALARM"
    val recommendedAction: String, // "REMOVE_CONTENT", "TEMPORARY_BAN", "SHADOWBAN", "DISMISS"
    val aiReasoning: String,
    val automatedResolution: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class BanAppealResult(
    val isRestored: Boolean,
    val uid: String,
    val appealTicketId: String,
    val status: String, // "UNBANNED", "UNDER_ADMIN_REVIEW", "APPEAL_DENIED"
    val trustScore: Int,
    val aiVerdictNotice: String,
    val recoveryStep: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AiDiagnosticResult(
    val isHealthy: Boolean,
    val issueDetected: String,
    val rootCause: String,
    val suggestedFix: String,
    val autoHealActionTaken: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val conversationId: String = "",
    val senderUid: String = "",
    val senderUsername: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "TEXT", // TEXT, IMAGE, VIDEO, VOICE, AUDIO, DOCUMENT, LOCATION, GIFT, STICKER
    val giftEmoji: String = "",
    val giftCoins: Int = 0,
    val reactionEmoji: String = "",
    val isRead: Boolean = false,
    val deliveryStatus: String = "READ", // SENT, DELIVERED, READ
    val replyToMessageId: String = "",
    val replyToSenderName: String = "",
    val replyToText: String = "",
    val replyToSender: String = "",
    val replyToPreview: String = "",
    val isForwarded: Boolean = false,
    val isStarred: Boolean = false,
    val isDeleted: Boolean = false,
    val audioDurationSec: Int = 8,
    val audioWaveform: List<Float> = listOf(0.25f, 0.55f, 0.85f, 0.4f, 0.95f, 0.65f, 0.35f, 0.8f, 0.9f, 0.5f, 0.7f, 0.3f),
    val waveformAmplitudes: List<Float> = listOf(0.25f, 0.55f, 0.85f, 0.4f, 0.95f, 0.65f, 0.35f, 0.8f, 0.9f, 0.5f, 0.7f, 0.3f),
    val documentName: String = "",
    val documentSizeKb: Int = 0,
    val documentSize: String = "",
    val locationName: String = "",
    val reactions: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

data class Conversation(
    val id: String,
    val otherUserUid: String,
    val otherUsername: String,
    val otherDisplayName: String = "",
    val otherAvatarUrl: String = "",
    val lastMessage: String,
    val lastMessageType: String = "TEXT",
    val timestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val isSentByMe: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val lastSeenText: String = "Online",
    val phoneNumber: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, BLOCKED
    val initiatorUid: String = "",
    val recipientHasReplied: Boolean = false,
    val blockedByUids: List<String> = emptyList(),
    val streakCount: Int = 0,
    val lastStreakTimestamp: Long = 0L,
    val retentionPolicy: String = "24_HOURS" // IMMEDIATE (Delete after viewing) or 24_HOURS
) {
    val isBlocked: Boolean
        get() = status == "BLOCKED" || blockedByUids.isNotEmpty()

    /**
     * Enforces the user's strict Mutual Consent rule:
     * - No messages or calls if BLOCKED.
     * - If status == "ACCEPTED" or recipientHasReplied -> allowed.
     * - If status == "PENDING":
     *   - If no message has been sent yet (initiatorUid.isBlank()), the user can send 1 introductory message request.
     *   - If initiatorUid == currentUid, the sender CANNOT send more messages or call until the other person accepts or replies!
     *   - If initiatorUid != currentUid, the recipient CAN reply (which automatically accepts the conversation).
     */
    fun canSendDirectMessage(currentUid: String, isMutualFollow: Boolean = false): Boolean {
        if (isBlocked) return false
        if (status == "ACCEPTED" || recipientHasReplied || isMutualFollow) return true
        if (initiatorUid.isBlank()) return true
        return initiatorUid != currentUid
    }

    fun canPlaceCall(currentUid: String, isMutualFollow: Boolean = false): Boolean {
        if (isBlocked) return false
        return status == "ACCEPTED" || recipientHasReplied || isMutualFollow
    }
}

data class SnapchatUserSettings(
    val isGhostMode: Boolean = false,
    val ghostModeDuration: String = "24 Hours", // "3 Hours", "24 Hours", "Until Turned Off"
    val locationSharingMode: String = "My Friends", // "Ghost Mode", "My Friends", "My Friends Except...", "Only These Friends"
    val messageRetentionPolicy: String = "24_HOURS", // "IMMEDIATE", "24_HOURS"
    val defaultStoryTimerSec: Int = 5, // 1..10 or 0 for No Limit Loop
    val bitmojiAvatarStyle: String = "3D Cyber Neon",
    val bitmojiPoseEmoji: String = "😎",
    val bitmojiBgColorHex: Long = 0xFFFFFC00,
    val snapStreakEnabled: Boolean = true,
    val streakRestoreAvailable: Int = 3,
    val cameraGridEnabled: Boolean = true,
    val levelHorizonEnabled: Boolean = false,
    val nightModeBoost: Boolean = false,
    val defaultArLens: String = "NONE", // "NONE", "NEON_GLASSES", "DOG_EARS", "ROYAL_CROWN", "FIRE_EYES", "HEART_CROWN"
    val whoCanContactMe: String = "Friends & Creators", // "Everyone", "Friends & Creators", "Friends Only"
    val whoCanViewMyStory: String = "Everyone" // "Everyone", "My Friends", "Custom"
)

data class StickerItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val emojiOrText: String = "🔥",
    val imageUrl: String = "",
    val category: String = "Trending", // Trending, Desi Vibes, Reactions, Custom
    val bgHex: Long = 0xFF00E676,
    val createdByUid: String = "system",
    val createdAt: Long = System.currentTimeMillis()
)

data class KeyframePoint(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timeRatio: Float, // 0f..1f
    val zoomScale: Float = 1.25f, // 0.8f..2.5f
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    val label: String = "Zoom"
)

data class TimedCaptionSegment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val startRatio: Float,
    val endRatio: Float,
    val highlightColorHex: Long = 0xFFFFFC00
)

data class PlacedStickerLayer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val emojiOrLabel: String,
    val imageUrl: String = "",
    val xRatio: Float = 0.5f,
    val yRatio: Float = 0.35f,
    val scale: Float = 1.0f
)

data class PlacedTextLayer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val styleName: String = "Neon Cyber", // Neon Cyber, CapCut Yellow, Classic Box, Minimalist
    val colorHex: Long = 0xFF00F2FE,
    val yRatio: Float = 0.25f
)

data class CapCutEditProject(
    val trimStartRatio: Float = 0.0f,
    val trimEndRatio: Float = 1.0f,
    val splitPoints: List<Float> = emptyList(),
    val playbackSpeed: Float = 1.0f,
    val speedCurvePreset: String = "Normal", // Normal, Montage, Hero, Bullet, Jump Cut
    val keyframes: List<KeyframePoint> = emptyList(),
    val activeTransition: String = "None", // None, Glitch, Flash White, Spin Zoom, Dissolve
    val chromaCutoutEnabled: Boolean = false,
    val chromaBgColorHex: Long = 0xFF000000,
    val brightness: Float = 1.0f, // 0.5f..1.5f
    val contrast: Float = 1.0f,   // 0.5f..1.8f
    val saturation: Float = 1.0f, // 0.0f..2.0f
    val sharpen: Float = 0.0f,    // 0.0f..1.0f
    val vignette: Float = 0.0f,   // 0.0f..1.0f
    val activeVisualEffect: String = "None", // None, VHS Retro, Cyber Glitch, Golden Halo, Shake Bass
    val originalVolume: Float = 1.0f,
    val musicVolume: Float = 0.85f,
    val activeSoundFx: String = "None",
    val voiceoverRecorded: Boolean = false,
    val voiceoverDurationSec: Int = 0,
    val autoCaptionsEnabled: Boolean = false,
    val autoCaptions: List<TimedCaptionSegment> = emptyList(),
    val stickers: List<PlacedStickerLayer> = emptyList(),
    val textLayers: List<PlacedTextLayer> = emptyList(),
    val exportResolution: String = "1080p • 60FPS"
)

data class CallLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val conversationId: String,
    val otherUsername: String,
    val otherDisplayName: String,
    val otherAvatarUrl: String = "",
    val isVideoCall: Boolean = false,
    val direction: String = "OUTGOING", // INCOMING, OUTGOING, MISSED
    val durationText: String = "04:18",
    val timestamp: Long = System.currentTimeMillis()
)

data class StatusUpdate(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userUid: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String = "",
    val mediaCaption: String,
    val backgroundColorHex: Long = 0xFF005C4B,
    val emojiBadge: String = "🔥",
    val timestamp: Long = System.currentTimeMillis(),
    val isViewed: Boolean = false,
    val storyTimerSec: Int = 5
)

data class VideoDraft(
    val id: String,
    val localUri: String,
    val caption: String,
    val hashtags: List<String>,
    val filterName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserFollow(
    val followId: String = "",
    val followerUid: String = "",
    val followedUid: String = "",
    val followedUsername: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class VideoReport(
    val reportId: String,
    val videoId: String,
    val videoTitle: String,
    val videoCreatorUid: String,
    val reporterUid: String,
    val reason: String,
    val description: String = "",
    val status: String = "PENDING_MODERATION", // PENDING_MODERATION, REVIEWED, ACTION_TAKEN, DISMISSED
    val timestamp: Long = System.currentTimeMillis()
)

data class WalletBalance(
    val coins: Int = 0,
    val tokens: Int = 0,
    val cashPkr: Double = 0.0,
    val cashUsd: Double = 0.0,
    val dailySpendLimitPkr: Double = 50000.0,
    val dailySpentTodayPkr: Double = 0.0
)

data class PayoutAccount(
    val type: String = "JazzCash", // JazzCash, EasyPaisa, Bank IBAN, UPI
    val accountNumber: String = "",
    val accountHolder: String = "",
    val bankName: String = "Meezan Bank Ltd",
    val isVerified: Boolean = true
)

data class TransactionRecord(
    val id: String,
    val title: String,
    val type: String, // Coin Purchase, Gift Sent, Creator Fund Payout, Bank Withdrawal
    val amount: String,
    val currency: String, // PKR, Coins, USD
    val isCredit: Boolean,
    val status: String = "COMPLETED", // COMPLETED, PENDING, PROCESSING
    val timestamp: Long = System.currentTimeMillis()
)

data class ActiveSessionItem(
    val id: String,
    val deviceName: String,
    val location: String,
    val ipAddress: String,
    val lastActive: String,
    val isCurrentDevice: Boolean = false
)

data class BlockedUserItem(
    val uid: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String = "",
    val blockedDate: String = "Recently"
)

data class VideoFilter(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Original",
    val emoji: String = "✨",
    val category: String = "Trending", // Trending, Portrait, Retro, Cinematic, Mood, Cyberpunk, Glamour
    val colorTintHex: String = "#00000000",
    val tintAlpha: Float = 0.0f,
    val saturation: Float = 1.0f, // 0.0f for B&W, 1.0f for normal, 1.5f for vivid
    val contrast: Float = 1.0f,
    val brightness: Float = 1.0f,
    val badge: String = "", // "HOT", "NEW", "VIP", "POPULAR"
    val isActive: Boolean = true,
    val order: Int = 0,
    val createdBy: String = "Admin",
    val createdAt: Long = System.currentTimeMillis()
)
