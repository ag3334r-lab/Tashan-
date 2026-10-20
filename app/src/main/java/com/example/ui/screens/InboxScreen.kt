package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.RealtimeChatManager
import com.example.data.VideoRepository
import com.example.model.CallLogEntry
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.SnapchatUserSettings
import com.example.model.StatusUpdate
import com.example.model.StickerItem
import com.example.model.UserProfile

import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val TashanDarkBg = Color(0xFF090A0F)
private val TashanHeaderBg = Color(0xFF141722)
private val TashanCardBg = Color(0xFF161926)
private val TashanOutgoingBubble = Color(0xFF0052D4)
private val TashanIncomingBubble = Color(0xFF1C2030)
private val TashanReadBlue = Color(0xFF00F2FE)
private val TashanCyanAccent = Color(0xFF00F2FE)
private val TashanRoseAccent = Color(0xFFFF007F)
private val TashanGoldAccent = Color(0xFFFFD700)

// Aliases for clean backward compatibility
private val WhatsAppDarkBg = TashanDarkBg
private val WhatsAppHeaderBg = TashanHeaderBg
private val WhatsAppOutgoingBubble = TashanOutgoingBubble
private val WhatsAppIncomingBubble = TashanIncomingBubble
private val WhatsAppReadBlue = TashanReadBlue
private val WhatsAppEmeraldAccent = TashanCyanAccent
private val SnapYellowAccent = TashanRoseAccent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InboxScreen(
    repository: VideoRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatManager = remember { RealtimeChatManager.getInstance() }
    val conversations by chatManager.conversations.collectAsState()
    val allMessagesMap by chatManager.messages.collectAsState()
    val statusUpdates by chatManager.statusUpdates.collectAsState()
    val callLogs by chatManager.callLogs.collectAsState()
    val discoverableUsers by chatManager.discoverableUsers.collectAsState()
    val feedVideos by repository.videos.collectAsState()
    val currentUserProfile by repository.currentUser.collectAsState()
    val snapSettings by repository.snapchatSettings.collectAsState()
    val adminControl by repository.adminSystemControl.collectAsState()
    val stickers by repository.stickers.collectAsState()

    // Merge live Firestore users + real creators from feed so New Chat (+) always has real creators to message
    val availableRealUsers = remember(discoverableUsers, feedVideos, currentUserProfile) {
        val fromFeed = feedVideos.map { vid ->
            UserProfile(
                uid = vid.uid,
                username = vid.username,
                displayName = vid.username.replace("_", " ").replaceFirstChar { it.uppercase() },
                bio = vid.caption,
                avatarUrl = vid.userAvatarUrl
            )
        }
        (discoverableUsers + fromFeed)
            .filter { it.username.isNotBlank() && it.uid != currentUserProfile.uid }
            .distinctBy { it.username.lowercase() }
    }

    var selectedConversationId by remember { mutableStateOf<String?>(null) }
    val selectedConversation = remember(conversations, selectedConversationId) {
        conversations.find { it.id == selectedConversationId }
    }

    LaunchedEffect(selectedConversationId) {
        selectedConversationId?.let { convId ->
            chatManager.attachMessagesListener(convId)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var primaryHubTab by remember { mutableStateOf("Chats") } // "Chats", "Updates", "Calls"
    var chatFilterChip by remember { mutableStateOf("All") } // "All", "Unread", "Streaks 🔥", "Pinned"

    var showSnapchatSettings by remember { mutableStateOf(false) }
    var showNewChatDirectoryDialog by remember { mutableStateOf(false) }

    // Active Call Overlay State
    var activeCallConversation by remember { mutableStateOf<Conversation?>(null) }
    var isActiveCallVideo by remember { mutableStateOf(false) }

    // Status Viewer & Creator State
    var viewingStatus by remember { mutableStateOf<StatusUpdate?>(null) }
    var showCreateStatusDialog by remember { mutableStateOf(false) }

    val totalUnreadChats = remember(conversations) {
        conversations.count { it.unreadCount > 0 }
    }

    val filteredConversations = remember(conversations, searchQuery, chatFilterChip) {
        conversations.filter { conv ->
            val matchesQuery = searchQuery.isBlank() ||
                conv.otherUsername.contains(searchQuery, ignoreCase = true) ||
                conv.otherDisplayName.contains(searchQuery, ignoreCase = true) ||
                conv.lastMessage.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (chatFilterChip) {
                "Unread" -> conv.unreadCount > 0
                "Streaks 🔥" -> conv.streakCount > 0
                "Pinned" -> conv.isPinned
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val isUserVerified = currentUserProfile.isLoggedIn &&
        (currentUserProfile.isEmailVerified || currentUserProfile.isPhoneVerified || currentUserProfile.authProvider.equals("google", ignoreCase = true))

    if (!isUserVerified) {
        TashanVerificationGate(
            onOpenAuth = { reason -> repository.openAuthModal(reason) },
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WhatsAppDarkBg)
            .statusBarsPadding()
    ) {
        if (selectedConversation == null) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top WhatsApp + Snapchat Header Bar
                Surface(
                    color = WhatsAppHeaderBg,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // User's 3D Bitmoji Badge (tappable for Snapchat Settings)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(snapSettings.bitmojiBgColorHex))
                                        .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                                        .clickable { showSnapchatSettings = true }
                                        .testTag("inbox_bitmoji_badge"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(snapSettings.bitmojiPoseEmoji, fontSize = 18.sp)
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Tashan Chat",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 19.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = WhatsAppEmeraldAccent.copy(alpha = 0.18f),
                                            border = BorderStroke(1.dp, WhatsAppEmeraldAccent.copy(alpha = 0.6f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Lock,
                                                    contentDescription = null,
                                                    tint = WhatsAppEmeraldAccent,
                                                    modifier = Modifier.size(9.dp)
                                                )
                                                Text(
                                                    text = "LIVE FIRESTORE",
                                                    color = WhatsAppEmeraldAccent,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (snapSettings.isGhostMode) "👻 Ghost Mode (${snapSettings.ghostModeDuration}) • 💰 ${currentUserProfile.coins} Coins"
                                        else "📍 ${snapSettings.locationSharingMode} • 💰 ${currentUserProfile.coins} Coins",
                                        color = if (snapSettings.isGhostMode) SnapYellowAccent else Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Snapchat Settings Button
                                Surface(
                                    onClick = { showSnapchatSettings = true },
                                    shape = RoundedCornerShape(10.dp),
                                    color = SnapYellowAccent.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, SnapYellowAccent.copy(alpha = 0.6f)),
                                    modifier = Modifier.testTag("open_snapchat_settings_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("👻", fontSize = 13.sp)
                                        Text(
                                            text = if (snapSettings.isGhostMode) "Ghost" else "Snap",
                                            color = SnapYellowAccent,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showNewChatDirectoryDialog = true },
                                    modifier = Modifier.size(36.dp).testTag("new_chat_directory_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PersonAddAlt1,
                                        contentDescription = "New Chat",
                                        tint = WhatsAppEmeraldAccent
                                    )
                                }

                                IconButton(
                                    onClick = { isSearchOpen = !isSearchOpen },
                                    modifier = Modifier.size(36.dp).testTag("whatsapp_search_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isSearchOpen) Icons.Filled.Close else Icons.Filled.Search,
                                        contentDescription = "Search Chats",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = isSearchOpen) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search live chats or creators...", color = TextSecondary, fontSize = 13.sp) },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = WhatsAppEmeraldAccent) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextSecondary)
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WhatsAppEmeraldAccent,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedContainerColor = Color(0xFF121B22),
                                    unfocusedContainerColor = Color(0xFF121B22),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }

                        // Tashan 3-Tab Switcher: Chats | Calls | Blocked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("Chats", "Calls", "Blocked").forEach { tab ->
                                val isSelected = primaryHubTab == tab
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { primaryHubTab = tab }
                                        .padding(top = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = tab,
                                            color = if (isSelected) WhatsAppEmeraldAccent else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        if (tab == "Chats" && totalUnreadChats > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(WhatsAppEmeraldAccent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = totalUnreadChats.toString(),
                                                    color = Color.Black,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .background(if (isSelected) WhatsAppEmeraldAccent else Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }

                when (primaryHubTab) {
                    "Chats" -> {
                        // Filter Chips + Quick Live Creators Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("All", "Unread", "Streaks 🔥", "Pinned").forEach { chip ->
                                    val selected = chatFilterChip == chip
                                    Surface(
                                        onClick = { chatFilterChip = chip },
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (selected) WhatsAppEmeraldAccent.copy(alpha = 0.22f) else Color(0xFF1F2C34),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) WhatsAppEmeraldAccent else Color.Transparent
                                        )
                                    ) {
                                        Text(
                                            text = chip,
                                            color = if (selected) WhatsAppEmeraldAccent else TextSecondary,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = { showNewChatDirectoryDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = WhatsAppEmeraldAccent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                    Text("New Chat", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }

                        if (filteredConversations.isEmpty()) {
                            // Live Creator Discovery Empty State (Zero Demo — Start Real Firestore Chat)
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF162229)),
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(1.dp, WhatsAppEmeraldAccent.copy(alpha = 0.4f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Filled.Forum, contentDescription = null, tint = WhatsAppEmeraldAccent)
                                                Text(
                                                    text = "100% Live Real-Time Chat Directory",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp
                                                )
                                            }
                                            Text(
                                                text = "No fake demo conversations are pre-filled. Tap any verified creator below or tap 'New Chat' to start a live Cloud Firestore conversation with Snap Streaks & E2E encryption.",
                                                color = Color.White.copy(alpha = 0.72f),
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp
                                            )
                                        }
                                    }
                                }

                                item {
                                    Text(
                                        text = "ACTIVE CREATORS ON TASHAN",
                                        color = WhatsAppEmeraldAccent,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp
                                    )
                                }

                                items(availableRealUsers, key = { it.username }) { user ->
                                    Surface(
                                        onClick = {
                                            val conv = chatManager.startOrGetConversationWithUser(
                                                currentUser = currentUserProfile,
                                                targetUser = user,
                                                defaultRetentionPolicy = snapSettings.messageRetentionPolicy
                                            )
                                            selectedConversationId = conv.id
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF162229),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .background(WhatsAppOutgoingBubble),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (user.avatarUrl.isNotBlank()) {
                                                        AsyncImage(
                                                            model = user.avatarUrl,
                                                            contentDescription = user.username,
                                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Text(
                                                            text = user.username.take(1).uppercase(),
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 18.sp
                                                        )
                                                    }
                                                }
                                                Column {
                                                    Text(
                                                        text = user.displayName.ifBlank { user.username },
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    )
                                                    Text(
                                                        text = "@${user.username}",
                                                        color = WhatsAppEmeraldAccent,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = WhatsAppEmeraldAccent
                                            ) {
                                                Text(
                                                    text = "Message 💬",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(filteredConversations, key = { it.id }) { conv ->
                                    WhatsAppConversationRow(
                                        conversation = conv,
                                        onClick = {
                                            chatManager.markConversationRead(conv.id)
                                            chatManager.attachMessagesListener(conv.id)
                                            selectedConversationId = conv.id
                                        },
                                        onTogglePin = { chatManager.togglePinConversation(conv.id) },
                                        onToggleMute = { chatManager.toggleMuteConversation(conv.id) },
                                        onRestoreStreak = {
                                            chatManager.restoreSnapStreak(conv.id)
                                            Toast.makeText(context, "🔥 Snap Streak Restored with @${conv.otherUsername}!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    "Blocked" -> {
                        val blockedList by chatManager.blockedUsers.collectAsState()
                        TashanBlockedUsersTab(
                            blockedUsers = blockedList,
                            onUnblock = { item ->
                                chatManager.unblockUser(item.uid)
                                Toast.makeText(context, "@${item.username} unblocked", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    "Calls" -> {
                        WhatsAppCallsTab(
                            callLogs = callLogs,
                            conversations = conversations,
                            onStartCall = { conv, isVideo ->
                                activeCallConversation = conv
                                isActiveCallVideo = isVideo
                            }
                        )
                    }
                }
            }
        } else {
            val activeMessages = allMessagesMap[selectedConversation.id] ?: emptyList()
            WhatsAppChatThreadView(
                conversation = selectedConversation,
                messages = activeMessages,
                allConversations = conversations,
                currentUser = currentUserProfile,
                snapSettings = snapSettings,
                stickers = stickers,
                repository = repository,
                chatManager = chatManager,
                onBack = {
                    // Enforce Snapchat Immediate Auto-Delete when leaving chat if enabled
                    chatManager.purgeImmediateMessagesOnExit(selectedConversation.id)
                    selectedConversationId = null
                },
                onStartAudioCall = {
                    activeCallConversation = selectedConversation
                    isActiveCallVideo = false
                },
                onStartVideoCall = {
                    activeCallConversation = selectedConversation
                    isActiveCallVideo = true
                },
                onOpenSnapSettings = { showSnapchatSettings = true }
            )
        }

        // Full-Screen Audio / Video Call Overlay
        activeCallConversation?.let { callConv ->
            WhatsAppCallOverlayScreen(
                conversation = callConv,
                isVideoCall = isActiveCallVideo,
                onEndCall = { durationFormatted ->
                    chatManager.logCall(callConv, isActiveCallVideo, durationFormatted)
                    activeCallConversation = null
                }
            )
        }

        // New Chat (+) Creator Directory Modal
        if (showNewChatDirectoryDialog) {
            NewChatCreatorDirectoryDialog(
                availableUsers = availableRealUsers,
                onDismiss = { showNewChatDirectoryDialog = false },
                onSelectUser = { targetUser ->
                    showNewChatDirectoryDialog = false
                    val conv = chatManager.startOrGetConversationWithUser(
                        currentUser = currentUserProfile,
                        targetUser = targetUser,
                        defaultRetentionPolicy = snapSettings.messageRetentionPolicy
                    )
                    selectedConversationId = conv.id
                }
            )
        }


    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WhatsAppConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onRestoreStreak: () -> Unit
) {
    var showRowMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showRowMenu = true }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Online Indicator
            Box(modifier = Modifier.size(54.dp)) {
                if (conversation.otherAvatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = conversation.otherAvatarUrl,
                        contentDescription = conversation.otherUsername,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(WhatsAppOutgoingBubble, Color(0xFF00A884))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (conversation.otherDisplayName.ifBlank { conversation.otherUsername })
                                .take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
                if (conversation.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(WhatsAppEmeraldAccent)
                            .border(2.dp, WhatsAppDarkBg, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = conversation.otherDisplayName.ifBlank { conversation.otherUsername },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (conversation.streakCount > 0) {
                            Surface(
                                color = SnapYellowAccent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "🔥 ${conversation.streakCount}",
                                    color = SnapYellowAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (conversation.retentionPolicy == "IMMEDIATE") {
                            Text("👻", fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = formatWhatsAppTime(conversation.timestamp),
                        color = if (conversation.unreadCount > 0) WhatsAppEmeraldAccent else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (conversation.isSentByMe) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = "Read",
                                tint = WhatsAppReadBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = conversation.lastMessage,
                            color = if (conversation.unreadCount > 0) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (conversation.isPinned) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Pinned",
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        if (conversation.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppEmeraldAccent)
                                    .padding(horizontal = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = conversation.unreadCount.toString(),
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showRowMenu,
            onDismissRequest = { showRowMenu = false },
            modifier = Modifier.background(Color(0xFF1F2C34))
        ) {
            DropdownMenuItem(
                text = { Text(if (conversation.isPinned) "Unpin Chat" else "Pin Chat", color = Color.White) },
                leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null, tint = WhatsAppEmeraldAccent) },
                onClick = {
                    showRowMenu = false
                    onTogglePin()
                }
            )
            DropdownMenuItem(
                text = { Text("🔥 Restore / Boost Snap Streak", color = SnapYellowAccent) },
                leadingIcon = { Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = SnapYellowAccent) },
                onClick = {
                    showRowMenu = false
                    onRestoreStreak()
                }
            )
            DropdownMenuItem(
                text = { Text(if (conversation.isMuted) "Unmute Notifications" else "Mute Notifications", color = Color.White) },
                leadingIcon = { Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = TextSecondary) },
                onClick = {
                    showRowMenu = false
                    onToggleMute()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WhatsAppChatThreadView(
    conversation: Conversation,
    messages: List<ChatMessage>,
    allConversations: List<Conversation>,
    currentUser: UserProfile,
    snapSettings: SnapchatUserSettings,
    stickers: List<StickerItem>,
    repository: VideoRepository,
    chatManager: RealtimeChatManager,
    onBack: () -> Unit,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onOpenSnapSettings: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val adminControl by repository.adminSystemControl.collectAsState()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessageForActions by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }
    var showGiftSheet by remember { mutableStateOf(false) }
    var showCreateCustomStickerDialog by remember { mutableStateOf(false) }
    var insufficientCoinsPrompt by remember { mutableStateOf<String?>(null) }

    // Voice Note Recording State
    var isRecordingVoiceNote by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecordingVoiceNote) {
        if (isRecordingVoiceNote) {
            recordingSeconds = 0
            while (isRecordingVoiceNote) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Real Android Photo/Video Picker
    val visualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            chatManager.sendMessage(
                conversationId = conversation.id,
                sender = currentUser,
                text = "Shared Media from Gallery",
                mediaUrl = uri.toString(),
                mediaType = "IMAGE",
                replyToMessage = replyingToMessage
            )
            replyingToMessage = null
        }
    }

    // Real Android Document Picker
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Document_${System.currentTimeMillis() % 1000}.pdf"
            chatManager.sendMessage(
                conversationId = conversation.id,
                sender = currentUser,
                text = "Shared Document",
                mediaUrl = uri.toString(),
                mediaType = "DOCUMENT",
                documentName = fileName,
                documentSizeKb = 420,
                replyToMessage = replyingToMessage
            )
            replyingToMessage = null
        }
    }

    val isPending = conversation.status == "PENDING"
    val isInitiator = conversation.initiatorUid == currentUser.uid
    val isBlocked = conversation.isBlocked
    var showChatMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TashanDarkBg)
    ) {
        // Active Chat Header with Snap Streak & Auto-Delete Retention Toggle
        Surface(
            color = WhatsAppHeaderBg,
            tonalElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp).testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(WhatsAppOutgoingBubble),
                        contentAlignment = Alignment.Center
                    ) {
                        if (conversation.otherAvatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = conversation.otherAvatarUrl,
                                contentDescription = conversation.otherUsername,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (conversation.otherDisplayName.ifBlank { conversation.otherUsername }).take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = conversation.otherDisplayName.ifBlank { conversation.otherUsername },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                onClick = {
                                    chatManager.restoreSnapStreak(conversation.id)
                                    Toast.makeText(context, "🔥 Streak Boosted!", Toast.LENGTH_SHORT).show()
                                },
                                color = SnapYellowAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "🔥 ${conversation.streakCount.coerceAtLeast(1)}",
                                    color = SnapYellowAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (conversation.isOnline) "Online • Live Firestore Sync" else conversation.lastSeenText,
                            color = WhatsAppEmeraldAccent,
                            fontSize = 11.sp
                        )
                    }

                    val canCall = conversation.canPlaceCall(currentUser.uid)
                    IconButton(
                        onClick = {
                            if (canCall) {
                                onStartVideoCall()
                            } else {
                                Toast.makeText(context, "Cannot video call until @${conversation.otherUsername} accepts your chat request!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(38.dp).testTag("chat_video_call_btn")
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = "Video Call", tint = if (canCall) Color.White else Color.White.copy(alpha = 0.35f))
                    }
                    IconButton(
                        onClick = {
                            if (canCall) {
                                onStartAudioCall()
                            } else {
                                Toast.makeText(context, "Cannot voice call until @${conversation.otherUsername} accepts your chat request!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(38.dp).testTag("chat_audio_call_btn")
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = "Voice Call", tint = if (canCall) Color.White else Color.White.copy(alpha = 0.35f))
                    }

                    Box {
                        var showThreadMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showThreadMenu = true }, modifier = Modifier.size(38.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showThreadMenu,
                            onDismissRequest = { showThreadMenu = false },
                            modifier = Modifier.background(Color(0xFF1F2C34))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Block @${conversation.otherUsername}", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null, tint = Color(0xFFFF5252)) },
                                onClick = {
                                    showThreadMenu = false
                                    chatManager.blockUser(conversation, currentUser.uid)
                                    Toast.makeText(context, "🚫 Blocked @${conversation.otherUsername}", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Snapchat Settings", color = Color.White) },
                                leadingIcon = { Text("👻", fontSize = 16.sp) },
                                onClick = {
                                    showThreadMenu = false
                                    onOpenSnapSettings()
                                }
                            )
                        }
                    }
                }

                // Snapchat Retention Policy Banner Bar (Tappable to toggle Immediate vs 24 Hours)
                val isImmediate = conversation.retentionPolicy == "IMMEDIATE"
                Surface(
                    onClick = {
                        val nextPolicy = if (isImmediate) "24_HOURS" else "IMMEDIATE"
                        chatManager.updateConversationRetentionPolicy(conversation.id, nextPolicy)
                        Toast.makeText(
                            context,
                            if (nextPolicy == "IMMEDIATE") "👻 Set to Delete Immediately After Viewing"
                            else "🕒 Set to Keep Messages for 24 Hours",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    color = if (isImmediate) SnapYellowAccent.copy(alpha = 0.14f) else Color(0xFF142229),
                    modifier = Modifier.fillMaxWidth().testTag("chat_retention_policy_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoDelete,
                                contentDescription = null,
                                tint = if (isImmediate) SnapYellowAccent else WhatsAppEmeraldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isImmediate) "👻 Snapchat Mode: Messages Delete Immediately After Viewing"
                                else "🕒 Snapchat Mode: Messages Expire 24 Hours After Viewing",
                                color = if (isImmediate) SnapYellowAccent else Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "CHANGE",
                            color = WhatsAppEmeraldAccent,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        color = Color(0xFF182229),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, WhatsAppEmeraldAccent.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "🔒 Messages & calls are end-to-end encrypted and synced in real-time via Cloud Firestore.",
                            color = Color(0xFFFFD279),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val isMe = msg.senderUid == currentUser.uid || msg.senderUsername == currentUser.username
                WhatsAppMessageBubble(
                    message = msg,
                    isSentByMe = isMe,
                    onLongPress = { selectedMessageForActions = msg },
                    onQuickReact = { emoji ->
                        chatManager.reactToMessage(conversation.id, msg.id, emoji)
                    },
                    onReplyClick = { replyingToMessage = msg }
                )
            }
        }

        // Reply Banner if Replying
        AnimatedVisibility(visible = replyingToMessage != null) {
            replyingToMessage?.let { replyMsg ->
                Surface(
                    color = Color(0xFF18252D),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to @${replyMsg.senderUsername}",
                                color = WhatsAppEmeraldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = replyMsg.text,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { replyingToMessage = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel Reply", tint = TextSecondary)
                        }
                    }
                }
            }
        }

        // Sticker Pack Drawer (100% Live Firestore-Synced Stickers + Custom Sticker Creator)
        AnimatedVisibility(visible = showStickerSheet) {
            Surface(
                color = Color(0xFF141E24),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ Live Firestore Stickers (${stickers.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = { showCreateCustomStickerDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                color = WhatsAppEmeraldAccent
                            ) {
                                Text(
                                    text = "+ Create Sticker",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            IconButton(onClick = { showStickerSheet = false }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(stickers, key = { it.id }) { stk ->
                            Surface(
                                onClick = {
                                    chatManager.sendMessage(
                                        conversationId = conversation.id,
                                        sender = currentUser,
                                        text = stk.emojiOrText,
                                        mediaUrl = stk.imageUrl,
                                        mediaType = "STICKER",
                                        replyToMessage = replyingToMessage
                                    )
                                    replyingToMessage = null
                                    showStickerSheet = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(stk.bgHex).copy(alpha = 0.25f),
                                border = BorderStroke(1.5.dp, Color(stk.bgHex))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stk.emojiOrText,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = stk.title,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Attachment Grid Tray
        AnimatedVisibility(visible = showAttachmentSheet) {
            Surface(
                color = Color(0xFF18252D),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachmentOptionItem(
                        icon = Icons.Filled.Description,
                        label = "Document",
                        color = Color(0xFF7F66FF),
                        onClick = {
                            showAttachmentSheet = false
                            documentLauncher.launch("*/*")
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Filled.PhotoLibrary,
                        label = "Gallery",
                        color = Color(0xFFC861FA),
                        onClick = {
                            showAttachmentSheet = false
                            visualMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Filled.AutoAwesome,
                        label = "Stickers",
                        color = SnapYellowAccent,
                        onClick = {
                            showAttachmentSheet = false
                            showStickerSheet = true
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Filled.LocationOn,
                        label = "Location",
                        color = WhatsAppEmeraldAccent,
                        onClick = {
                            showAttachmentSheet = false
                            val locLabel = if (snapSettings.isGhostMode) "Ghost Location Pin (Encrypted)" else "Live GPS Pin • Pakistan"
                            chatManager.sendMessage(
                                conversationId = conversation.id,
                                sender = currentUser,
                                text = locLabel,
                                mediaType = "LOCATION",
                                locationName = locLabel,
                                replyToMessage = replyingToMessage
                            )
                            replyingToMessage = null
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Filled.CardGiftcard,
                        label = "Coin Gift",
                        color = Color(0xFFFF2A54),
                        onClick = {
                            showAttachmentSheet = false
                            showGiftSheet = true
                        }
                    )
                }
            }
        }

        // Real Coin Gift Sheet (Deducts from Real Firestore Wallet Balance)
        AnimatedVisibility(visible = showGiftSheet) {
            val gifts = listOf(
                Triple("🌹 Rose", 10, "Rose Gift"),
                Triple("☕ Karak Chai", 25, "Karak Chai"),
                Triple("👑 Golden Crown", 100, "Golden Crown"),
                Triple("🦁 Tashan Lion", 500, "Tashan Lion"),
                Triple("🚀 Cyber Jet", 1000, "Cyber Jet")
            )
            Surface(
                color = Color(0xFF141E24),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎁 Send Real Coin Gift (Wallet: ${currentUser.coins} Coins)",
                            color = SnapYellowAccent,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        IconButton(onClick = { showGiftSheet = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(gifts) { (emojiLabel, costCoins, name) ->
                            Surface(
                                onClick = {
                                    if (adminControl.isGiftingAndPurchasesLocked) {
                                        Toast.makeText(context, adminControl.giftingLockNoticeMessage, Toast.LENGTH_LONG).show()
                                        return@Surface
                                    }
                                    repository.deductCoinsForGift(
                                        giftCoins = costCoins,
                                        giftName = name,
                                        recipientUsername = conversation.otherUsername
                                    ) { success, message ->
                                        if (success) {
                                            chatManager.sendMessage(
                                                conversationId = conversation.id,
                                                sender = currentUser,
                                                text = "Sent $name",
                                                mediaType = "GIFT",
                                                giftEmoji = emojiLabel,
                                                giftCoins = costCoins,
                                                replyToMessage = replyingToMessage
                                            )
                                            replyingToMessage = null
                                            showGiftSheet = false
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        } else {
                                            insufficientCoinsPrompt = message
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1F2C34),
                                border = BorderStroke(1.dp, SnapYellowAccent.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(emojiLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("💰 $costCoins Coins", color = SnapYellowAccent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Composer Bar with Strict Mutual-Consent & Request Protection
        val canSend = conversation.canSendDirectMessage(currentUser.uid)
        val isBlocked = conversation.isBlocked
        val isPending = conversation.status == "PENDING"
        val isInitiator = conversation.initiatorUid == currentUser.uid

        Surface(
            color = WhatsAppHeaderBg,
            tonalElevation = 6.dp
        ) {
            when {
                isBlocked -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🚫 You cannot send messages or place calls because this conversation is blocked.",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                chatManager.unblockUser(conversation.otherUserUid.ifBlank { conversation.otherUsername })
                                Toast.makeText(context, "@${conversation.otherUsername} unblocked", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppEmeraldAccent)
                        ) {
                            Text("Unblock User", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                isPending && isInitiator -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("⏳", fontSize = 16.sp)
                            Text(
                                text = "Chat Request Sent",
                                color = SnapYellowAccent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "You cannot send additional messages or make audio/video calls until @${conversation.otherUsername} accepts your request or replies first.",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = {
                                chatManager.triggerRecipientAcceptAndReply(conversation.id)
                                Toast.makeText(context, "✨ @${conversation.otherUsername} accepted and replied!", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, WhatsAppEmeraldAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppEmeraldAccent)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate @${conversation.otherUsername} Replying & Accepting", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                isPending && !isInitiator && conversation.initiatorUid.isNotBlank() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📩 Message Request from @${conversation.otherUsername}",
                            color = WhatsAppEmeraldAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Accept this request to unlock free-flow messaging, voice notes, Pakistani stickers, and HD calling.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    chatManager.acceptMessageRequest(conversation.id)
                                    Toast.makeText(context, "✅ Request Accepted! Chat Unlocked", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppEmeraldAccent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Accept ✅", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = onBack,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Decline ❌", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    chatManager.blockUser(conversation, currentUser.uid)
                                    Toast.makeText(context, "🚫 User Blocked", Toast.LENGTH_SHORT).show()
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Block 🚫", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                else -> {
                    if (isRecordingVoiceNote) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                )
                                Text(
                                    text = "Recording Voice 0:${recordingSeconds.toString().padStart(2, '0')}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { isRecordingVoiceNote = false }) {
                                    Text("Cancel", color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        val dur = recordingSeconds.coerceAtLeast(1)
                                        isRecordingVoiceNote = false
                                        chatManager.sendMessage(
                                            conversationId = conversation.id,
                                            sender = currentUser,
                                            text = "Voice Note",
                                            mediaType = "VOICE",
                                            audioDurationSec = dur,
                                            audioWaveform = listOf(0.3f, 0.7f, 0.9f, 0.4f, 0.85f, 0.6f, 0.95f, 0.5f, 0.4f, 0.8f),
                                            replyToMessage = replyingToMessage
                                        )
                                        replyingToMessage = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppEmeraldAccent)
                                ) {
                                    Text("Send 🎤", color = Color.Black, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    showStickerSheet = !showStickerSheet
                                    showAttachmentSheet = false
                                },
                                modifier = Modifier.size(38.dp).testTag("chat_sticker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEmotions,
                                    contentDescription = "Stickers",
                                    tint = if (showStickerSheet) SnapYellowAccent else TextSecondary
                                )
                            }

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Message...", color = TextSecondary, fontSize = 14.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WhatsAppEmeraldAccent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color(0xFF121B22),
                                    unfocusedContainerColor = Color(0xFF121B22),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_message_input"),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (inputText.isNotBlank()) {
                                            chatManager.sendMessage(
                                                conversationId = conversation.id,
                                                sender = currentUser,
                                                text = inputText.trim(),
                                                replyToMessage = replyingToMessage
                                            )
                                            inputText = ""
                                            replyingToMessage = null
                                        }
                                    }
                                )
                            )

                            IconButton(
                                onClick = {
                                    showAttachmentSheet = !showAttachmentSheet
                                    showStickerSheet = false
                                },
                                modifier = Modifier.size(38.dp).testTag("chat_attach_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = "Attach",
                                    tint = if (showAttachmentSheet) WhatsAppEmeraldAccent else TextSecondary
                                )
                            }

                            IconButton(
                                onClick = { showGiftSheet = !showGiftSheet },
                                modifier = Modifier.size(38.dp).testTag("chat_gift_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CardGiftcard,
                                    contentDescription = "Send Gift",
                                    tint = SnapYellowAccent
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            FloatingActionButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        chatManager.sendMessage(
                                            conversationId = conversation.id,
                                            sender = currentUser,
                                            text = inputText.trim(),
                                            replyToMessage = replyingToMessage
                                        )
                                        inputText = ""
                                        replyingToMessage = null
                                    } else {
                                        isRecordingVoiceNote = true
                                    }
                                },
                                containerColor = WhatsAppEmeraldAccent,
                                contentColor = Color.Black,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp).testTag("chat_send_or_voice_button")
                            ) {
                                Icon(
                                    imageVector = if (inputText.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Filled.Mic,
                                    contentDescription = if (inputText.isNotBlank()) "Send" else "Record Voice",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Message Actions Sheet (React, Reply, Forward, Star, Delete for Everyone)
    selectedMessageForActions?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessageForActions = null },
            containerColor = Color(0xFF1F2C34),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("❤️", "😂", "😮", "😢", "🙏", "🔥").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    chatManager.reactToMessage(conversation.id, msg.id, emoji)
                                    selectedMessageForActions = null
                                }
                                .padding(6.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            replyingToMessage = msg
                            selectedMessageForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = WhatsAppEmeraldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reply to Message", color = Color.White)
                    }
                    TextButton(
                        onClick = {
                            forwardingMessage = msg
                            selectedMessageForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = null, tint = WhatsAppReadBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Forward Message", color = Color.White)
                    }
                    TextButton(
                        onClick = {
                            chatManager.toggleStarMessage(conversation.id, msg.id)
                            selectedMessageForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = SnapYellowAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (msg.isStarred) "Unstar Message" else "Star Message (Keep in Chat)", color = Color.White)
                    }
                    TextButton(
                        onClick = {
                            chatManager.deleteMessage(conversation.id, msg.id, deleteForEveryone = true)
                            selectedMessageForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = Color(0xFFFF5252))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete for Everyone", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            chatManager.deleteMessage(conversation.id, msg.id, deleteForEveryone = false)
                            selectedMessageForActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFFB74D))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete for Me", color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForActions = null }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // Forward Message Dialog
    forwardingMessage?.let { msgToForward ->
        AlertDialog(
            onDismissRequest = { forwardingMessage = null },
            containerColor = Color(0xFF1F2C34),
            title = { Text("Forward Message To...", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allConversations.forEach { targetConv ->
                        Surface(
                            onClick = {
                                chatManager.forwardMessage(msgToForward, targetConv.id, currentUser)
                                forwardingMessage = null
                                Toast.makeText(context, "Forwarded to @${targetConv.otherUsername}", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF121B22),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${targetConv.otherDisplayName.ifBlank { targetConv.otherUsername }} (@${targetConv.otherUsername})",
                                color = Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { forwardingMessage = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Create Custom Sticker Dialog
    if (showCreateCustomStickerDialog) {
        var stickerTitle by remember { mutableStateOf("") }
        var stickerEmojiText by remember { mutableStateOf("🔥 VIP") }
        AlertDialog(
            onDismissRequest = { showCreateCustomStickerDialog = false },
            containerColor = Color(0xFF1F2C34),
            title = { Text("✨ Create Custom Firestore Sticker", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stickerEmojiText,
                        onValueChange = { stickerEmojiText = it },
                        label = { Text("Sticker Text / Emoji") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = stickerTitle,
                        onValueChange = { stickerTitle = it },
                        label = { Text("Sticker Pack Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.addCustomSticker(
                            title = stickerTitle.ifBlank { "Creator Sticker" },
                            emojiOrText = stickerEmojiText.ifBlank { "🔥 VIP" }
                        )
                        showCreateCustomStickerDialog = false
                        Toast.makeText(context, "Custom Sticker saved to Firestore!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppEmeraldAccent)
                ) {
                    Text("Save Sticker", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCustomStickerDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Insufficient Coins Modal
    insufficientCoinsPrompt?.let { errText ->
        AlertDialog(
            onDismissRequest = { insufficientCoinsPrompt = null },
            containerColor = Color(0xFF1F2C34),
            title = { Text("💰 Wallet Balance Required", color = SnapYellowAccent, fontWeight = FontWeight.Bold) },
            text = {
                Text(errText, color = Color.White, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Allow instant verified coin package top-up so user can test gifting right away
                        repository.addCoins(500)
                        insufficientCoinsPrompt = null
                        Toast.makeText(context, "+500 Real Coins credited to your Firestore Wallet!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppEmeraldAccent)
                ) {
                    Text("Top Up +500 Coins Now", color = Color.Black, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { insufficientCoinsPrompt = null }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WhatsAppMessageBubble(
    message: ChatMessage,
    isSentByMe: Boolean,
    onLongPress: () -> Unit,
    onQuickReact: (String) -> Unit,
    onReplyClick: () -> Unit
) {
    var isPlayingVoice by remember { mutableStateOf(false) }
    var voiceProgress by remember { mutableFloatStateOf(0f) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(isPlayingVoice, playbackSpeed) {
        if (isPlayingVoice) {
            val totalMs = (message.audioDurationSec.coerceAtLeast(3) * 1000 / playbackSpeed).toLong()
            val stepMs = 100L
            var elapsed = (voiceProgress * totalMs).toLong()
            while (isPlayingVoice && elapsed < totalMs) {
                delay(stepMs)
                elapsed += stepMs
                voiceProgress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
            }
            isPlayingVoice = false
            voiceProgress = 0f
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isSentByMe) 16.dp else 4.dp,
                    bottomEnd = if (isSentByMe) 4.dp else 16.dp
                ),
                color = if (message.mediaType == "STICKER") Color.Transparent
                else if (isSentByMe) WhatsAppOutgoingBubble else WhatsAppIncomingBubble,
                border = if (message.mediaType == "STICKER") BorderStroke(1.5.dp, SnapYellowAccent) else null,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        onClick = { onReplyClick() },
                        onLongClick = onLongPress
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.isForwarded) {
                        Text(
                            text = "↪ Forwarded",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    if (message.replyToText.isNotBlank()) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "@${message.replyToSenderName}",
                                    color = WhatsAppEmeraldAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = message.replyToText,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    when (message.mediaType) {
                        "VOICE" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { isPlayingVoice = !isPlayingVoice },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(WhatsAppEmeraldAccent)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingVoice) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "Play Voice Note",
                                        tint = Color.Black
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(26.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    val bars = message.audioWaveform.ifEmpty {
                                        listOf(0.3f, 0.7f, 0.5f, 0.9f, 0.4f, 0.8f, 0.6f, 0.5f)
                                    }
                                    bars.forEachIndexed { idx, amp ->
                                        val ratio = idx.toFloat() / bars.size
                                        val active = ratio <= voiceProgress
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .fillMaxHeight(amp.coerceIn(0.2f, 1f))
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (active) SnapYellowAccent else Color.White.copy(alpha = 0.45f))
                                        )
                                    }
                                }
                                Surface(
                                    onClick = {
                                        playbackSpeed = when (playbackSpeed) {
                                            1.0f -> 1.5f
                                            1.5f -> 2.0f
                                            else -> 1.0f
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        "GIFT" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(message.giftEmoji.ifBlank { "🎁" }, fontSize = 26.sp)
                                Column {
                                    Text(
                                        text = message.text,
                                        color = SnapYellowAccent,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Verified Gift • ${message.giftCoins} Coins",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        "STICKER" -> {
                            Text(
                                text = message.text,
                                color = SnapYellowAccent,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        "DOCUMENT" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = SnapYellowAccent)
                                Column {
                                    Text(message.documentName.ifBlank { "Document.pdf" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${message.documentSizeKb} KB • Tap to open", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                }
                            }
                        }

                        "LOCATION" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = WhatsAppEmeraldAccent)
                                Text(message.locationName.ifBlank { message.text }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        "IMAGE" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (message.mediaUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = message.mediaUrl,
                                        contentDescription = "Shared photo",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Text(message.text, color = Color.White, fontSize = 13.sp)
                            }
                        }

                        else -> {
                            Text(
                                text = message.text,
                                color = if (message.isDeleted) Color.White.copy(alpha = 0.6f) else Color.White,
                                fontStyle = if (message.isDeleted) FontStyle.Italic else FontStyle.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.isStarred) {
                            Icon(Icons.Filled.Star, contentDescription = "Starred", tint = SnapYellowAccent, modifier = Modifier.size(11.dp))
                        }
                        Text(
                            text = formatWhatsAppTime(message.timestamp),
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 10.sp
                        )
                        if (isSentByMe) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = "Read",
                                tint = WhatsAppReadBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (message.reactionEmoji.isNotBlank()) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1F2C34),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = 8.dp)
                ) {
                    Text(
                        text = message.reactionEmoji,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .border(1.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun NewChatCreatorDirectoryDialog(
    availableUsers: List<UserProfile>,
    onDismiss: () -> Unit,
    onSelectUser: (UserProfile) -> Unit
) {
    var customUsername by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF162229),
        title = {
            Text("Start Live Firestore Chat", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = customUsername,
                    onValueChange = { customUsername = it },
                    placeholder = { Text("Enter any @username...", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = WhatsAppEmeraldAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (customUsername.isNotBlank()) {
                    Button(
                        onClick = {
                            val clean = customUsername.removePrefix("@").trim()
                            onSelectUser(
                                UserProfile(
                                    uid = "user_$clean",
                                    username = clean,
                                    displayName = clean.replaceFirstChar { it.uppercase() }
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppEmeraldAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Chat with @${customUsername.removePrefix("@")}", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Or pick a live creator:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableUsers, key = { it.username }) { u ->
                        Surface(
                            onClick = { onSelectUser(u) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1F2C34),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(u.displayName.ifBlank { u.username }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("@${u.username}", color = WhatsAppEmeraldAccent, fontSize = 11.sp)
                                }
                                Text("Chat →", color = SnapYellowAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun TashanBlockedUsersTab(
    blockedUsers: List<com.example.model.BlockedUserItem>,
    onUnblock: (com.example.model.BlockedUserItem) -> Unit
) {
    if (blockedUsers.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = TashanCyanAccent.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "No Blocked Users",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "Users you block will appear here. They cannot send you message requests, voice notes, or place audio/video calls.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(blockedUsers, key = { it.uid }) { item ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TashanCardBg,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blocked_user_${item.uid}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2B141C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Block, contentDescription = null, tint = TashanRoseAccent, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(item.displayName.ifBlank { item.username }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("@${item.username}", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = { onUnblock(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = TashanCyanAccent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Unblock", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatsAppCallsTab(
    callLogs: List<CallLogEntry>,
    conversations: List<Conversation>,
    onStartCall: (Conversation, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (callLogs.isEmpty() && conversations.isNotEmpty()) {
            item {
                Text("Tap a chat to place an encrypted voice or HD video call:", color = TextSecondary, fontSize = 12.sp)
            }
            items(conversations, key = { it.id }) { conv ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF162229),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(conv.otherDisplayName.ifBlank { conv.otherUsername }, color = Color.White, fontWeight = FontWeight.Bold)
                        Row {
                            IconButton(onClick = { onStartCall(conv, false) }) {
                                Icon(Icons.Filled.Call, contentDescription = "Audio Call", tint = WhatsAppEmeraldAccent)
                            }
                            IconButton(onClick = { onStartCall(conv, true) }) {
                                Icon(Icons.Filled.Videocam, contentDescription = "Video Call", tint = SnapYellowAccent)
                            }
                        }
                    }
                }
            }
        } else {
            items(callLogs, key = { it.id }) { log ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF162229),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(log.otherDisplayName.ifBlank { log.otherUsername }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${log.direction} • ${log.durationText}", color = WhatsAppEmeraldAccent, fontSize = 11.sp)
                        }
                        Icon(
                            imageVector = if (log.isVideoCall) Icons.Filled.Videocam else Icons.Filled.Call,
                            contentDescription = null,
                            tint = WhatsAppEmeraldAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatsAppCallOverlayScreen(
    conversation: Conversation,
    isVideoCall: Boolean,
    onEndCall: (String) -> Unit
) {
    var seconds by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(true) }
    var isCameraOn by remember { mutableStateOf(true) }
    var isFrontCamera by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds++
        }
    }

    val formatted = remember(seconds) {
        val m = (seconds / 60).toString().padStart(2, '0')
        val s = (seconds % 60).toString().padStart(2, '0')
        "$m:$s"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF141724), Color(0xFF090A0F))
                )
            )
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = WhatsAppEmeraldAccent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, WhatsAppEmeraldAccent.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isVideoCall) Icons.Filled.Videocam else Icons.Filled.Call,
                            contentDescription = null,
                            tint = WhatsAppEmeraldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "🔒 Tashan E2E Encrypted ${if (isVideoCall) "Video" else "Voice"} Call",
                            color = WhatsAppEmeraldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isVideoCall && isCameraOn) {
                    // HD Video Stream Preview Frame
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, WhatsAppEmeraldAccent.copy(alpha = 0.7f)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161926))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                        .background(WhatsAppOutgoingBubble),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (conversation.otherDisplayName.ifBlank { conversation.otherUsername }).take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 28.sp
                                    )
                                }
                                Text(
                                    text = "1080p HD Live Stream",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isFrontCamera) "📷 Front Camera (Selfie Mode)" else "📷 Rear Ultra-Wide Camera",
                                    color = WhatsAppEmeraldAccent,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(WhatsAppOutgoingBubble)
                            .border(3.dp, WhatsAppEmeraldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (conversation.otherDisplayName.ifBlank { conversation.otherUsername }).take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 42.sp
                        )
                    }
                }

                Text(
                    text = conversation.otherDisplayName.ifBlank { conversation.otherUsername },
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = formatted,
                    color = SnapYellowAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            // Call Controls Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color.Red else Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic, contentDescription = "Mute", tint = Color.White)
                }

                if (isVideoCall) {
                    // Camera Toggle
                    IconButton(
                        onClick = { isCameraOn = !isCameraOn },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (!isCameraOn) Color.Red else Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(if (isCameraOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff, contentDescription = "Camera Toggle", tint = Color.White)
                    }

                    // Flip Camera
                    IconButton(
                        onClick = { isFrontCamera = !isFrontCamera },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Filled.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
                    }
                }

                // Speaker
                IconButton(
                    onClick = { isSpeaker = !isSpeaker },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isSpeaker) WhatsAppEmeraldAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "Speaker", tint = Color.White)
                }

                // End Call
                IconButton(
                    onClick = { onEndCall(formatted) },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF1744))
                ) {
                    Icon(Icons.Filled.CallEnd, contentDescription = "End Call", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TashanVerificationGate(
    onOpenAuth: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TashanDarkBg)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = TashanHeaderBg,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(TashanCyanAccent, TashanRoseAccent))),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tashan_verification_gate_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(TashanCyanAccent, TashanRoseAccent))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = "Verified Only",
                        tint = Color.Black,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    text = "Tashan Verified Messenger",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "To protect users from fake chats, harassment, and spam, direct messaging and voice/video calling are restricted to verified accounts.\n\nPlease log in with Google, or verify your Phone / Email OTP to enter Tashan Chat.",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = { onOpenAuth("Sign in with Google or verify to open Tashan Chat") },
                    colors = ButtonDefaults.buttonColors(containerColor = TashanCyanAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("verification_gate_login_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.LockOpen, contentDescription = null, tint = Color.Black)
                        Text(
                            text = "Log In & Verify Account",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatWhatsAppTime(timestamp: Long): String {
    return try {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    } catch (e: Exception) {
        "Now"
    }
}
