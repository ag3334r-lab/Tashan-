package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.VideoRepository
import com.example.ui.screens.AdminControlPanelScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.ProfileScreen

enum class AppScreen {
    FEED, PROFILE, ADMIN
}

class MainActivity : ComponentActivity() {
    private val videoRepository = VideoRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.FEED) }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.FEED,
                                onClick = { currentScreen = AppScreen.FEED },
                                label = { Text("Feed") },
                                icon = { Text("🔥") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.PROFILE,
                                onClick = { currentScreen = AppScreen.PROFILE },
                                label = { Text("Profile") },
                                icon = { Text("👤") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.ADMIN,
                                onClick = { currentScreen = AppScreen.ADMIN },
                                label = { Text("Admin") },
                                icon = { Text("👑") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            AppScreen.FEED -> FeedScreen(
                                videoRepository = videoRepository,
                                onNavigateToAdmin = { currentScreen = AppScreen.ADMIN }
                            )
                            AppScreen.PROFILE -> ProfileScreen(
                                videoRepository = videoRepository,
                                onNavigateToAdmin = { currentScreen = AppScreen.ADMIN }
                            )
                            AppScreen.ADMIN -> AdminControlPanelScreen(
                                videoRepository = videoRepository,
                                onNavigateBack = { currentScreen = AppScreen.FEED }
                            )
                        }
                    }
                }
            }
        }
    }
}
