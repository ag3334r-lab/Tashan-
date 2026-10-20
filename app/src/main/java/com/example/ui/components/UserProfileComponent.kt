package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.model.VideoItem
import com.example.ui.theme.*

@Composable
fun UserProfileComponent(
    user: UserProfile,
    uploadedVideos: List<VideoItem>,
    savedVideos: List<VideoItem> = emptyList(),
    onEditProfileClick: () -> Unit = {},
    onShareProfileClick: () -> Unit = {},
    onWalletClick: () -> Unit = {},
    onOpenCreatorStudio: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    onVideoClick: (VideoItem) -> Unit = {},
    onUploadPromptClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ProfileVideoTab.UPLOADED) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("user_profile_component")
    ) {
        // --- 1. User Profile Header Section ---
        UserProfileHeader(
            user = user,
            onEditProfileClick = onEditProfileClick,
            onShareProfileClick = onShareProfileClick,
            onWalletClick = onWalletClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- 👑 MASTER ADMIN CONTROL HUB BANNER ---
        Surface(
            color = Color(0xFF191228),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, TashanCyan),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .align(Alignment.CenterHorizontally)
                .clickable { onOpenAdminPanel() }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TashanCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👑", fontSize = 20.sp)
                    }
                    Column {
                        Text("Master Admin Control Panel 👑", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Payouts (10 PKR/1k views), Gifting Lock & Moderation", color = TashanCyan, fontSize = 11.sp)
                    }
                }
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = TashanCyan)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- CREATOR STUDIO PRO & LIVE BROADCAST HUB BANNER ---
        Surface(
            color = BgElevated,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(TashanPink, TashanCyan))),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .align(Alignment.CenterHorizontally)
                .clickable { onOpenCreatorStudio() }
        ) {

            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(TashanPink, TashanCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RocketLaunch,
                            contentDescription = "Studio",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text("Tashan Creator Studio Pro 🚀", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Live streaming, analytics & instant payouts", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = TashanCyan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Firestore Collection Tabs (Uploaded / Saved) ---
        ProfileCollectionTabs(
            selectedTab = selectedTab,
            uploadedCount = uploadedVideos.size,
            savedCount = savedVideos.size,
            onTabSelected = { selectedTab = it }
        )

        HorizontalDivider(
            color = BorderSubtle,
            thickness = 1.dp
        )

        // --- 3. Grid View of Uploaded / Saved Videos from Firestore ---
        val currentDisplayVideos = when (selectedTab) {
            ProfileVideoTab.UPLOADED -> uploadedVideos
            ProfileVideoTab.SAVED -> savedVideos
        }

        if (currentDisplayVideos.isEmpty()) {
            ProfileEmptyGridState(
                tab = selectedTab,
                onUploadPromptClick = onUploadPromptClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("user_profile_videos_grid"),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(currentDisplayVideos, key = { it.id }) { video ->
                    UserVideoGridCard(
                        video = video,
                        isSavedTab = selectedTab == ProfileVideoTab.SAVED,
                        onClick = { onVideoClick(video) },
                        modifier = Modifier.testTag("user_video_grid_item_${video.id}")
                    )
                }
            }
        }
    }
}

enum class ProfileVideoTab {
    UPLOADED,
    SAVED
}

@Composable
fun UserProfileHeader(
    user: UserProfile,
    onEditProfileClick: () -> Unit,
    onShareProfileClick: () -> Unit,
    onWalletClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dynamicLevel = (user.totalLikes / 100) + 1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar with Glowing Gradient Border & Level Badge
        Box(
            modifier = Modifier
                .size(92.dp)
                .testTag("user_profile_avatar"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(TashanTeal, TashanCyan, TashanTealDark, TashanTeal)
                        )
                    )
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(BgElevated2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.displayName.ifEmpty { user.username }.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp
                    )
                }
            }
            Surface(
                color = TashanTeal,
                shape = CircleShape,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lv.$dynamicLevel",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Display Name & Verified Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = user.displayName.ifEmpty { "Tashan Creator" },
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                modifier = Modifier.testTag("user_profile_display_name")
            )
            if (user.isPhoneVerified || user.isEmailVerified) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Verified Creator",
                    tint = TashanCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Username Tag
        Text(
            text = "@${user.username}",
            color = TashanCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.testTag("user_profile_username")
        )

        // Creator Star ID Badge (1★ to 5★)
        val starRating = user.computedStarRating()
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E1A0E),
            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.7f)),
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag("user_profile_star_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    repeat(starRating) {
                        Text("⭐", fontSize = 11.sp)
                    }
                }
                Text(
                    text = user.starRatingBadgeLabel(),
                    color = Color(0xFFFFD700),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Creator 5K-10K Followers + 500K Views Monetization Status Card
        Surface(
            color = if (user.isMonetizationUnlocked()) Color(0xFF122818) else Color(0xFF171926),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (user.isMonetizationUnlocked()) Color(0xFF00E676) else BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("monetization_status_card")
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(if (user.isMonetizationUnlocked()) "💰" else "🎯", fontSize = 13.sp)
                        Text(
                            text = if (user.isMonetizationUnlocked()) "Creator Monetization UNLOCKED" else "Monetization: 5K-10K Followers & 500K Views",
                            color = if (user.isMonetizationUnlocked()) Color(0xFF00E676) else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = if (user.isMonetizationUnlocked()) "ACTIVE" else "${((user.totalViews.coerceAtMost(500000).toFloat() / 500000f) * 100).toInt()}%",
                        color = if (user.isMonetizationUnlocked()) Color(0xFF00E676) else TashanCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Followers: ${user.followersCount} / 5,000",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Views: ${user.totalViews} / 500,000",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        if (user.bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = user.bio,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Achievements / Badges Row
        if (user.achievements.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                user.achievements.forEach { badge ->
                    Surface(
                        color = BgElevated2,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TashanTeal.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "⭐ $badge",
                            color = TashanCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Follower / Posts / Likes Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_profile_stats_row"),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStatBox(count = "${user.postsCount}", label = "Uploads", icon = Icons.Filled.VideoLibrary)
            ProfileStatBox(count = formatProfileCount(user.totalLikes), label = "Likes", icon = Icons.Filled.Favorite)
            ProfileStatBox(count = formatProfileCount(user.followersCount), label = "Followers", icon = Icons.Filled.People)
            ProfileStatBox(count = formatProfileCount(user.followingCount), label = "Following", icon = Icons.Filled.PersonAdd)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Monetization status
        val isMonetized = user.followersCount >= 5000 && user.totalViews >= 50000
        Surface(
            color = if (isMonetized) TashanTeal.copy(alpha = 0.1f) else BgElevated2,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isMonetized) TashanTeal else BorderSubtle),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isMonetized) "💰 Monetization Active" else "🔒 Monetization Locked",
                        color = if (isMonetized) TashanTeal else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Req: 5K Followers & 50K Views",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "${user.totalViews} / 50K Views",
                    color = if (user.totalViews >= 50000) TashanTeal else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // User Wallet Summary Card
        Surface(
            color = Color(0xFF1A1D2E),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Brush.horizontalGradient(listOf(TashanTeal, TashanCyan))),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable { onWalletClick() }
                .testTag("user_profile_wallet_summary_card")
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(TashanTeal, TashanCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text("Tashan Wallet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙 ${user.coins}", color = TashanYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("•", color = Color.Gray, fontSize = 10.sp)
                            Text("PKR ${user.balancePkr.toInt()}", color = TashanCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                Surface(
                    color = TashanCyan,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Wallet ➔", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row (Edit Profile & Share)
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onEditProfileClick,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = RoundedCornerShape(19.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BgElevated2,
                    contentColor = TextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(TashanTeal, TashanCyan))
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TashanCyan
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onShareProfileClick,
                modifier = Modifier.height(38.dp),
                shape = RoundedCornerShape(19.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share Profile",
                    modifier = Modifier.size(15.dp),
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ProfileStatBox(
    count: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TashanTeal,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = count,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }
        Text(
            text = label,
            color = TextTertiary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProfileCollectionTabs(
    selectedTab: ProfileVideoTab,
    uploadedCount: Int,
    savedCount: Int,
    onTabSelected: (ProfileVideoTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ProfileTabItem(
            icon = Icons.Filled.GridOn,
            label = "Uploaded ($uploadedCount)",
            selected = selectedTab == ProfileVideoTab.UPLOADED,
            onClick = { onTabSelected(ProfileVideoTab.UPLOADED) },
            modifier = Modifier.weight(1f)
        )
        ProfileTabItem(
            icon = Icons.Filled.Bookmark,
            label = "Saved ($savedCount)",
            selected = selectedTab == ProfileVideoTab.SAVED,
            onClick = { onTabSelected(ProfileVideoTab.SAVED) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ProfileTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor by animateColorAsState(
        targetValue = if (selected) TashanCyan else TextTertiary,
        animationSpec = tween(200),
        label = "tabColor"
    )

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = activeColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                color = activeColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.6f)
                .background(if (selected) TashanCyan else Color.Transparent)
        )
    }
}

@Composable
fun UserVideoGridCard(
    video: VideoItem,
    isSavedTab: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(4.dp))
            .background(BgElevated)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            BgElevated2,
                            BgElevated,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        if (video.category.isNotBlank()) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Text(
                    text = video.category,
                    color = TashanCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        if (isSavedTab) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = "Saved",
                    tint = WarningYellow,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Text(
                text = video.caption,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = formatProfileCount(video.likesCount),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProfileEmptyGridState(
    tab: ProfileVideoTab,
    onUploadPromptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (tab == ProfileVideoTab.UPLOADED) Icons.Outlined.VideoLibrary else Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (tab == ProfileVideoTab.UPLOADED) "No Uploads in Firestore 'Videos' Yet" else "No Saved Videos Yet",
            color = TextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (tab == ProfileVideoTab.UPLOADED)
                "Create and share your vertical reels with the Tashan community!"
            else
                "Bookmark your favorite reels from the feed to watch them later.",
            color = TextTertiary,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (tab == ProfileVideoTab.UPLOADED) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onUploadPromptClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TashanTeal,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload Reel", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatProfileCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
