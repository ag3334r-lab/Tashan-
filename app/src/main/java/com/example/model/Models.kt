package com.example.model

data class UserProfile(
    val uid: String = "user_101",
    val username: String = "TikTokCreator",
    val displayName: String = "Tashan Creator",
    val profilePicUrl: String = "https://picsum.photos/200",
    val coinBalance: Int = 1250,
    val totalViews: Long = 15400,
    val isVerified: Boolean = true,
    val isAdmin: Boolean = true
) {
    fun calculateViewEarningsPkr(ratePer1000Views: Double = 10.0): Double {
        return (totalViews / 1000.0) * ratePer1000Views
    }
}

data class VideoItem(
    val id: String = "vid_1",
    val title: String = "Trending Short Video",
    val creatorUid: String = "user_101",
    val creatorName: String = "Tashan Creator",
    val videoUrl: String = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    val likesCount: Int = 340,
    val commentsCount: Int = 45,
    val viewsCount: Long = 1200,
    val viewedUserUids: List<String> = emptyList()
)

data class AdminSystemControl(
    val isGiftingAndPurchasesLocked: Boolean = true,
    val giftingLockNoticeMessage: String = "Gifting and Coin Purchases are currently locked in Trailer Preview Mode until payment gateway activation.",
    val monetizationPkrPer1000Views: Double = 10.0,
    val isMonetizationSystemActive: Boolean = true,
    val globalMaintenanceMode: Boolean = false,
    val maintenanceReason: String = "System undergoing scheduled maintenance",
    val totalRevenueDistributedPkr: Double = 154.0,
    val totalViewsLogged: Long = 15400
)

data class SnackVideoSettings(
    val pkrRatePer1000Views: Double = 10.0,
    val minWithdrawalPkr: Double = 100.0
)
