package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoRepository
import com.example.model.AdminSystemControl
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlPanelScreen(
    repository: VideoRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adminControl by repository.adminSystemControl.collectAsState()
    val allVideos by repository.videos.collectAsState()
    val currentUser by repository.currentUser.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var pkrRateInput by remember(adminControl) { mutableStateOf(adminControl.monetizationPkrPer1000Views.toString()) }
    var lockNoticeInput by remember(adminControl) { mutableStateOf(adminControl.giftingLockNoticeMessage) }
    var showBanDialogUser by remember { mutableStateOf<String?>(null) }
    var banReasonInput by remember { mutableStateOf("Violation of Community Guidelines") }

    val totalAppViews = remember(allVideos) { allVideos.sumOf { it.viewsCount.toLong() } }
    val calculatedDistributedPkr = remember(totalAppViews, adminControl.monetizationPkrPer1000Views) {
        (totalAppViews / 1000.0) * adminControl.monetizationPkrPer1000Views
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TashanCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "👑", fontSize = 18.sp)
                        }
                        Column {
                            Text(
                                text = "Admin System Control Center",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Full Application Master Governance",
                                color = TashanCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0E15))
            )
        },
        containerColor = Color(0xFF0D0E15),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Master Navigation Bar for Admin Sections
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF141622),
                contentColor = TashanCyan,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("💰 Payout Rules", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🔒 Gifting & Payment Lock", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("🛡️ Moderation (${allVideos.size} Videos)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("⚡ Maintenance & Health", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> AdminMonetizationRulesSection(
                        adminControl = adminControl,
                        pkrRateInput = pkrRateInput,
                        onRateChange = { pkrRateInput = it },
                        totalAppViews = totalAppViews,
                        calculatedDistributedPkr = calculatedDistributedPkr,
                        onSaveRate = {
                            val rate = pkrRateInput.toDoubleOrNull() ?: 10.0
                            repository.updateAdminControl(
                                adminControl.copy(monetizationPkrPer1000Views = rate)
                            )
                            Toast.makeText(context, "Monetization Rate Updated: PKR $rate per 1000 views! ✅", Toast.LENGTH_SHORT).show()
                        },
                        onToggleMonetization = { active ->
                            repository.updateAdminControl(adminControl.copy(isMonetizationSystemActive = active))
                            Toast.makeText(context, if (active) "Monetization Enabled Globally! 🟢" else "Monetization Disabled! 🔴", Toast.LENGTH_SHORT).show()
                        }
                    )

                    1 -> AdminGiftingLockSection(
                        adminControl = adminControl,
                        lockNoticeInput = lockNoticeInput,
                        onLockNoticeChange = { lockNoticeInput = it },
                        onToggleLock = { isLocked ->
                            repository.updateAdminControl(adminControl.copy(isGiftingAndPurchasesLocked = isLocked))
                            Toast.makeText(
                                context,
                                if (isLocked) "🔒 Gifting & Coin Purchases Locked for Trailer Mode!" else "🔓 Gifting & Purchases Unlocked!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onSaveNotice = {
                            repository.updateAdminControl(adminControl.copy(giftingLockNoticeMessage = lockNoticeInput))
                            Toast.makeText(context, "Lock Notice Message Updated! ✅", Toast.LENGTH_SHORT).show()
                        }
                    )

                    2 -> AdminModerationSection(
                        allVideos = allVideos,
                        bannedUids = adminControl.bannedUids,
                        onDeleteVideo = { videoId ->
                            repository.adminDeleteVideo(videoId)
                            Toast.makeText(context, "Video $videoId removed by Admin! 🗑️", Toast.LENGTH_SHORT).show()
                        },
                        onBanUser = { uid ->
                            showBanDialogUser = uid
                        },
                        onUnbanUser = { uid ->
                            repository.adminUnbanUser(uid)
                            Toast.makeText(context, "User $uid unbanned! 🟢", Toast.LENGTH_SHORT).show()
                        }
                    )

                    3 -> AdminMaintenanceSection(
                        adminControl = adminControl,
                        onToggleMaintenance = { maint ->
                            repository.updateAdminControl(adminControl.copy(globalMaintenanceMode = maint))
                            Toast.makeText(
                                context,
                                if (maint) "🚨 Maintenance Mode ACTIVE Globally!" else "🟢 Maintenance Mode Deactivated!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }

        // Ban User Confirmation Dialog
        showBanDialogUser?.let { targetUid ->
            AlertDialog(
                onDismissRequest = { showBanDialogUser = null },
                icon = { Text("🔨", fontSize = 32.sp) },
                title = { Text("Ban User Account ($targetUid)", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Provide ban reason for admin audit records:", color = TextSecondary, fontSize = 13.sp)
                        OutlinedTextField(
                            value = banReasonInput,
                            onValueChange = { banReasonInput = it },
                            label = { Text("Reason for Ban") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TashanCyan,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            repository.adminBanUser(targetUid)
                            showBanDialogUser = null
                            Toast.makeText(context, "User $targetUid has been banned! 🔨", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Confirm Ban", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBanDialogUser = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = Color(0xFF1E202E)
            )
        }
    }
}

@Composable
private fun AdminMonetizationRulesSection(
    adminControl: AdminSystemControl,
    pkrRateInput: String,
    onRateChange: (String) -> Unit,
    totalAppViews: Long,
    calculatedDistributedPkr: Double,
    onSaveRate: () -> Unit,
    onToggleMonetization: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Monetization Formula Highlight Card
        Surface(
            color = Color(0xFF181B2A),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, TashanCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📊", fontSize = 20.sp)
                        Text("Creator View Revenue Rule", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Switch(
                        checked = adminControl.isMonetizationSystemActive,
                        onCheckedChange = onToggleMonetization,
                        colors = SwitchDefaults.colors(checkedThumbColor = TashanCyan)
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Text(
                    text = "Current Rate: PKR ${adminControl.monetizationPkrPer1000Views} per 1,000 Views",
                    color = TashanCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                // Breakdown Scale Example
                Surface(
                    color = Color(0xFF0F111C),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("💡 Payout Scale Preview:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("• 1,000 Views ➔ PKR ${(1000 / 1000.0) * adminControl.monetizationPkrPer1000Views}", color = TextSecondary, fontSize = 12.sp)
                        Text("• 10,000 Views ➔ PKR ${(10000 / 1000.0) * adminControl.monetizationPkrPer1000Views} (Target Rate)", color = TashanGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("• 100,000 Views ➔ PKR ${(100000 / 1000.0) * adminControl.monetizationPkrPer1000Views}", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                // Rate Adjustment Input Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = pkrRateInput,
                        onValueChange = onRateChange,
                        label = { Text("PKR per 1,000 Views") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TashanCyan,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Button(
                        onClick = onSaveRate,
                        colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Rate", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Platform Earnings Distribution Stat
        Surface(
            color = Color(0xFF131522),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📈 Live Platform Analytics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Platform Views:", color = TextSecondary, fontSize = 13.sp)
                    Text("$totalAppViews views", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calculated Creator Payout:", color = TextSecondary, fontSize = 13.sp)
                    Text("PKR ${String.format("%.2f", calculatedDistributedPkr)}", color = TashanGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun AdminGiftingLockSection(
    adminControl: AdminSystemControl,
    lockNoticeInput: String,
    onLockNoticeChange: (String) -> Unit,
    onToggleLock: (Boolean) -> Unit,
    onSaveNotice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Lock Card for Trailer/Preview Mode
        Surface(
            color = if (adminControl.isGiftingAndPurchasesLocked) Color(0xFF2A1519) else Color(0xFF152A1D),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.5.dp,
                if (adminControl.isGiftingAndPurchasesLocked) Color(0xFFFF453A) else TashanGreen
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (adminControl.isGiftingAndPurchasesLocked) "🔒" else "🔓", fontSize = 24.sp)
                        Column {
                            Text(
                                text = if (adminControl.isGiftingAndPurchasesLocked) "Gifting & Purchases LOCKED" else "Gifting & Purchases UNLOCKED",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Trailer / Preview Mode Protection",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = adminControl.isGiftingAndPurchasesLocked,
                        onCheckedChange = onToggleLock,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF453A),
                            uncheckedThumbColor = TashanGreen
                        )
                    )
                }

                Text(
                    text = if (adminControl.isGiftingAndPurchasesLocked)
                        "⚠️ When locked, users can browse packages & gift items, but clicking 'Buy' or 'Send Gift' will show the lock popup stating payment gateway setup is pending."
                    else
                        "✅ Purchases unlocked for active payment gateway testing.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
        }

        // Custom Lock Notice Message Editor
        Surface(
            color = Color(0xFF161826),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("📝 Custom User Lock Notice", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = "This notice message will be displayed in dialogs when users attempt to purchase coins or send gifts during Trailer Mode:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = lockNoticeInput,
                    onValueChange = onLockNoticeChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TashanCyan,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Button(
                    onClick = onSaveNotice,
                    colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Lock Notice", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AdminModerationSection(
    allVideos: List<com.example.model.VideoItem>,
    bannedUids: List<String>,
    onDeleteVideo: (String) -> Unit,
    onBanUser: (String) -> Unit,
    onUnbanUser: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📹 Moderation Feed (${allVideos.size} Videos)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(allVideos, key = { it.id }) { video ->
                val isUserBanned = bannedUids.contains(video.uid)
                Surface(
                    color = Color(0xFF161826),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isUserBanned) Color.Red.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("@${video.username}", color = TashanCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (isUserBanned) {
                                    Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                                        Text("BANNED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text(video.caption.ifBlank { "No caption" }, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("👁️ ${video.viewsCount} views • ❤️ ${video.likesCount} likes", color = TextSecondary, fontSize = 11.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Ban/Unban User Button
                            IconButton(
                                onClick = {
                                    if (isUserBanned) onUnbanUser(video.uid) else onBanUser(video.uid)
                                }
                            ) {
                                Text(if (isUserBanned) "🟢" else "🔨", fontSize = 18.sp)
                            }

                            // Delete Video Button
                            IconButton(onClick = { onDeleteVideo(video.id) }) {
                                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMaintenanceSection(
    adminControl: AdminSystemControl,
    onToggleMaintenance: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = Color(0xFF161826),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("⚡ Global System Maintenance Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Pause video feeds & uploads during server updates", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = adminControl.globalMaintenanceMode,
                        onCheckedChange = onToggleMaintenance,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Red)
                    )
                }
            }
        }
    }
}
