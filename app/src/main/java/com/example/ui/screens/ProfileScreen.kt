package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.VideoRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    videoRepository: VideoRepository,
    onNavigateToAdmin: () -> Unit
) {
    val user by videoRepository.currentUser.collectAsState()
    val adminControl by videoRepository.adminSystemControl.collectAsState()
    val pkrEarnings = user.calculateViewEarningsPkr(adminControl.monetizationPkrPer1000Views)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("👤 My Creator Profile") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💼 Creator Wallet & Revenue",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Total Views: ${user.totalViews}")
                    Text(text = "Monetization Rate: PKR ${adminControl.monetizationPkrPer1000Views} / 1000 views")
                    Text(
                        text = "Estimated Earnings: PKR ${String.format("%.2f", pkrEarnings)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNavigateToAdmin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("👑 Admin System Control")
            }
        }
    }
}
