package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NetworkConnectivityObserver
import com.example.data.VideoRepository
import com.example.model.VideoItem
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.AudioPlayerBottomBar
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class AppScreen {
    SPLASH,
    MAIN,
    CAMERA_UPLOAD,
    CREATOR_STUDIO,
    ADD_FUNDS,
    SETTINGS,
    EMAIL_VERIFICATION,
    AUTH,
    ADMIN_PANEL
}

enum class MainTab(val label: String) {
    HOME("Home"),
    DISCOVER("Discover"),
    INBOX("Inbox"),
    PROFILE("Profile")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TashanTheme(darkTheme = true) {
                TashanApp()
            }
        }
    }
}

@Composable
fun TashanApp() {
    val repository = remember { VideoRepository() }
    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    val showAuthModal by repository.showAuthModal.collectAsState()
    val authModalReason by repository.authModalReason.collectAsState()

    // Real-time Network Connectivity Monitoring
    val context = LocalContext.current
    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val networkStatus by connectivityObserver.observe().collectAsState(initial = NetworkConnectivityObserver.Status.Available)
    val isNetworkLost = networkStatus == NetworkConnectivityObserver.Status.Lost || networkStatus == NetworkConnectivityObserver.Status.Unavailable

    val coroutineScope = rememberCoroutineScope()

    // Sync real network status with repository state
    LaunchedEffect(isNetworkLost) {
        repository.setOfflineMode(isNetworkLost)
    }

    // Back handling
    BackHandler(enabled = currentScreen != AppScreen.SPLASH) {
        when {
            currentScreen != AppScreen.MAIN -> {
                currentScreen = AppScreen.MAIN
            }
            currentTab != MainTab.HOME -> {
                currentTab = MainTab.HOME
            }
            else -> {
                // Let system handle default back
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
    ) {
        when (currentScreen) {
            AppScreen.SPLASH -> {
                SplashScreen(
                    onSplashFinished = {
                        currentScreen = AppScreen.MAIN
                    }
                )
            }

            AppScreen.MAIN -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BgBase,
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                        ) {
                            // Persistent audio player preview bar if audio is actively playing
                            AudioPlayerBottomBar(
                                audioPlayerManager = repository.audioPlayerManager,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // TikTok-style Bottom Navigation Bar
                            TashanBottomNavBar(
                                currentTab = currentTab,
                                onTabSelected = { tab ->
                                    currentTab = tab
                                },
                                onAddClick = {
                                    currentScreen = AppScreen.CAMERA_UPLOAD
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        when (currentTab) {
                            MainTab.HOME -> {
                                FeedScreen(
                                    repository = repository,
                                    onOpenSearch = {
                                        currentTab = MainTab.DISCOVER
                                    },
                                    onOpenNotifications = {
                                        currentTab = MainTab.INBOX
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            MainTab.DISCOVER -> {
                                SearchScreen(
                                    repository = repository,
                                    onVideoSelected = { videoItem: VideoItem ->
                                        currentTab = MainTab.HOME
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            MainTab.INBOX -> {
                                InboxScreen(
                                    repository = repository,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            MainTab.PROFILE -> {
                                ProfileScreen(
                                    repository = repository,
                                    onOpenSettings = {
                                        currentScreen = AppScreen.SETTINGS
                                    },
                                    onNavigateToUpload = {
                                        currentScreen = AppScreen.CAMERA_UPLOAD
                                    },
                                    onNavigateToAddFunds = {
                                        currentScreen = AppScreen.ADD_FUNDS
                                    },
                                    onNavigateToAdminPanel = {
                                        currentScreen = AppScreen.ADMIN_PANEL
                                    },
                                    onOpenNotifications = {
                                        currentTab = MainTab.INBOX
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            AppScreen.SETTINGS -> {
                SettingsAndPrivacyScreen(
                    repository = repository,
                    onBack = {
                        currentScreen = AppScreen.MAIN
                    },
                    onNavigateToAddFunds = {
                        currentScreen = AppScreen.ADD_FUNDS
                    },
                    onNavigateToCreatorStudio = {
                        currentScreen = AppScreen.CREATOR_STUDIO
                    },
                    onNavigateToAdminPanel = {
                        currentScreen = AppScreen.ADMIN_PANEL
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppScreen.CAMERA_UPLOAD -> {
                CameraUploadScreen(
                    repository = repository,
                    onUploadComplete = {
                        currentScreen = AppScreen.MAIN
                        currentTab = MainTab.HOME
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppScreen.CREATOR_STUDIO -> {
                CreatorStudioScreen(
                    repository = repository,
                    onBack = {
                        currentScreen = AppScreen.MAIN
                    }
                )
            }

            AppScreen.ADD_FUNDS -> {
                AddFundsScreen(
                    repository = repository,
                    onBack = {
                        currentScreen = AppScreen.MAIN
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }



            AppScreen.EMAIL_VERIFICATION -> {
                EmailVerificationGateScreen(
                    repository = repository,
                    onVerified = {
                        currentScreen = AppScreen.MAIN
                    },
                    onLogout = {
                        repository.logout()
                        currentScreen = AppScreen.MAIN
                    }
                )
            }

            AppScreen.AUTH -> {
                AuthScreen(
                    repository = repository,
                    reason = "Log in to access your creator tools",
                    onClose = {
                        currentScreen = AppScreen.MAIN
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppScreen.ADMIN_PANEL -> {
                AdminControlPanelScreen(
                    repository = repository,
                    onBack = {
                        currentScreen = AppScreen.MAIN
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // FULL-SCREEN NO INTERNET OVERLAY (Tashan Style)
        AnimatedVisibility(
            visible = isNetworkLost,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            NoInternetScreen(
                onRefresh = {
                    // Simple refresh logic: The observer will automatically update 
                    // state when connection returns, but we can show a feedback toast here.
                    if (!isNetworkLost) {
                        android.widget.Toast.makeText(context, "Internet connection restored! ✅", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Still offline. Please check your data settings. 🔍", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Global Auth modal sheet if requested by repository
        if (showAuthModal) {
            AuthModalBottomSheet(
                repository = repository,
                reason = authModalReason,
                onDismiss = {
                    repository.closeAuthModal()
                }
            )
        }
    }
}

@Composable
fun TashanBottomNavBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tashan_bottom_navigation"),
        color = Color.Black,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(54.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Home Tab
            BottomNavItem(
                label = "Home",
                isSelected = currentTab == MainTab.HOME,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                onClick = { onTabSelected(MainTab.HOME) },
                testTag = "nav_home"
            )

            // Discover Tab
            BottomNavItem(
                label = "Discover",
                isSelected = currentTab == MainTab.DISCOVER,
                selectedIcon = Icons.Filled.Search,
                unselectedIcon = Icons.Outlined.Search,
                onClick = { onTabSelected(MainTab.DISCOVER) },
                testTag = "nav_discover"
            )

            // Center "+" Add Button (TikTok Style)
            TikTokCreateButton(
                onClick = onAddClick,
                modifier = Modifier.testTag("nav_create_video")
            )

            // Inbox Tab with Unified Green Notification Badge
            BottomNavItem(
                label = "Inbox",
                isSelected = currentTab == MainTab.INBOX,
                selectedIcon = Icons.Filled.Mail,
                unselectedIcon = Icons.Outlined.Mail,
                onClick = { onTabSelected(MainTab.INBOX) },
                testTag = "nav_inbox",
                hasBadge = true
            )

            // Profile Tab
            BottomNavItem(
                label = "Profile",
                isSelected = currentTab == MainTab.PROFILE,
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                onClick = { onTabSelected(MainTab.PROFILE) },
                testTag = "nav_profile"
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    isSelected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    hasBadge: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val color = if (isSelected) Color.White else Color(0xFF8E8E93)

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            if (hasBadge) {
                // Unified Green Notification Dot with Glow
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(NotificationGreen)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun TikTokCreateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(48.dp)
            .height(32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Left Cyan badge
        Box(
            modifier = Modifier
                .offset(x = (-3).dp)
                .width(40.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TashanCyan)
        )
        // Right Green Accent badge
        Box(
            modifier = Modifier
                .offset(x = 3.dp)
                .width(40.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TashanGreenDark)
        )
        // Middle Vibrant Neon Cyber Green Card with crisp Black '+' Icon
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            TashanGreen,
                            TashanGreenDark
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create Video",
                tint = Color.Black,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
