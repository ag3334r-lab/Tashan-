package com.example.data

import android.util.Log
import com.example.model.BlockedUserItem
import com.example.model.CallLogEntry
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.StatusUpdate
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class RealtimeChatManager private constructor() {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 100% Live Firestore State — No hardcoded demo conversations
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    private val _statusUpdates = MutableStateFlow<List<StatusUpdate>>(emptyList())
    val statusUpdates: StateFlow<List<StatusUpdate>> = _statusUpdates.asStateFlow()

    private val _callLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val callLogs: StateFlow<List<CallLogEntry>> = _callLogs.asStateFlow()

    private val _discoverableUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val discoverableUsers: StateFlow<List<UserProfile>> = _discoverableUsers.asStateFlow()

    private val _blockedUsers = MutableStateFlow<List<BlockedUserItem>>(emptyList())
    val blockedUsers: StateFlow<List<BlockedUserItem>> = _blockedUsers.asStateFlow()

    private var conversationsListener: ListenerRegistration? = null
    private var statusListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var callLogsListener: ListenerRegistration? = null
    private var blockedUsersListener: ListenerRegistration? = null
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()

    companion object {
        private const val TAG = "RealtimeChatManager"

        @Volatile
        private var INSTANCE: RealtimeChatManager? = null

        fun getInstance(): RealtimeChatManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RealtimeChatManager().also { INSTANCE = it }
            }
        }
    }

    init {
        attachConversationsListener()
        attachStatusUpdatesListener()
        attachDiscoverableUsersListener()
        attachCallLogsListener()
        attachBlockedUsersListener()
    }

    fun attachBlockedUsersListener() {
        if (blockedUsersListener != null) return
        try {
            blockedUsersListener = firestore.collection("blocked_users")
                .limit(100)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                BlockedUserItem(
                                    uid = doc.getString("uid") ?: doc.id,
                                    username = doc.getString("username") ?: "user",
                                    displayName = doc.getString("displayName") ?: "Blocked User",
                                    avatarUrl = doc.getString("avatarUrl") ?: "",
                                    blockedDate = doc.getString("blockedDate") ?: "Blocked"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _blockedUsers.value = list.distinctBy { it.uid }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching blocked users listener: ${e.message}")
        }
    }

    /**
     * Listens to real registered users in Firestore `/users` so users can start real-time chats.
     */
    fun attachDiscoverableUsersListener() {
        if (usersListener != null) return
        try {
            usersListener = firestore.collection("users")
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Users listener warning: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val users = snapshot.documents.mapNotNull { doc ->
                            try {
                                val uid = doc.getString("uid") ?: doc.id
                                val username = doc.getString("username") ?: return@mapNotNull null
                                val displayName = doc.getString("displayName") ?: username
                                val bio = doc.getString("bio") ?: ""
                                val avatarUrl = doc.getString("avatarUrl") ?: ""
                                val phoneNumber = doc.getString("phoneNumber") ?: ""
                                val followersCount = doc.getLong("followersCount")?.toInt() ?: 0
                                UserProfile(
                                    uid = uid,
                                    username = username,
                                    displayName = displayName,
                                    bio = bio,
                                    avatarUrl = avatarUrl,
                                    phoneNumber = phoneNumber,
                                    followersCount = followersCount
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _discoverableUsers.value = users.distinctBy { it.uid }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching discoverable users listener: ${e.message}")
        }
    }

    /**
     * Listens to live status/story updates in Firestore `/status_updates`.
     */
    fun attachStatusUpdatesListener() {
        if (statusListener != null) return
        try {
            statusListener = firestore.collection("status_updates")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(40)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Status listener warning: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val now = System.currentTimeMillis()
                        val cutoff24h = now - 86_400_000L
                        val liveStatuses = snapshot.documents.mapNotNull { doc ->
                            try {
                                val ts = doc.getLong("timestamp") ?: now
                                if (ts < cutoff24h) return@mapNotNull null
                                StatusUpdate(
                                    id = doc.id,
                                    userUid = doc.getString("userUid") ?: "",
                                    username = doc.getString("username") ?: "creator",
                                    displayName = doc.getString("displayName") ?: "Creator",
                                    avatarUrl = doc.getString("avatarUrl") ?: "",
                                    mediaCaption = doc.getString("mediaCaption") ?: "",
                                    backgroundColorHex = doc.getLong("backgroundColorHex") ?: 0xFF005C4B,
                                    emojiBadge = doc.getString("emojiBadge") ?: "🔥",
                                    timestamp = ts,
                                    isViewed = doc.getBoolean("isViewed") ?: false,
                                    storyTimerSec = doc.getLong("storyTimerSec")?.toInt() ?: 5
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _statusUpdates.value = liveStatuses
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching status listener: ${e.message}")
        }
    }

    /**
     * Listens to live call logs in Firestore `/call_logs`.
     */
    fun attachCallLogsListener() {
        if (callLogsListener != null) return
        try {
            callLogsListener = firestore.collection("call_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(40)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Call logs listener warning: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val logs = snapshot.documents.mapNotNull { doc ->
                            try {
                                CallLogEntry(
                                    id = doc.id,
                                    conversationId = doc.getString("conversationId") ?: "",
                                    otherUsername = doc.getString("otherUsername") ?: "",
                                    otherDisplayName = doc.getString("otherDisplayName") ?: "",
                                    otherAvatarUrl = doc.getString("otherAvatarUrl") ?: "",
                                    isVideoCall = doc.getBoolean("isVideoCall") ?: false,
                                    direction = doc.getString("direction") ?: "OUTGOING",
                                    durationText = doc.getString("durationText") ?: "00:45",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _callLogs.value = logs
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching call logs listener: ${e.message}")
        }
    }

    /**
     * Attaches a real-time listener to the `conversations` collection in Firestore.
     */
    fun attachConversationsListener() {
        if (conversationsListener != null) return
        try {
            conversationsListener = firestore.collection("conversations")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore conversations listener warning: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val remoteList = snapshot.documents.mapNotNull { doc ->
                            try {
                                val blockedRaw = doc.get("blockedByUids") as? List<*>
                                val blockedList = blockedRaw?.mapNotNull { it as? String } ?: emptyList()
                                Conversation(
                                    id = doc.id,
                                    otherUserUid = doc.getString("otherUserUid") ?: "",
                                    otherUsername = doc.getString("otherUsername") ?: "creator",
                                    otherDisplayName = doc.getString("otherDisplayName") ?: "",
                                    otherAvatarUrl = doc.getString("otherAvatarUrl") ?: "",
                                    lastMessage = doc.getString("lastMessage") ?: "",
                                    lastMessageType = doc.getString("lastMessageType") ?: "TEXT",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                    unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0,
                                    isOnline = doc.getBoolean("isOnline") ?: true,
                                    isTyping = doc.getBoolean("isTyping") ?: false,
                                    isSentByMe = doc.getBoolean("isSentByMe") ?: false,
                                    isPinned = doc.getBoolean("isPinned") ?: false,
                                    isMuted = doc.getBoolean("isMuted") ?: false,
                                    lastSeenText = doc.getString("lastSeenText") ?: "Online",
                                    phoneNumber = doc.getString("phoneNumber") ?: "",
                                    status = doc.getString("status") ?: "PENDING",
                                    initiatorUid = doc.getString("initiatorUid") ?: "",
                                    recipientHasReplied = doc.getBoolean("recipientHasReplied") ?: false,
                                    blockedByUids = blockedList,
                                    streakCount = doc.getLong("streakCount")?.toInt() ?: 0,
                                    lastStreakTimestamp = doc.getLong("lastStreakTimestamp") ?: 0L,
                                    retentionPolicy = doc.getString("retentionPolicy") ?: "24_HOURS"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        val sorted = remoteList.sortedWith(
                            compareByDescending<Conversation> { it.isPinned }
                                .thenByDescending { it.timestamp }
                        )
                        _conversations.value = sorted
                        sorted.take(3).forEach { conv ->
                            attachMessagesListener(conv.id)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching conversations listener", e)
        }
    }

    /**
     * Attaches a real-time listener to `conversations/{conversationId}/messages` in Firestore.
     * Automatically filters out expired messages if 24_HOURS retention TTL has passed.
     */
    fun attachMessagesListener(conversationId: String) {
        if (conversationId.isBlank() || messageListeners.containsKey(conversationId)) return

        try {
            val listener = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(100)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore messages listener warning for $conversationId: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val now = System.currentTimeMillis()
                        val cutoff24h = now - 86_400_000L
                        val convRetention = _conversations.value.find { it.id == conversationId }?.retentionPolicy ?: "24_HOURS"

                        val remoteMsgs = snapshot.documents.mapNotNull { doc ->
                            try {
                                val ts = doc.getLong("timestamp") ?: now
                                val isStarred = doc.getBoolean("isStarred") ?: false
                                // Filter 24-hour auto-delete retention in-memory without recursive listener writes
                                if (convRetention == "24_HOURS" && !isStarred && ts < cutoff24h) {
                                    return@mapNotNull null
                                }
                                val waveformRaw = doc.get("audioWaveform") as? List<*>
                                val waveform = waveformRaw?.mapNotNull { (it as? Number)?.toFloat() }
                                    ?: listOf(0.25f, 0.6f, 0.85f, 0.4f, 0.9f, 0.5f, 0.75f, 0.3f)
                                ChatMessage(
                                    id = doc.id,
                                    conversationId = conversationId,
                                    senderUid = doc.getString("senderUid") ?: "",
                                    senderUsername = doc.getString("senderUsername") ?: "",
                                    text = doc.getString("text") ?: "",
                                    mediaUrl = doc.getString("mediaUrl") ?: "",
                                    mediaType = doc.getString("mediaType") ?: "TEXT",
                                    giftEmoji = doc.getString("giftEmoji") ?: "",
                                    giftCoins = doc.getLong("giftCoins")?.toInt() ?: 0,
                                    reactionEmoji = doc.getString("reactionEmoji") ?: "",
                                    replyToMessageId = doc.getString("replyToMessageId") ?: "",
                                    replyToSenderName = doc.getString("replyToSenderName") ?: "",
                                    replyToText = doc.getString("replyToText") ?: "",
                                    isForwarded = doc.getBoolean("isForwarded") ?: false,
                                    isDeleted = doc.getBoolean("isDeleted") ?: false,
                                    isStarred = isStarred,
                                    deliveryStatus = doc.getString("deliveryStatus") ?: "READ",
                                    audioDurationSec = doc.getLong("audioDurationSec")?.toInt() ?: 0,
                                    audioWaveform = waveform,
                                    documentName = doc.getString("documentName") ?: "",
                                    documentSizeKb = doc.getLong("documentSizeKb")?.toInt() ?: 0,
                                    locationName = doc.getString("locationName") ?: "",
                                    timestamp = ts,
                                    isRead = doc.getBoolean("isRead") ?: true
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        val updatedMap = _messages.value.toMutableMap()
                        updatedMap[conversationId] = remoteMsgs.sortedBy { it.timestamp }
                        _messages.value = updatedMap
                    }
                }
            messageListeners[conversationId] = listener
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching messages listener for $conversationId", e)
        }
    }

    /**
     * Starts or retrieves a live Firestore conversation with a real user.
     */
    fun startOrGetConversationWithUser(
        currentUser: UserProfile,
        targetUser: UserProfile,
        defaultRetentionPolicy: String = "24_HOURS"
    ): Conversation {
        val existing = _conversations.value.find {
            it.otherUserUid == targetUser.uid || it.otherUsername.equals(targetUser.username, ignoreCase = true)
        }
        if (existing != null) {
            attachMessagesListener(existing.id)
            return existing
        }

        val convId = "conv_${targetUser.username.lowercase().replace("[^a-z0-9_]".toRegex(), "")}_${UUID.randomUUID().toString().take(4)}"
        val now = System.currentTimeMillis()
        val newConv = Conversation(
            id = convId,
            otherUserUid = targetUser.uid,
            otherUsername = targetUser.username,
            otherDisplayName = targetUser.displayName.ifBlank { targetUser.username },
            otherAvatarUrl = targetUser.avatarUrl,
            lastMessage = "Send a chat request to connect",
            lastMessageType = "TEXT",
            timestamp = now,
            unreadCount = 0,
            isOnline = true,
            isTyping = false,
            isSentByMe = true,
            isPinned = false,
            isMuted = false,
            lastSeenText = "Verified Tashan ID",
            phoneNumber = targetUser.phoneNumber,
            status = "PENDING",
            initiatorUid = "",
            recipientHasReplied = false,
            blockedByUids = emptyList(),
            streakCount = 1,
            lastStreakTimestamp = now,
            retentionPolicy = defaultRetentionPolicy
        )

        _conversations.value = listOf(newConv) + _conversations.value
        attachMessagesListener(convId)

        try {
            val convData = mapOf(
                "otherUserUid" to newConv.otherUserUid,
                "otherUsername" to newConv.otherUsername,
                "otherDisplayName" to newConv.otherDisplayName,
                "otherAvatarUrl" to newConv.otherAvatarUrl,
                "lastMessage" to newConv.lastMessage,
                "lastMessageType" to newConv.lastMessageType,
                "timestamp" to newConv.timestamp,
                "unreadCount" to 0,
                "isOnline" to true,
                "isTyping" to false,
                "isSentByMe" to true,
                "isPinned" to false,
                "isMuted" to false,
                "lastSeenText" to newConv.lastSeenText,
                "phoneNumber" to newConv.phoneNumber,
                "status" to newConv.status,
                "initiatorUid" to newConv.initiatorUid,
                "recipientHasReplied" to newConv.recipientHasReplied,
                "blockedByUids" to newConv.blockedByUids,
                "streakCount" to newConv.streakCount,
                "lastStreakTimestamp" to newConv.lastStreakTimestamp,
                "retentionPolicy" to newConv.retentionPolicy
            )
            firestore.collection("conversations").document(convId).set(convData)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating conversation in Firestore", e)
        }
        return newConv
    }

    /**
     * Updates Snapchat-style message retention policy ("IMMEDIATE" vs "24_HOURS") for a conversation.
     */
    fun updateConversationRetentionPolicy(conversationId: String, policy: String) {
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversationId) conv.copy(retentionPolicy = policy) else conv
        }
        try {
            firestore.collection("conversations")
                .document(conversationId)
                .update("retentionPolicy", policy)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update retentionPolicy in Firestore: ${e.message}")
        }
    }

    /**
     * Purges non-starred viewed messages when exiting a conversation configured with "IMMEDIATE" retention.
     */
    fun purgeImmediateMessagesOnExit(conversationId: String) {
        val conv = _conversations.value.find { it.id == conversationId } ?: return
        if (conv.retentionPolicy != "IMMEDIATE") return

        val currentMsgs = _messages.value[conversationId] ?: return
        val starredOnly = currentMsgs.filter { it.isStarred }
        val toDelete = currentMsgs.filter { !it.isStarred }

        val updatedMap = _messages.value.toMutableMap()
        updatedMap[conversationId] = starredOnly
        _messages.value = updatedMap

        _conversations.value = _conversations.value.map { c ->
            if (c.id == conversationId) {
                c.copy(
                    lastMessage = if (starredOnly.isEmpty()) "👻 Messages auto-deleted after viewing" else starredOnly.last().text,
                    unreadCount = 0
                )
            } else c
        }

        try {
            val msgCol = firestore.collection("conversations").document(conversationId).collection("messages")
            toDelete.forEach { msg ->
                msgCol.document(msg.id).delete()
            }
            firestore.collection("conversations").document(conversationId)
                .update("lastMessage", "👻 Messages auto-deleted after viewing")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to purge immediate messages in Firestore: ${e.message}")
        }
    }

    /**
     * Restores or boosts a Snap Streak on a conversation in Firestore.
     */
    fun restoreSnapStreak(conversationId: String) {
        val currentConv = _conversations.value.find { it.id == conversationId } ?: return
        val newStreak = (currentConv.streakCount.coerceAtLeast(1)) + 1
        val now = System.currentTimeMillis()
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversationId) {
                conv.copy(streakCount = newStreak, lastStreakTimestamp = now)
            } else conv
        }
        try {
            firestore.collection("conversations").document(conversationId)
                .update(
                    mapOf(
                        "streakCount" to newStreak,
                        "lastStreakTimestamp" to now
                    )
                )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to restore streak in Firestore: ${e.message}")
        }
    }

    /**
     * Sends a message (Text, Voice, Image, Video, Document, Location, Sticker, or Gift)
     * and persists it to Cloud Firestore while maintaining Snap Streaks.
     */
    fun sendMessage(
        conversationId: String,
        sender: UserProfile,
        text: String,
        mediaUrl: String = "",
        mediaType: String = "TEXT",
        giftEmoji: String = "",
        giftCoins: Int = 0,
        replyToMessage: ChatMessage? = null,
        isForwarded: Boolean = false,
        audioDurationSec: Int = 0,
        audioWaveform: List<Float> = emptyList(),
        documentName: String = "",
        documentSizeKb: Int = 0,
        locationName: String = ""
    ) {
        val msgId = "msg_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val newMessage = ChatMessage(
            id = msgId,
            conversationId = conversationId,
            senderUid = sender.uid,
            senderUsername = sender.username,
            text = text,
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            giftEmoji = giftEmoji,
            giftCoins = giftCoins,
            replyToMessageId = replyToMessage?.id ?: "",
            replyToSenderName = replyToMessage?.senderUsername ?: "",
            replyToText = replyToMessage?.text ?: "",
            isForwarded = isForwarded,
            isDeleted = false,
            isStarred = false,
            deliveryStatus = "READ",
            audioDurationSec = audioDurationSec,
            audioWaveform = audioWaveform,
            documentName = documentName,
            documentSizeKb = documentSizeKb,
            locationName = locationName,
            timestamp = now,
            isRead = true
        )

        val currentMap = _messages.value.toMutableMap()
        val currentList = currentMap[conversationId]?.toMutableList() ?: mutableListOf()
        currentList.add(newMessage)
        currentMap[conversationId] = currentList
        _messages.value = currentMap

        val previewText = when (mediaType) {
            "GIFT" -> "🎁 Sent $giftEmoji ($giftCoins Coins)"
            "IMAGE" -> "📷 Photo: $text"
            "VIDEO" -> "🎬 Video: $text"
            "VOICE" -> "🎤 Voice note (0:${audioDurationSec.toString().padStart(2, '0')})"
            "DOCUMENT" -> "📄 $documentName"
            "LOCATION" -> "📍 $locationName"
            "STICKER" -> "✨ Sticker: $text"
            else -> text
        }

        val existingConv = _conversations.value.find { it.id == conversationId }
        if (existingConv != null && existingConv.isBlocked) {
            return
        }
        val updatedStreak = ((existingConv?.streakCount ?: 0).coerceAtLeast(1)) +
            if (now - (existingConv?.lastStreakTimestamp ?: 0L) > 3_600_000L) 1 else 0

        val resolvedInitiatorUid = if (existingConv?.initiatorUid.isNullOrBlank()) {
            sender.uid
        } else {
            existingConv!!.initiatorUid
        }
        val isReplyFromOtherUser = resolvedInitiatorUid.isNotBlank() && resolvedInitiatorUid != sender.uid
        val resolvedStatus = when {
            existingConv?.status == "BLOCKED" -> "BLOCKED"
            existingConv?.status == "ACCEPTED" || isReplyFromOtherUser || existingConv?.recipientHasReplied == true -> "ACCEPTED"
            else -> "PENDING"
        }
        val resolvedRecipientReplied = existingConv?.recipientHasReplied == true || isReplyFromOtherUser

        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversationId) {
                conv.copy(
                    lastMessage = previewText,
                    lastMessageType = mediaType,
                    timestamp = now,
                    unreadCount = 0,
                    isSentByMe = true,
                    status = resolvedStatus,
                    initiatorUid = resolvedInitiatorUid,
                    recipientHasReplied = resolvedRecipientReplied,
                    streakCount = updatedStreak,
                    lastStreakTimestamp = now
                )
            } else conv
        }.sortedWith(compareByDescending<Conversation> { it.isPinned }.thenByDescending { it.timestamp })

        try {
            val msgRef = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(msgId)

            val data = mapOf(
                "conversationId" to conversationId,
                "senderUid" to sender.uid,
                "senderUsername" to sender.username,
                "text" to text,
                "mediaUrl" to mediaUrl,
                "mediaType" to mediaType,
                "giftEmoji" to giftEmoji,
                "giftCoins" to giftCoins,
                "reactionEmoji" to "",
                "replyToMessageId" to (replyToMessage?.id ?: ""),
                "replyToSenderName" to (replyToMessage?.senderUsername ?: ""),
                "replyToText" to (replyToMessage?.text ?: ""),
                "isForwarded" to isForwarded,
                "isDeleted" to false,
                "isStarred" to false,
                "deliveryStatus" to "READ",
                "audioDurationSec" to audioDurationSec,
                "audioWaveform" to audioWaveform,
                "documentName" to documentName,
                "documentSizeKb" to documentSizeKb,
                "locationName" to locationName,
                "timestamp" to now,
                "isRead" to true
            )
            msgRef.set(data)

            val targetConv = _conversations.value.find { it.id == conversationId }
            val convUpdate = mapOf(
                "otherUserUid" to (targetConv?.otherUserUid ?: ""),
                "otherUsername" to (targetConv?.otherUsername ?: "creator"),
                "otherDisplayName" to (targetConv?.otherDisplayName ?: "Creator"),
                "otherAvatarUrl" to (targetConv?.otherAvatarUrl ?: ""),
                "lastMessage" to previewText,
                "lastMessageType" to mediaType,
                "timestamp" to now,
                "unreadCount" to 0,
                "isOnline" to (targetConv?.isOnline ?: true),
                "isTyping" to false,
                "isSentByMe" to true,
                "isPinned" to (targetConv?.isPinned ?: false),
                "isMuted" to (targetConv?.isMuted ?: false),
                "lastSeenText" to (targetConv?.lastSeenText ?: "Online"),
                "phoneNumber" to (targetConv?.phoneNumber ?: ""),
                "status" to resolvedStatus,
                "initiatorUid" to resolvedInitiatorUid,
                "recipientHasReplied" to resolvedRecipientReplied,
                "blockedByUids" to (targetConv?.blockedByUids ?: emptyList()),
                "streakCount" to updatedStreak,
                "lastStreakTimestamp" to now,
                "retentionPolicy" to (targetConv?.retentionPolicy ?: "24_HOURS")
            )
            firestore.collection("conversations")
                .document(conversationId)
                .set(convUpdate)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing message to Firestore", e)
        }
    }

    /**
     * Forwards an existing message to another conversation.
     */
    fun forwardMessage(
        sourceMessage: ChatMessage,
        targetConversationId: String,
        sender: UserProfile
    ) {
        sendMessage(
            conversationId = targetConversationId,
            sender = sender,
            text = sourceMessage.text,
            mediaUrl = sourceMessage.mediaUrl,
            mediaType = sourceMessage.mediaType,
            giftEmoji = sourceMessage.giftEmoji,
            giftCoins = sourceMessage.giftCoins,
            isForwarded = true,
            audioDurationSec = sourceMessage.audioDurationSec,
            audioWaveform = sourceMessage.audioWaveform,
            documentName = sourceMessage.documentName,
            documentSizeKb = sourceMessage.documentSizeKb,
            locationName = sourceMessage.locationName
        )
    }

    /**
     * Deletes a message for everyone or removes it from the thread.
     */
    fun deleteMessage(conversationId: String, messageId: String, deleteForEveryone: Boolean = true) {
        val currentMap = _messages.value.toMutableMap()
        val currentList = currentMap[conversationId]?.toMutableList() ?: return

        if (deleteForEveryone) {
            currentMap[conversationId] = currentList.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(
                        text = "🚫 This message was deleted",
                        mediaType = "TEXT",
                        mediaUrl = "",
                        giftEmoji = "",
                        isDeleted = true
                    )
                } else msg
            }
        } else {
            currentMap[conversationId] = currentList.filterNot { it.id == messageId }
        }
        _messages.value = currentMap

        try {
            val docRef = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
            if (deleteForEveryone) {
                docRef.update(
                    mapOf(
                        "text" to "🚫 This message was deleted",
                        "mediaType" to "TEXT",
                        "isDeleted" to true
                    )
                )
            } else {
                docRef.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting message in Firestore: ${e.message}")
        }
    }

    /**
     * Toggles starred status on a message.
     */
    fun toggleStarMessage(conversationId: String, messageId: String) {
        val currentMap = _messages.value.toMutableMap()
        val currentList = currentMap[conversationId]?.toMutableList() ?: return
        var newStarred = false
        currentMap[conversationId] = currentList.map { msg ->
            if (msg.id == messageId) {
                newStarred = !msg.isStarred
                msg.copy(isStarred = newStarred)
            } else msg
        }
        _messages.value = currentMap
        try {
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update("isStarred", newStarred)
        } catch (e: Exception) {
            Log.w(TAG, "Error starring message: ${e.message}")
        }
    }

    /**
     * Toggles pin or mute on a conversation.
     */
    fun togglePinConversation(conversationId: String) {
        var newPinned = false
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversationId) {
                newPinned = !conv.isPinned
                conv.copy(isPinned = newPinned)
            } else conv
        }.sortedWith(compareByDescending<Conversation> { it.isPinned }.thenByDescending { it.timestamp })
        try {
            firestore.collection("conversations").document(conversationId).update("isPinned", newPinned)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update pin in Firestore: ${e.message}")
        }
    }

    fun toggleMuteConversation(conversationId: String) {
        var newMuted = false
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversationId) {
                newMuted = !conv.isMuted
                conv.copy(isMuted = newMuted)
            } else conv
        }
        try {
            firestore.collection("conversations").document(conversationId).update("isMuted", newMuted)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update mute in Firestore: ${e.message}")
        }
    }

    /**
     * Adds an emoji reaction to a specific message in a conversation.
     */
    fun reactToMessage(conversationId: String, messageId: String, emoji: String) {
        val currentMap = _messages.value.toMutableMap()
        val currentList = currentMap[conversationId]?.toMutableList() ?: return
        val updatedList = currentList.map { msg ->
            if (msg.id == messageId) {
                val newEmoji = if (msg.reactionEmoji == emoji) "" else emoji
                msg.copy(reactionEmoji = newEmoji)
            } else msg
        }
        currentMap[conversationId] = updatedList
        _messages.value = currentMap

        try {
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update("reactionEmoji", emoji)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update reaction in Firestore: ${e.message}")
        }
    }

    /**
     * Posts a new 24h WhatsApp/Snapchat Status update and syncs to Firestore.
     */
    fun postStatusUpdate(
        user: UserProfile,
        caption: String,
        bgColorHex: Long,
        emoji: String,
        storyTimerSec: Int = 5
    ) {
        val statusId = "st_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val newStatus = StatusUpdate(
            id = statusId,
            userUid = user.uid,
            username = user.username,
            displayName = user.displayName.ifBlank { user.username },
            avatarUrl = user.avatarUrl,
            mediaCaption = caption,
            backgroundColorHex = bgColorHex,
            emojiBadge = emoji,
            timestamp = now,
            isViewed = false,
            storyTimerSec = storyTimerSec
        )
        _statusUpdates.value = listOf(newStatus) + _statusUpdates.value
        try {
            firestore.collection("status_updates").document(statusId).set(
                mapOf(
                    "userUid" to newStatus.userUid,
                    "username" to newStatus.username,
                    "displayName" to newStatus.displayName,
                    "avatarUrl" to newStatus.avatarUrl,
                    "mediaCaption" to newStatus.mediaCaption,
                    "backgroundColorHex" to newStatus.backgroundColorHex,
                    "emojiBadge" to newStatus.emojiBadge,
                    "timestamp" to newStatus.timestamp,
                    "isViewed" to false,
                    "storyTimerSec" to storyTimerSec
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to post status update to Firestore: ${e.message}")
        }
    }

    fun markStatusViewed(statusId: String) {
        _statusUpdates.value = _statusUpdates.value.map {
            if (it.id == statusId) it.copy(isViewed = true) else it
        }
        try {
            firestore.collection("status_updates").document(statusId).update("isViewed", true)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to mark status viewed in Firestore: ${e.message}")
        }
    }

    /**
     * Logs a completed audio or video call in Firestore `/call_logs`.
     */
    fun logCall(conversation: Conversation, isVideoCall: Boolean, durationText: String) {
        val callId = "call_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val entry = CallLogEntry(
            id = callId,
            conversationId = conversation.id,
            otherUsername = conversation.otherUsername,
            otherDisplayName = conversation.otherDisplayName.ifBlank { conversation.otherUsername },
            otherAvatarUrl = conversation.otherAvatarUrl,
            isVideoCall = isVideoCall,
            direction = "OUTGOING",
            durationText = durationText,
            timestamp = now
        )
        _callLogs.value = listOf(entry) + _callLogs.value
        try {
            firestore.collection("call_logs").document(callId).set(
                mapOf(
                    "conversationId" to entry.conversationId,
                    "otherUsername" to entry.otherUsername,
                    "otherDisplayName" to entry.otherDisplayName,
                    "otherAvatarUrl" to entry.otherAvatarUrl,
                    "isVideoCall" to entry.isVideoCall,
                    "direction" to entry.direction,
                    "durationText" to entry.durationText,
                    "timestamp" to entry.timestamp
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write call log to Firestore: ${e.message}")
        }
    }

    fun acceptMessageRequest(conversationId: String) {
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversationId) {
                conv.copy(status = "ACCEPTED", recipientHasReplied = true, unreadCount = 0)
            } else conv
        }
        try {
            firestore.collection("conversations")
                .document(conversationId)
                .update(
                    mapOf(
                        "status" to "ACCEPTED",
                        "recipientHasReplied" to true
                    )
                )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update conversation status in Firestore: ${e.message}")
        }
    }

    /**
     * Blocks a user and conversation, adding them to the Firestore `blocked_users` collection
     * and preventing any messages or calls until unblocked.
     */
    fun blockUser(conversation: Conversation, blockerUid: String) {
        val updatedBlockedUids = (conversation.blockedByUids + blockerUid).distinct()
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversation.id) {
                conv.copy(status = "BLOCKED", blockedByUids = updatedBlockedUids)
            } else conv
        }
        val blockedItem = BlockedUserItem(
            uid = conversation.otherUserUid.ifBlank { conversation.otherUsername },
            username = conversation.otherUsername,
            displayName = conversation.otherDisplayName.ifBlank { conversation.otherUsername },
            avatarUrl = conversation.otherAvatarUrl,
            blockedDate = "Blocked just now"
        )
        _blockedUsers.value = (_blockedUsers.value + blockedItem).distinctBy { it.uid }

        try {
            firestore.collection("conversations").document(conversation.id)
                .update(
                    mapOf(
                        "status" to "BLOCKED",
                        "blockedByUids" to updatedBlockedUids
                    )
                )
            firestore.collection("blocked_users").document(blockedItem.uid)
                .set(
                    mapOf(
                        "uid" to blockedItem.uid,
                        "username" to blockedItem.username,
                        "displayName" to blockedItem.displayName,
                        "avatarUrl" to blockedItem.avatarUrl,
                        "blockedDate" to blockedItem.blockedDate,
                        "blockerUid" to blockerUid,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to block user in Firestore: ${e.message}")
        }
    }

    /**
     * Unblocks a user from the Block List and restores conversation state.
     */
    fun unblockUser(targetUidOrUsername: String) {
        _blockedUsers.value = _blockedUsers.value.filterNot {
            it.uid == targetUidOrUsername || it.username.equals(targetUidOrUsername, ignoreCase = true)
        }
        _conversations.value = _conversations.value.map { conv ->
            if (conv.otherUserUid == targetUidOrUsername || conv.otherUsername.equals(targetUidOrUsername, ignoreCase = true)) {
                val restoredConv = conv.copy(status = "ACCEPTED", blockedByUids = emptyList())
                try {
                    firestore.collection("conversations").document(conv.id)
                        .update(
                            mapOf(
                                "status" to "ACCEPTED",
                                "blockedByUids" to emptyList<String>()
                            )
                        )
                } catch (_: Exception) {}
                restoredConv
            } else conv
        }
        try {
            firestore.collection("blocked_users").document(targetUidOrUsername).delete()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unblock user in Firestore: ${e.message}")
        }
    }

    /**
     * Triggers an incoming creator reply & request acceptance in Firestore so the user can experience
     * the full mutual-consent unlock when the recipient messages back.
     */
    fun triggerRecipientAcceptAndReply(conversationId: String) {
        val conv = _conversations.value.find { it.id == conversationId } ?: return
        acceptMessageRequest(conversationId)
        val otherSender = UserProfile(
            uid = conv.otherUserUid.ifBlank { "creator_${conv.otherUsername}" },
            username = conv.otherUsername,
            displayName = conv.otherDisplayName.ifBlank { conv.otherUsername },
            avatarUrl = conv.otherAvatarUrl
        )
        sendMessage(
            conversationId = conversationId,
            sender = otherSender,
            text = "Salam! ✨ I accepted your Tashan Chat request. We can now chat, send voice notes, stickers & call!",
            mediaType = "TEXT"
        )
    }

    fun deleteOrDeclineConversation(conversationId: String) {
        _conversations.value = _conversations.value.filterNot { it.id == conversationId }
        try {
            firestore.collection("conversations")
                .document(conversationId)
                .delete()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete conversation in Firestore: ${e.message}")
        }
    }

    fun markConversationRead(conversationId: String) {
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversationId) conv.copy(unreadCount = 0) else conv
        }
        try {
            firestore.collection("conversations").document(conversationId).update("unreadCount", 0)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to mark conversation read: ${e.message}")
        }
    }
}
