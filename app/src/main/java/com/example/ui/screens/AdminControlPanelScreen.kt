package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.VideoRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlPanelScreen(
    videoRepository: VideoRepository,
    onNavigateBack: () -> Unit
) {
    val adminControl by videoRepository.adminSystemControl.collectAsState()
    val context = LocalContext.current

    var pkrRate by remember { mutableStateOf(adminControl.monetizationPkrPer1000Views.toString()) }
    var isGiftingLocked by remember { mutableStateOf(adminControl.isGiftingAndPurchasesLocked) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👑 System Admin Governance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "💰 Creator Monetization Rules",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pkrRate,
                onValueChange = { pkrRate = it },
                label = { Text("PKR Rate per 1000 Views") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔒 Lock Gifting & Coin Deposits",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isGiftingLocked,
                    onCheckedChange = { isGiftingLocked = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val newRate = pkrRate.toDoubleOrNull() ?: 10.0
                    videoRepository.updateAdminControl(
                        adminControl.copy(
                            monetizationPkrPer1000Views = newRate,
                            isGiftingAndPurchasesLocked = isGiftingLocked
                        )
                    )
                    Toast.makeText(context, "Admin Settings Saved!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Admin System Settings")
            }
        }
    }
}
