package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoRepository

import com.example.model.VideoItem
import com.example.ui.components.CommentBottomSheet
import com.example.ui.components.ReportVideoBottomSheet
import com.example.ui.components.ShareBottomSheet
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class FeedCategory(
    val tag: String,
    val label: String,
    val icon: String
)

val FEED_CATEGORIES = listOf(
    FeedCategory("all", "All", "✨"),
    FeedCategory("Trending", "Trending", "🔥"),
    FeedCategory("Pakistan", "Pakistan", "🇵🇰"),
    FeedCategory("Cricket", "Cricket", "🏏"),
    FeedCategory("FoodiePK", "Foodie PK", "🥘"),
    FeedCategory("Comedy", "Urdu Comedy", "😂"),
    FeedCategory("Music", "Music", "🎵"),
    FeedCategory("Tech", "Tech", "⚡"),
    FeedCategory("Travel", "Travel PK", "🏔️")
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    repository: VideoRepository,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allVideos by repository.videos.collectAsState()
    val commentsMap by repository.comments.collectAsState()
    val isOfflineMode by repository.isOfflineMode.collectAsState()
    val currentUser by repository.currentUser.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("foryou") }
    var selectedCategory by remember { mutableStateOf("all") }
    var selectedVideoForComments by remember { mutableStateOf<VideoItem?>(null) }
    var selectedVideoForReport by remember { mutableStateOf<VideoItem?>(null) }
    var selectedVideoForShare by remember { mutableStateOf<VideoItem?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchFilterType by remember { mutableStateOf("all") }

    val displayedVideos = remember(allVideos, searchQuery, activeTab, searchFilterType, selectedCategory) {
        val baseList = if (activeTab == "following") {
            allVideos.filter { it.isFollowing }
        } else {
            allVideos
        }

        val categoryFiltered = if (selectedCategory == "all") {
            baseList
        } else {
            baseList.filter { video ->
                video.category.equals(selectedCategory, ignoreCase = true) ||
                video.hashtags.any { it.equals(selectedCategory, ignoreCase = true) } ||
                video.caption.contains(selectedCategory, ignoreCase = true)
            }
        }

        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { video ->
                when (searchFilterType) {
                    "title" -> video.caption.lowercase().contains(query)
                    "creator" -> video.username.lowercase().contains(query)
                    else -> video.caption.lowercase().contains(query) ||
                            video.username.lowercase().contains(query) ||
                            video.hashtags.any { it.lowercase().contains(query) }
                }
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { displayedVideos.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedCategory, activeTab) {
        if (displayedVideos.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }




    var showAudioPlayer by remember { mutableStateOf(false) }
    var refreshToastMessage by remember { mutableStateOf<String?>(null) }

    val onRefresh: () -> Unit = {
        coroutineScope.launch {
            isRefreshing = true
            repository.refreshFeed()
            isRefreshing = false
            refreshToastMessage = "Latest content reloaded from Firestore"
            kotlinx.coroutines.delay(2200)
            refreshToastMessage = null
        }
    }



    val isFeedLoading by repository.isFeedLoading.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().testTag("feed_pull_to_refresh_box"),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = rememberPullToRefreshState(),
                    isRefreshing = isRefreshing,
                    containerColor = Color(0xFF1E1E2E),
                    color = TashanCyan,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = if (isSearchExpanded) 120.dp else 60.dp)
                )
            }
        ) {
            if (isFeedLoading) {
                FeedLoadingSkeleton()
            } else if (displayedVideos.isNotEmpty()) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val video = displayedVideos[page]
                    val isActive = pagerState.currentPage == page
                    VideoPlayerView(
                        videoItem = video,
                        isActive = isActive,
                        onLikeToggle = { repository.toggleLike(video.id) },
                        onSaveToggle = { repository.toggleSave(video.id) },
                        onFollowToggle = { repository.toggleFollow(video.uid) },
                        onCommentClick = { selectedVideoForComments = video },
                        onShareClick = { selectedVideoForShare = video },
                        onReportClick = { selectedVideoForReport = video },
                        onVotePoll = { optionIdx -> repository.votePoll(video.id, optionIdx) },
                        onViewWatched = { repository.recordVideoView(video.id) },
                        onWatchDwell = { vidId, secs -> repository.recordWatchDwell(vidId, secs) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (searchQuery.isNotBlank()) Icons.Filled.Search else Icons.Filled.VideoLibrary,
                            contentDescription = null,
                            tint = TashanCyan.copy(alpha = 0.8f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No videos found for \"$searchQuery\"" else "No videos on the server yet",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching with a different title, creator @username, or hashtag." else "Server videos will appear here as soon as they are uploaded. Tap below to refresh or publish your first video!",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        searchQuery = ""
                                    } else {
                                        coroutineScope.launch {
                                            repository.refreshFeed()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("feed_empty_action_button")
                            ) {
                                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (searchQuery.isNotBlank()) "Show All Videos" else "Check Server", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Top Navigation and Search Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            AnimatedVisibility(
                visible = !isSearchExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        // Center: Mathematically Centered Tabs (Following | For You)
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { activeTab = "following" }
                                    .testTag("feed_tab_following")
                            ) {
                                Text(
                                    text = "Following",
                                    color = if (activeTab == "following") Color.White else Color.White.copy(alpha = 0.60f),
                                    fontWeight = if (activeTab == "following") FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(2.5.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (activeTab == "following") Color.White else Color.Transparent)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { activeTab = "foryou" }
                                    .testTag("feed_tab_foryou")
                            ) {
                                Text(
                                    text = "For You",
                                    color = if (activeTab == "foryou") Color.White else Color.White.copy(alpha = 0.60f),
                                    fontWeight = if (activeTab == "foryou") FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(2.5.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (activeTab == "foryou") Color.White else Color.Transparent)
                                )
                            }
                        }

                        // Right: Perfectly Sized & Styled Action Icons (Notifications, Search)
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // 2. Notifications Bell with Vibrant Green Badge
                            Surface(
                                onClick = onOpenNotifications,
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("feed_notifications_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = "Notifications",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    // Vibrant Green Notification Badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-3).dp, y = 3.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(NotificationGreen)
                                            .border(1.dp, Color.Black, CircleShape)
                                    )
                                }
                            }

                            // 3. Search Videos Button
                            Surface(
                                onClick = { isSearchExpanded = true },
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("feed_search_toggle_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Search Videos",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141420).copy(alpha = 0.95f))
                        .padding(10.dp)
                        .testTag("feed_search_bar")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Search by title or creator...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = TashanCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.testTag("feed_search_clear_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E1E2E),
                                unfocusedContainerColor = Color(0xFF1E1E2E),
                                focusedBorderColor = TashanCyan,
                                unfocusedBorderColor = Color(0xFF33334D),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("feed_search_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                isSearchExpanded = false
                                searchQuery = ""
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .testTag("feed_search_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close Search",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter by:",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        SearchFilterChip(
                            label = "All",
                            isSelected = searchFilterType == "all",
                            onClick = { searchFilterType = "all" }
                        )
                        SearchFilterChip(
                            label = "Title",
                            isSelected = searchFilterType == "title",
                            onClick = { searchFilterType = "title" }
                        )
                        SearchFilterChip(
                            label = "Creator (@)",
                            isSelected = searchFilterType == "creator",
                            onClick = { searchFilterType = "creator" }
                        )
                    }

                    if (searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Indexed results: ${displayedVideos.size} video(s) found",
                            color = TashanCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }



        selectedVideoForComments?.let { video ->
            val comments = commentsMap[video.id] ?: emptyList()
            CommentBottomSheet(
                video = video,
                comments = comments,
                onAddComment = { text -> repository.addComment(video.id, text) },
                onLikeComment = { commentId -> repository.toggleLikeComment(video.id, commentId) },
                onDismiss = { selectedVideoForComments = null }
            )
        }

        selectedVideoForReport?.let { video ->
            ReportVideoBottomSheet(
                videoItem = video,
                reporterUsername = currentUser.username,
                reporterUid = currentUser.uid,
                isPhoneVerified = currentUser.isPhoneVerified,
                onDismiss = { selectedVideoForReport = null },
                onSubmitReport = { reason, description ->
                    repository.reportVideo(
                        videoId = video.id,
                        reason = reason,
                        description = description
                    )
                }
            )
        }

        selectedVideoForShare?.let { video ->
            ShareBottomSheet(
                videoItem = video,
                onDismiss = { selectedVideoForShare = null },
                onRepost = { repository.toggleLike(video.id) }
            )
        }

        AnimatedVisibility(
            visible = showAudioPlayer,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp)
        ) {
            com.example.ui.components.AudioPlayerBottomBar(
                audioPlayerManager = repository.audioPlayerManager
            )
        }
    }
}

@Composable
private fun SearchFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) TashanCyan else Color(0xFF2A2A3E))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun FeedLoadingSkeleton() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Placeholder for video content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFF121212))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TashanCyan, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading Feed...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }
        }

        // Sidebar Skeleton
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Bottom Info Skeleton
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 40.dp, end = 80.dp)
        ) {
            Box(modifier = Modifier.width(120.dp).height(16.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.width(200.dp).height(12.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
        }
    }
}
