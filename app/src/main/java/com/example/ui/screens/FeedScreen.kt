package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.VideoRepository

@Composable
fun FeedScreen(
    videoRepository: VideoRepository,
    onNavigateToAdmin: () -> Unit
) {
    val videos by videoRepository.videos.collectAsState()
    val adminControl by videoRepository.adminSystemControl.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🔥 Tashan Short Video Feed",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (adminControl.isGiftingAndPurchasesLocked) {
                Surface(
                    color = Color(0xFF332200),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 Trailer Mode: Coin Purchases & Gifting Locked by Admin",
                        color = Color(0xFFFFCC00),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Videos Loaded: ${videos.size}",
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onNavigateToAdmin) {
                Text("👑 Open Admin Dashboard")
            }
        }
    }
}
