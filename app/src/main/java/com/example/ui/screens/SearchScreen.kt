package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoRepository
import com.example.model.VideoItem

import com.example.ui.theme.*

@Composable
fun SearchScreen(
    repository: VideoRepository,
    onVideoSelected: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val videos by repository.videos.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedHashtag by remember { mutableStateOf<String?>(null) }

    val trendingHashtags = listOf("TashanVibes", "DanceRel", "FoodiePK", "TechTrends", "FitnessMotivation", "FireShow", "TravelDiaries")

    val filteredVideos = remember(searchQuery, selectedHashtag, videos) {
        videos.filter { video ->
            val matchesQuery = searchQuery.isBlank() ||
                    video.caption.contains(searchQuery, ignoreCase = true) ||
                    video.username.contains(searchQuery, ignoreCase = true)
            val matchesTag = selectedHashtag == null || video.hashtags.contains(selectedHashtag)
            matchesQuery && matchesTag
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgBase)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search videos, creators, or hashtags...", color = TextTertiary, fontSize = 14.sp) },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BgInput,
                unfocusedContainerColor = BgInput,
                focusedBorderColor = TashanTeal,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        // Trending Hashtags
        Text("Trending Hashtags", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(trendingHashtags) { tag ->
                val isSelected = selectedHashtag == tag
                Surface(
                    onClick = {
                        selectedHashtag = if (isSelected) null else tag
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) TashanTeal else BgElevated,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) TashanTeal else BorderSubtle)
                    )
                ) {
                    Text(
                        text = "#$tag",
                        color = if (isSelected) Color.White else TashanCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = BorderSubtle)



        // Video Discovery Grid
        Text("Popular Videos", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (filteredVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching videos found", color = TextTertiary, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredVideos, key = { it.id }) { video ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(9f / 16f)
                            .clickable { onVideoSelected(video) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BgElevated)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "@${video.username}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "❤️ ${video.likesCount} likes",
                                    color = TashanCyan,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }


    }
}
