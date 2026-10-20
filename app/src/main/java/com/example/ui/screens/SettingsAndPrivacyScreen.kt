package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.RealtimeChatManager
import com.example.data.VideoRepository
import com.example.model.UserProfile
import com.example.ui.theme.*

/**
 * TikTok-style vertical "Settings and privacy" screen.
 * Displays all features in a straight vertical list organized into clear sections:
 * Account, Content & Display, Cache & Cellular, Support & About, and Login.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndPrivacyScreen(
    repository: VideoRepository,
    onBack: () -> Unit,
    onNavigateToAddFunds: () -> Unit = {},
    onNavigateToCreatorStudio: () -> Unit = {},
    onNavigateToAdminPanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val snackSettings by repository.snackVideoSettings.collectAsState()
    val chatManager = remember { RealtimeChatManager.getInstance() }
    val blockedUsersSet by chatManager.blockedUsers.collectAsState()
    val blockedList = remember(blockedUsersSet) { blockedUsersSet.toList() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Sub-dialog states
    var showManageAccountDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showSecurityPinDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showBlockedAccountsDialog by remember { mutableStateOf(false) }
    var showReportProblemDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showCreatorMilestonesDialog by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showOAuthSessionDialog by remember { mutableStateOf(false) }
    var showAdvancedVideoEngineDialog by remember { mutableStateOf(false) }

    // Dynamic settings state synchronized with repository.snackVideoSettings
    var selectedLanguage by remember { mutableStateOf("English (US)") }
    var cdnPingLatencyMs by remember { mutableIntStateOf(18) }

    // PIN input state
    var pinInput by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<String?>(null) }

    // Report state
    var reportText by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgBase)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("settings_and_privacy_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TikTok-style Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Settings and privacy",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)

            // Vertical list of all settings options
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("settings_vertical_list"),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // ==========================================
                // 1. ACCOUNT
                // ==========================================
                item {
                    SettingsSectionHeader(title = "ACCOUNT & MASTER ADMIN")
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.AdminPanelSettings,
                        title = "👑 Master Admin Control Panel",
                        subtitle = "Payout Rules (10 PKR/1k views), Gifting Lock & Moderation",
                        testTag = "settings_admin_panel",
                        onClick = { onNavigateToAdminPanel() }
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.Person,
                        title = "Manage account",
                        subtitle = if (currentUser.email.isNotBlank()) currentUser.email else "Phone & Security",
                        testTag = "settings_manage_account",
                        onClick = { showManageAccountDialog = true }
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.Lock,
                        title = "Privacy",
                        subtitle = if (currentUser.isPrivate) "Private account active" else "Public profile",
                        testTag = "settings_privacy",
                        onClick = { showPrivacyDialog = true }
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.Security,
                        title = "Security & permissions",
                        subtitle = "4-Digit Wallet PIN & 2-Step verification",
                        testTag = "settings_security",
                        onClick = { showSecurityPinDialog = true }
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = "Balance & Coins",
                        badgeText = "${currentUser.coins} Coins",
                        subtitle = "PKR ${String.format("%.1f", currentUser.balancePkr)} • Instant JazzCash/EasyPaisa",
                        testTag = "settings_balance",
                        onClick = { onNavigateToAddFunds() }
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.QrCode,
                        title = "My QR Code",
                        subtitle = "Scan to add me on Tashan",
                        testTag = "settings_qr_code",
                        onClick = { showQrCodeDialog = true }
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.Share,
                        title = "Share profile",
                        subtitle = "@${currentUser.username}",
                        testTag = "settings_share_profile",
                        onClick = {
                            clipboardManager.setText(AnnotatedString("https://tashan.app/@${currentUser.username}"))
                            Toast.makeText(context, "Profile link copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // ==========================================
                // 2. CONTENT & DISPLAY
                // ==========================================
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSectionHeader(title = "CONTENT & DISPLAY")
                }
                item {
                    SettingsListItem(
                        icon = Icons.Filled.Stars,
                        title = "Creator tools & Monetization",
                        badgeText = "${currentUser.computedStarRating()}★ Star",
                        subtitle = "5K–10K Followers + 500K Views Payout Hub",
                        testTag = "settings_creator_tools",
                        onClick = { showCreatorMilestonesDialog = true }
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.Notifications,
                        title = "Push notifications",
                        subtitle = "Likes, comments, live stream alerts",
                        testTag = "settings_notifications",
                        onClick = { showNotificationsDialog = true }
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.Translate,
                        title = "Language",
                        badgeText = selectedLanguage.take(7),
                        subtitle = "English, Urdu اردو, Punjabi, Sindhi, Pashto",
                        testTag = "settings_language",
                        onClick = { showLanguageDialog = true }
                    )
                }
                item {
                    SettingsSwitchItem(
                        icon = Icons.Outlined.AccessibilityNew,
                        title = "Easy Mode (Kids & Seniors)",
                        subtitle = "Large readable buttons and high-contrast cues",
                        isChecked = snackSettings.easyUiModeForAllAges,
                        onCheckedChange = {
                            repository.updateSnackVideoSettings(snackSettings.copy(easyUiModeForAllAges = it))
                            Toast.makeText(context, if (it) "Easy Mode Enabled" else "Standard Mode Active", Toast.LENGTH_SHORT).show()
                        },
                        testTag = "settings_easy_mode_switch"
                    )
                }

                // ==========================================
                // 3. ADVANCED SYSTEM ENGINE & CONTROLS
                // ==========================================
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSectionHeader(title = "ADVANCED SYSTEM ENGINE")
                }
                item {
                    SettingsListItem(
                        icon = Icons.Filled.VerifiedUser,
                        title = "OAuth & Session Security Center",
                        badgeText = currentUser.authProvider.uppercase(),
                        subtitle = "Browser OAuth bindings, active token validation & session revocation",
                        testTag = "settings_oauth_session_center",
                        onClick = { showOAuthSessionDialog = true }
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Filled.Speed,
                        title = "Video Quality, Bitrate, CDN & Cache Engine",
                        badgeText = "${snackSettings.maxBitrateMbps} Mbps • ${snackSettings.preferredVideoCodec.substringBefore(" ")}",
                        subtitle = "60fps GPU acceleration, codec pipeline, edge CDN routing & Pixabay cache",
                        testTag = "settings_advanced_video_engine",
                        onClick = { showAdvancedVideoEngineDialog = true }
                    )
                }
                item {
                    SettingsSwitchItem(
                        icon = Icons.Filled.Memory,
                        title = "60FPS Hardware Acceleration (MediaCodec)",
                        subtitle = "Zero-drop frame rendering for Pixabay FX overlays & HD feeds",
                        isChecked = snackSettings.hardwareAcceleration60Fps,
                        onCheckedChange = {
                            repository.updateSnackVideoSettings(snackSettings.copy(hardwareAcceleration60Fps = it))
                            Toast.makeText(context, if (it) "60FPS GPU Pipeline Active ⚡" else "Standard 30FPS Decoder", Toast.LENGTH_SHORT).show()
                        },
                        testTag = "settings_hw_60fps_switch"
                    )
                }
                item {
                    SettingsSwitchItem(
                        icon = Icons.Filled.Dns,
                        title = "DNS-over-HTTPS (DoH) Anti-Block Routing",
                        subtitle = "Encrypted edge resolution for uninterrupted OAuth & Pixabay streaming",
                        isChecked = snackSettings.dnsOverHttpsEnabled,
                        onCheckedChange = {
                            repository.updateSnackVideoSettings(snackSettings.copy(dnsOverHttpsEnabled = it))
                            Toast.makeText(context, if (it) "Encrypted DoH Routing Enabled 🛡️" else "Standard ISP DNS", Toast.LENGTH_SHORT).show()
                        },
                        testTag = "settings_doh_switch"
                    )
                }

                // ==========================================
                // 4. CACHE & CELLULAR
                // ==========================================
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSectionHeader(title = "CACHE & CELLULAR")
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.CleaningServices,
                        title = "Free up space",
                        badgeText = "${snackSettings.cacheSizeMb} MB",
                        subtitle = "Tap to clear cached videos, Pixabay FX overlays & audio tracks",
                        testTag = "settings_free_up_space",
                        onClick = {
                            repository.updateSnackVideoSettings(snackSettings.copy(cacheSizeMb = 0))
                            Toast.makeText(context, "All video, Pixabay FX, and audio caches purged! 0 MB used 🎉", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                item {
                    SettingsSwitchItem(
                        icon = Icons.Outlined.DataSaverOn,
                        title = "Data Saver",
                        subtitle = "Reduce cellular data usage on 3G/4G networks",
                        isChecked = snackSettings.dataSaverMode,
                        onCheckedChange = {
                            repository.updateSnackVideoSettings(snackSettings.copy(dataSaverMode = it))
                            Toast.makeText(context, if (it) "Data Saver ON" else "Data Saver OFF", Toast.LENGTH_SHORT).show()
                        },
                        testTag = "settings_data_saver_switch"
                    )
                }
                item {
                    SettingsSwitchItem(
                        icon = Icons.Filled.Bolt,
                        title = "Pakistan Smart CDN Turbo",
                        subtitle = "Route: ${snackSettings.activePakistanRoute} (${cdnPingLatencyMs}ms)",
                        isChecked = snackSettings.pakistanSmartCdnEnabled,
                        onCheckedChange = {
                            repository.updateSnackVideoSettings(snackSettings.copy(pakistanSmartCdnEnabled = it))
                            Toast.makeText(context, if (it) "Smart CDN Turbo Active ⚡" else "CDN Turbo Disabled", Toast.LENGTH_SHORT).show()
                        },
                        testTag = "settings_cdn_turbo_switch"
                    )
                }

                // ==========================================
                // 4. SUPPORT & ABOUT
                // ==========================================
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSectionHeader(title = "SUPPORT & ABOUT")
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.Flag,
                        title = "Report a problem",
                        subtitle = "Help us improve playback, monetization, or chat",
                        testTag = "settings_report_problem",
                        onClick = { showReportProblemDialog = true }
                    )
                }
                item { SettingsSectionHeader("Privacy & Security") }
                item {
                    SettingsSwitchItem(
                        icon = Icons.Outlined.Lock,
                        title = "Biometric / PIN App Lock",
                        subtitle = "Require PIN or biometric authentication to open Tashan",
                        isChecked = snackSettings.biometricAppLock,
                        onCheckedChange = {
                            repository.updateSnackVideoSettings(snackSettings.copy(biometricAppLock = it))
                            Toast.makeText(context, if (it) "Biometric & PIN App Lock Enabled 🔒" else "App Lock Disabled", Toast.LENGTH_SHORT).show()
                        },
                        testTag = "settings_app_lock_switch"
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.Block,
                        title = "Blocked accounts",
                        badgeText = "${blockedList.size}",
                        subtitle = "Manage restricted creators and chat users",
                        testTag = "settings_blocked_accounts",
                        onClick = { showBlockedAccountsDialog = true }
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.Description,
                        title = "Terms of Service & Privacy Policy",
                        subtitle = "Community guidelines, privacy terms & safety",
                        testTag = "settings_terms_policy",
                        onClick = { showTermsDialog = true }
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.Info,
                        title = "About Tashan",
                        badgeText = "v3.4.0 Pro",
                        subtitle = "Tashan High-Retention Short Video Ecosystem",
                        testTag = "settings_about_app",
                        onClick = {
                            Toast.makeText(context, "Tashan Pro v3.4.0 (Official Build)", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // ==========================================
                // 5. LOGIN
                // ==========================================
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSectionHeader(title = "LOGIN")
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.SwapHoriz,
                        title = "Switch account",
                        subtitle = "Logged in as @${currentUser.username}",
                        testTag = "settings_switch_account",
                        onClick = {
                            repository.openAuthModal("Switch account to continue")
                        }
                    )
                }
                item {
                    SettingsListItem(
                        icon = Icons.Outlined.Logout,
                        title = "Log out",
                        titleColor = TashanPink,
                        subtitle = "Sign out from this device",
                        testTag = "settings_logout",
                        onClick = { showLogoutConfirmDialog = true }
                    )
                }
            }
        }

        // ==========================================
        // SUB-DIALOGS & SHEETS
        // ==========================================

        // 1. Manage Account Dialog
        if (showManageAccountDialog) {
            Dialog(onDismissRequest = { showManageAccountDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Manage Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        AccountDetailRow(label = "Username", value = "@${currentUser.username}")
                        AccountDetailRow(label = "Display Name", value = currentUser.displayName)
                        AccountDetailRow(label = "Email", value = currentUser.email.ifBlank { "Not linked" })
                        AccountDetailRow(label = "Email Verified", value = if (currentUser.isEmailVerified) "Verified ✅" else "Unverified ⚠️")
                        AccountDetailRow(label = "Phone", value = currentUser.phoneNumber.ifBlank { "Not linked" })
                        AccountDetailRow(label = "Auth Provider", value = currentUser.authProvider.replaceFirstChar { it.uppercase() })
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { showManageAccountDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Privacy & Creator Controls Sheet
        if (showPrivacyDialog) {
            val watermarkOptions = listOf("Cyber Neon Badge", "Minimal Corner", "Hidden")
            val latencyOptions = listOf("Ultra-Low (WebRTC)", "Balanced HD", "Cinema Buffer")
            Dialog(onDismissRequest = { showPrivacyDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, TashanCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Privacy & Creator Studio Controls", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Private Account", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Only approved followers can view your videos", color = TextSecondary, fontSize = 12.sp)
                            }
                            Switch(
                                checked = currentUser.isPrivate,
                                onCheckedChange = { repository.updatePrivacy(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TashanCyan)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mutual Consent Direct Chat", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Require mutual follow before media DMs", color = TextSecondary, fontSize = 12.sp)
                            }
                            Switch(
                                checked = snackSettings.strictMutualConsentChat,
                                onCheckedChange = {
                                    repository.updateSnackVideoSettings(snackSettings.copy(strictMutualConsentChat = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TashanCyan)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Verified-Only Audio/Video Calls", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Block calls from unverified accounts", color = TextSecondary, fontSize = 12.sp)
                            }
                            Switch(
                                checked = snackSettings.allowCallsFromVerifiedOnly,
                                onCheckedChange = {
                                    repository.updateSnackVideoSettings(snackSettings.copy(allowCallsFromVerifiedOnly = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TashanCyan)
                            )
                        }
                        HorizontalDivider(color = BorderSubtle)
                        Text("Creator Watermark Style", color = TashanCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            watermarkOptions.forEach { style ->
                                val active = snackSettings.creatorWatermarkStyle == style
                                Surface(
                                    onClick = {
                                        repository.updateSnackVideoSettings(snackSettings.copy(creatorWatermarkStyle = style))
                                    },
                                    color = if (active) TashanCyan else BgElevated2,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = style.substringBefore(" "),
                                        color = if (active) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                        Text("Live Stream Latency Mode", color = TashanCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            latencyOptions.forEach { mode ->
                                val active = snackSettings.liveStreamLatencyMode == mode
                                Surface(
                                    onClick = {
                                        repository.updateSnackVideoSettings(snackSettings.copy(liveStreamLatencyMode = mode))
                                    },
                                    color = if (active) TashanPink else BgElevated2,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = mode.substringBefore(" "),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = { showPrivacyDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Privacy & Studio Rules", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2b. Push & Security Notification Matrix Dialog
        if (showNotificationsDialog) {
            Dialog(onDismissRequest = { showNotificationsDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, TashanCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Notification Control Matrix", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("Configure real-time push alerts for engagement, live rooms & OAuth security.", color = TextSecondary, fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Likes, Comments & Mentions", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Instant creator engagement notifications", color = TextSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = snackSettings.pushLikesAndComments,
                                onCheckedChange = {
                                    repository.updateSnackVideoSettings(snackSettings.copy(pushLikesAndComments = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TashanCyan)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live Stream & PK Battle Alerts", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Notify when followed creators go LIVE", color = TextSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = snackSettings.pushLiveStreamAlerts,
                                onCheckedChange = {
                                    repository.updateSnackVideoSettings(snackSettings.copy(pushLiveStreamAlerts = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TashanCyan)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("OAuth & OTP Security Alerts", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Alert on new Apple, Facebook, X or SMS login attempts", color = TextSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = snackSettings.pushOAuthSecurityAlerts,
                                onCheckedChange = {
                                    repository.updateSnackVideoSettings(snackSettings.copy(pushOAuthSecurityAlerts = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TashanPink)
                            )
                        }
                        Button(
                            onClick = { showNotificationsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2c. OAuth & Session Security Center Dialog
        if (showOAuthSessionDialog) {
            Dialog(onDismissRequest = { showOAuthSessionDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, TashanCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = TashanCyan)
                            Text("OAuth & Session Security", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Surface(
                            color = BgElevated2,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AccountDetailRow("Active Provider", currentUser.authProvider.uppercase())
                                AccountDetailRow("Session UID", currentUser.uid.take(14))
                                AccountDetailRow("Phone SMS OTP", if (currentUser.isPhoneVerified) "Verified ✅" else "Not Verified ⚠️")
                                AccountDetailRow("Email Status", if (currentUser.isEmailVerified) "Verified ✅" else "Pending ⚠️")
                                AccountDetailRow("Browser Redirect Mode", "Custom Tabs / Strict OAuth2")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Strict Browser OAuth Binding", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Block unverified local logins; require real browser token for Apple, FB & X", color = TextSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = snackSettings.oauthStrictSessionBinding,
                                onCheckedChange = {
                                    repository.updateSnackVideoSettings(snackSettings.copy(oauthStrictSessionBinding = it))
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TashanCyan)
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "All secondary device tokens revoked. Current session re-verified! 🛡️", Toast.LENGTH_LONG).show()
                            },
                            border = BorderStroke(1.dp, TashanPink),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.GppBad, contentDescription = null, tint = TashanPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Revoke Other OAuth Sessions", color = TashanPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showOAuthSessionDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close Security Center", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2d. Advanced Video Quality, Bitrate, CDN & Cache Engine Dialog
        if (showAdvancedVideoEngineDialog) {
            val codecs = listOf("H.264 (AVC)", "HEVC / H.265", "AV1 Ultra")
            val cdnRoutes = listOf(
                "Lahore-Karachi Edge CDN (Anti-Block)",
                "Singapore Ultra-Low Latency",
                "Dubai MENA Relay",
                "Frankfurt Global Edge"
            )
            Dialog(onDismissRequest = { showAdvancedVideoEngineDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, TashanTeal.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Speed, contentDescription = null, tint = TashanCyan)
                            Text("Video, Bitrate & CDN Engine", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Text(
                            text = "Adaptive Bitrate Ceiling: ${snackSettings.maxBitrateMbps} Mbps",
                            color = TashanCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Slider(
                            value = snackSettings.maxBitrateMbps.toFloat(),
                            onValueChange = {
                                repository.updateSnackVideoSettings(snackSettings.copy(maxBitrateMbps = it.toInt().coerceIn(4, 35)))
                            },
                            valueRange = 4f..35f,
                            colors = SliderDefaults.colors(thumbColor = TashanCyan, activeTrackColor = TashanCyan)
                        )
                        Text("Hardware Video Codec Pipeline", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            codecs.forEach { codec ->
                                val active = snackSettings.preferredVideoCodec == codec
                                Surface(
                                    onClick = {
                                        repository.updateSnackVideoSettings(snackSettings.copy(preferredVideoCodec = codec))
                                    },
                                    color = if (active) TashanCyan else BgElevated2,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = codec.substringBefore(" "),
                                        color = if (active) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                        Text("Active Multi-Region CDN Edge Node", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        cdnRoutes.forEach { route ->
                            val selected = snackSettings.activePakistanRoute == route
                            Surface(
                                onClick = {
                                    cdnPingLatencyMs = (12..29).random()
                                    repository.updateSnackVideoSettings(snackSettings.copy(activePakistanRoute = route))
                                },
                                color = if (selected) TashanTeal.copy(alpha = 0.2f) else BgElevated2,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (selected) TashanCyan else BorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(route, color = Color.White, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    if (selected) {
                                        Text("${cdnPingLatencyMs}ms ⚡", color = TashanCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    cdnPingLatencyMs = (11..24).random()
                                    Toast.makeText(context, "Edge CDN Ping: ${cdnPingLatencyMs}ms • Zero Packet Loss ⚡", Toast.LENGTH_SHORT).show()
                                },
                                border = BorderStroke(1.dp, TashanCyan),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ping CDN", color = TashanCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    repository.updateSnackVideoSettings(snackSettings.copy(cacheSizeMb = 0))
                                    Toast.makeText(context, "Purged Video, Pixabay FX & Audio Caches! 🧹", Toast.LENGTH_SHORT).show()
                                },
                                border = BorderStroke(1.dp, TashanPink),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Purge Cache (${snackSettings.cacheSizeMb}MB)", color = TashanPink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = { showAdvancedVideoEngineDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Apply Engine Settings", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. 4-Digit Security PIN Dialog
        if (showSecurityPinDialog) {
            Dialog(onDismissRequest = { showSecurityPinDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Wallet Security PIN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Enter a 4-digit PIN to secure your coins and cash withdrawals.", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinInput = it },
                            placeholder = { Text("4-digit PIN (e.g. 1234)", color = TextTertiary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TashanCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        if (pinMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(pinMessage ?: "", color = TashanCyan, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showSecurityPinDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    if (pinInput.length == 4) {
                                        repository.setWalletSecurityPin(pinInput) { success, msg ->
                                            pinMessage = msg
                                            if (success) {
                                                Toast.makeText(context, "Security PIN updated! 🔒", Toast.LENGTH_SHORT).show()
                                                showSecurityPinDialog = false
                                            }
                                        }
                                    } else {
                                        pinMessage = "Please enter exactly 4 digits"
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Set PIN", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Creator Milestones & Monetization Dialog
        if (showCreatorMilestonesDialog) {
            Dialog(onDismissRequest = { showCreatorMilestonesDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TashanYellow.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⭐", fontSize = 20.sp)
                            Text("Creator Monetization Hub", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Current Tier: ${currentUser.computedStarRating()}★ Star Creator (${currentUser.followersCount} Followers)",
                            color = TashanYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        MilestoneProgressBar(
                            label = "Followers Target (5K - 10K)",
                            current = currentUser.followersCount,
                            target = 5000
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MilestoneProgressBar(
                            label = "Total Video Views (500K)",
                            current = currentUser.totalViews,
                            target = 500000
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (currentUser.isMonetizationUnlocked())
                                "🎉 Gold Creator Monetization is UNLOCKED! Earning PKR & Coins enabled."
                            else
                                "Reach 5,000–10,000 followers and 500,000 video views to unlock live cash payouts.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                showCreatorMilestonesDialog = false
                                onNavigateToCreatorStudio()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TashanYellow),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Creator Studio", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Language Dialog
        if (showLanguageDialog) {
            val languages = listOf("English (US)", "Urdu (اردو)", "Punjabi (پنجابی)", "Sindhi (سنڌي)", "Pashto (پښتو)")
            Dialog(onDismissRequest = { showLanguageDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Select App Language", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        languages.forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedLanguage = lang
                                        showLanguageDialog = false
                                        Toast.makeText(context, "Language set to $lang", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang, color = Color.White, fontSize = 15.sp)
                                if (selectedLanguage == lang) {
                                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = TashanCyan, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Blocked Accounts Dialog
        if (showBlockedAccountsDialog) {
            Dialog(onDismissRequest = { showBlockedAccountsDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Blocked Accounts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        if (blockedList.isEmpty()) {
                            Text("No blocked accounts. Your chat is safe and clear.", color = TextSecondary, fontSize = 13.sp)
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                blockedList.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("@${item.username}", color = Color.White, fontSize = 14.sp)
                                        OutlinedButton(
                                            onClick = {
                                                chatManager.unblockUser(item.uid)
                                                Toast.makeText(context, "Unblocked user @${item.username}", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Unblock", color = TashanCyan, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showBlockedAccountsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 7. Report a Problem Dialog
        if (showReportProblemDialog) {
            Dialog(onDismissRequest = { showReportProblemDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Report a Problem", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Describe the issue you experienced. Our team will review it.", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = reportText,
                            onValueChange = { reportText = it },
                            placeholder = { Text("What went wrong?", color = TextTertiary) },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TashanCyan,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showReportProblemDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    if (reportText.isNotBlank()) {
                                        Toast.makeText(context, "Problem reported! Ticket #TASH-${(1000..9999).random()}", Toast.LENGTH_LONG).show()
                                        reportText = ""
                                        showReportProblemDialog = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Submit", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 8. Terms Dialog
        if (showTermsDialog) {
            Dialog(onDismissRequest = { showTermsDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Terms & Privacy Policy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tashan is committed to user safety and creator authenticity. All videos must comply with community guidelines. Bullying, spam, and copyright infringement are strictly prohibited. In-app wallet transactions are verified cryptographically via secure SHA-256 ledger signatures.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { showTermsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("I Agree", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 9. Logout Confirmation Dialog
        if (showLogoutConfirmDialog) {
            Dialog(onDismissRequest = { showLogoutConfirmDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Are you sure you want to log out of @${currentUser.username}?",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showLogoutConfirmDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    showLogoutConfirmDialog = false
                                    repository.logout()
                                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                    onBack()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = TashanPink),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        // 10. QR Code Dialog
        if (showQrCodeDialog) {
            var isScanningMode by remember { mutableStateOf(false) }
            Dialog(onDismissRequest = { showQrCodeDialog = false }) {
                Surface(
                    color = BgElevated,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TashanCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isScanningMode) "Scan QR Code" else "My QR Code",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        if (!isScanningMode) {
                            // QR Code View
                            Box(
                                modifier = Modifier
                                    .size(220.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCode2,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(150.dp)
                                    )
                                    Text(text = "TASHAN-ID", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("@${currentUser.username}", color = TashanCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { Toast.makeText(context, "Saved! 🖼️", Toast.LENGTH_SHORT).show() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = BgElevated2),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { Toast.makeText(context, "Sharing...", Toast.LENGTH_SHORT).show() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Share, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        } else {
                            // Scanner View with Animation
                            Box(
                                modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "scan")
                                val offset by infiniteTransition.animateFloat(
                                    initialValue = 0f, targetValue = 260f,
                                    animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                                    label = "scan_offset"
                                )
                                Box(modifier = Modifier.fillMaxWidth().height(2.dp).offset(y = (offset - 130).dp).background(TashanCyan))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.CenterFocusWeak, null, tint = TashanCyan.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Scanning...", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { isScanningMode = !isScanningMode },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3E)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isScanningMode) Icons.Filled.QrCode else Icons.Filled.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isScanningMode) "My Code" else "Scan", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    if (isScanningMode) {
                                        Toast.makeText(context, "Looking for QR code...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "QR Code saved to gallery! 🖼️", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isScanningMode) Icons.Filled.FlashOn else Icons.Filled.Download,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isScanningMode) "Flash" else "Save", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
    }
}
}


@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF8E8E93),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun SettingsListItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    titleColor: Color = Color.White,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == TashanPink) TashanPink else Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
        }
        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(TashanCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    color = TashanCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFF5E5E62),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = TextTertiary,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = TashanCyan
            )
        )
    }
}

@Composable
private fun AccountDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun MilestoneProgressBar(label: String, current: Int, target: Int) {
    val progress = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, fontSize = 12.sp)
            Text("$current / $target", color = TashanYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = TashanYellow,
            trackColor = BorderSubtle
        )
    }
}
