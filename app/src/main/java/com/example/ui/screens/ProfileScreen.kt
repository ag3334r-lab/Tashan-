package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoRepository
import com.example.model.VideoItem
import com.example.ui.components.AccountSwitcherBottomSheet
import com.example.ui.components.UserProfileComponent
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    repository: VideoRepository,
    onOpenSettings: () -> Unit,
    onNavigateToUpload: () -> Unit = {},
    onNavigateToAddFunds: () -> Unit = {},
    onNavigateToAdminPanel: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLoggedIn by repository.isLoggedIn.collectAsState()
    val user by repository.currentUser.collectAsState()
    val savedAccounts by repository.savedAccounts.collectAsState()
    val userPosts by repository.userPosts.collectAsState()
    val savedVideos by repository.savedVideos.collectAsState()

    val totalLikes = remember(userPosts) { userPosts.sumOf { it.likesCount } }
    val totalViews = remember(userPosts) { userPosts.sumOf { it.viewsCount } }

    val dynamicUser = user.copy(
        postsCount = userPosts.size,
        totalLikes = totalLikes,
        totalViews = totalViews
    )

    var showAccountSwitcher by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showWalletDialog by remember { mutableStateOf(false) }

    var showCreatorStudio by remember { mutableStateOf(false) }
    var selectedPreviewVideo by remember { mutableStateOf<VideoItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgBase)
            .statusBarsPadding()
            .testTag("profile_screen")
    ) {
        if (!isLoggedIn) {
            TikTokGuestProfileView(
                onLoginClick = {
                    repository.openAuthModal("Log in to customize your profile, upload videos, and save favorites")
                },
                onOpenSettings = onOpenSettings
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable { showAccountSwitcher = true }
                        .padding(vertical = 4.dp)
                        .testTag("profile_account_dropdown_trigger")
                ) {
                    Text(
                        text = "@${user.username}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Switch Account",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Surface(
                        color = TashanCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when (user.authProvider) {
                                "phone" -> "Phone"
                                "google" -> "Google"
                                "apple" -> "Apple"
                                else -> "Creator"
                            },
                            color = TashanCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showAccountSwitcher = true },
                        modifier = Modifier.testTag("profile_switch_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwitchAccount,
                            contentDescription = "Switch Account",
                            tint = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = { showQrDialog = true },
                        modifier = Modifier.testTag("profile_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode,
                            contentDescription = "QR Code",
                            tint = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("profile_notifications_button")
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = TextPrimary
                            )
                            // Unified Green Notification Badge
                            Box(
                                modifier = Modifier
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NotificationGreen)
                            )
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("profile_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary
                        )
                    }
                }
            }

            if (user.isBanned) {
                Surface(
                    color = Color(0xFF3B151C),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TashanPink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onOpenSettings() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.LockPerson, contentDescription = null, tint = TashanPink)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Account Restricted / Banned", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Please contact support or review settings", color = TashanPink, fontSize = 10.sp)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TashanPink)
                    }
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            UserProfileComponent(
                user = dynamicUser,
                uploadedVideos = userPosts,
                savedVideos = savedVideos,
                onEditProfileClick = { showEditProfileDialog = true },
                onShareProfileClick = { showQrDialog = true },
                onWalletClick = onNavigateToAddFunds,
                onOpenCreatorStudio = { showCreatorStudio = true },
                onOpenAdminPanel = onNavigateToAdminPanel,
                onVideoClick = { video -> selectedPreviewVideo = video },
                onUploadPromptClick = onNavigateToUpload,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showAccountSwitcher) {
        AccountSwitcherBottomSheet(
            repository = repository,
            currentUser = user,
            savedAccounts = savedAccounts,
            onAddAccountClick = {
                repository.openAuthModal("Add or switch into another Tashan account")
            },
            onLogoutClick = {
                repository.logout()
            },
            onDismiss = { showAccountSwitcher = false }
        )
    }

    selectedPreviewVideo?.let { video ->
        AlertDialog(
            onDismissRequest = { selectedPreviewVideo = null },
            title = {
                Text(
                    text = video.caption,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎵 ${video.musicTitle}", color = TashanCyan, fontSize = 12.sp)
                    Text("🏷️ ${video.hashtags.joinToString(" ") { "#$it" }}", color = TextSecondary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("❤️ ${video.likesCount} likes", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("💬 ${video.commentsCount} comments", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedPreviewVideo = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TashanTeal)
                ) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = BgElevated
        )
    }



    if (showCreatorStudio) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showCreatorStudio = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CreatorStudioScreen(
                repository = repository,
                onBack = { showCreatorStudio = false }
            )
        }
    }

    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(user.displayName) }
        var editUsername by remember { mutableStateOf(user.username) }
        var editBirthDate by remember { mutableStateOf(user.birthDate) }
        var editCountry by remember { mutableStateOf(user.country) }
        var editBio by remember { mutableStateOf(user.bio) }
        var editAvatarUrl by remember { mutableStateOf(user.avatarUrl) }

        LaunchedEffect(editName) {
            if (editUsername.isEmpty() || editUsername.startsWith("user_")) {
                editUsername = editName.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
            }
        }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Set up profile", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(BgElevated)
                                .border(1.dp, BorderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (editAvatarUrl.isNotEmpty()) {
                                Icon(Icons.Filled.Person, contentDescription = "Avatar", tint = TextSecondary, modifier = Modifier.size(40.dp))
                            } else {
                                Icon(Icons.Filled.CameraAlt, contentDescription = "Change Photo", tint = TextSecondary, modifier = Modifier.size(30.dp))
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TashanCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input")
                    )

                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it.lowercase().replace(Regex("[^a-z0-9_]"), "") },
                        label = { Text("Username (ID)") },
                        leadingIcon = { Text("@", color = TextSecondary, modifier = Modifier.padding(start = 12.dp)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TashanCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_username_input")
                    )

                    OutlinedTextField(
                        value = editBirthDate,
                        onValueChange = { editBirthDate = it },
                        label = { Text("Birth Date (DD/MM/YYYY)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TashanCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCountry,
                        onValueChange = { editCountry = it },
                        label = { Text("Country") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TashanCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Status (Bio)") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TashanCyan,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_bio_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.updateProfileFull(editName, editUsername, editBirthDate, editCountry, editBio, editAvatarUrl)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TashanTeal),
                    modifier = Modifier.testTag("save_profile_button")
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = BgBase
        )
    }

    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("Share Tashan Profile", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(110.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "@${user.username}",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQrDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TashanTeal)
                ) {
                    Text("Done", color = Color.White)
                }
            },
            containerColor = BgElevated
        )
    }
}

@Composable
fun TikTokGuestProfileView(
    onLoginClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("guest_profile_view")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            IconButton(onClick = onOpenSettings) {
                Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = TextPrimary)
            }
        }
        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(BgElevated2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Guest",
                    tint = TextTertiary,
                    modifier = Modifier.size(54.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Sign up for an account",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Manage your profile, create and upload reels, follow your favorite creators, and interact with the Tashan community.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(48.dp)
                    .testTag("guest_profile_login_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TashanTeal)
            ) {
                Text(
                    text = "Sign up / Log in",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
