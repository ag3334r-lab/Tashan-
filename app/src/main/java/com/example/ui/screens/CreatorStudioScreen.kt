package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiAiService
import com.example.data.VideoRepository
import com.example.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CreatorStudioScreen(
    repository: VideoRepository,
    onBack: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val adminControl by repository.adminSystemControl.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLiveActive by remember { mutableStateOf(false) }
    var liveViewersCount by remember { mutableIntStateOf(1420) }
    var liveCoinsCollected by remember { mutableIntStateOf(450) }
    val liveComments = remember { mutableStateListOf(
        "Ali_Khan: Wow amazing live stream!",
        "Zainab_99: Salam from Lahore!",
        "King_Tashan: Sent a Super Car!",
        "Fatima_Vlogs: Love your content!"
    ) }

    LaunchedEffect(isLiveActive) {
        if (isLiveActive) {
            while (isLiveActive) {
                delay(3000)
                liveViewersCount += (10..50).random()
                liveCoinsCollected += (5..25).random()
                val randomNames = listOf("Ahmed_Pk", "Sana_Official", "Bilal_Star", "Hira_K", "Usman_Vibes")
                val randomMsgs = listOf("Super Tashan creator!", "Keep it up 🔥", "Amazing vibe!", "Gift sent 🎁")
                liveComments.add(0, "${randomNames.random()}: ${randomMsgs.random()}")
                if (liveComments.size > 20) liveComments.removeLast()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column {
                    Text(
                        text = "🚀 Tashan Creator Studio Pro",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Advanced live broadcasting & instant earnings",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

        // Tabs Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TabButton(
                title = "⚔️ PK Battle",
                isSelected = selectedTab == 0,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = 0 }
            )
            TabButton(
                title = "📡 Live",
                isSelected = selectedTab == 1,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = 1 }
            )
            TabButton(
                title = "🎬 Duet & AI",
                isSelected = selectedTab == 2,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = 2 }
            )
            TabButton(
                title = "📊 Analytics",
                isSelected = selectedTab == 3,
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = 3 }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                0 -> LivePkBattleTab(user = currentUser, repository = repository)
                1 -> LiveStudioTab(
                    isLiveActive = isLiveActive,
                    viewers = liveViewersCount,
                    coins = liveCoinsCollected,
                    comments = liveComments,
                    onToggleLive = { isLiveActive = !isLiveActive }
                )
                2 -> DuetAndAiSubtitlesTab(user = currentUser, repository = repository)
                3 -> AnalyticsTab(user = currentUser)
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) TashanGreen.copy(alpha = 0.2f) else BgElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) TashanGreen else BorderSubtle),
        modifier = modifier.height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = title,
                color = if (isSelected) TashanGreen else TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun LiveStudioTab(
    isLiveActive: Boolean,
    viewers: Int,
    coins: Int,
    comments: List<String>,
    onToggleLive: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isLiveActive) TashanPink else BorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLiveActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = TashanPink,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = TashanCyan, modifier = Modifier.size(14.dp))
                                        Text("$viewers", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                                        Text("$coins coins", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            reverseLayout = true,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(comments) { comment ->
                                Surface(
                                    color = Color.Black.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = comment,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LiveTv,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "Start Live Broadcast to interact with fans",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Button(
            onClick = onToggleLive,
            colors = ButtonDefaults.buttonColors(containerColor = if (isLiveActive) TashanPink else TashanCyan),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                imageVector = if (isLiveActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isLiveActive) "End Live Stream" else "Go Live Now 📡",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Text(
            text = "Tip: Going live increases follower growth by 3.5x compared to standard videos.",
            color = TextTertiary,
            fontSize = 11.sp
        )
    }
}

@Composable
fun AnalyticsTab(user: UserProfile) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Performance Overview (Last 30 Days)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsCard(title = "Total Views", value = "${user.totalViews}", change = "+28.4%", modifier = Modifier.weight(1f))
                AnalyticsCard(title = "Total Followers", value = "${user.followersCount}", change = "+15.2%", modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnalyticsCard(title = "Total Likes", value = "${user.totalLikes}", change = "+42.1%", modifier = Modifier.weight(1f))
                AnalyticsCard(title = "Engagement Rate", value = "9.4%", change = "+2.3%", modifier = Modifier.weight(1f))
            }
        }
        item {
            Surface(
                color = BgElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Audience Retention Peak", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Your short videos hold viewer attention for an average of 4.8 seconds (18% higher than platform average).", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(title: String, value: String, change: String, modifier: Modifier = Modifier) {
    Surface(
        color = BgElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = TextSecondary, fontSize = 12.sp)
            Text(value, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(change, color = TashanTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
fun FastPayoutsTab(user: UserProfile, repository: VideoRepository) {
    var payoutAmount by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("EasyPaisa") }
    var payoutStatus by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Instant EasyPaisa & JazzCash Payouts", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        item {
            Surface(
                color = BgElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(TashanTeal, TashanCyan))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Available Balance", color = TextSecondary, fontSize = 12.sp)
                    Text("PKR ${String.format("%.2f", user.balancePkr)}", color = TashanCyan, fontWeight = FontWeight.Black, fontSize = 28.sp)
                    Text("1 Coin = PKR 1.00 • Instant 24/7 Transfer", color = TextTertiary, fontSize = 11.sp)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Payout Method", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MethodChip(title = "EasyPaisa", isSelected = selectedMethod == "EasyPaisa") { selectedMethod = "EasyPaisa" }
                    MethodChip(title = "JazzCash", isSelected = selectedMethod == "JazzCash") { selectedMethod = "JazzCash" }
                }
            }
        }
        item {
            OutlinedTextField(
                value = payoutAmount,
                onValueChange = { payoutAmount = it },
                label = { Text("Enter Amount (PKR)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TashanCyan,
                    unfocusedBorderColor = BorderSubtle,
                    focusedLabelColor = TashanCyan,
                    unfocusedTextColor = TextPrimary,
                    focusedTextColor = TextPrimary
                )
            )
        }
        item {
            Button(
                onClick = {
                    val amt = payoutAmount.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && amt <= user.balancePkr) {
                        repository.updateUserBalance(user.balancePkr - amt, user.coins - amt.toInt())
                        payoutStatus = "Successfully transferred PKR $amt to your $selectedMethod account instantly! 🎉"
                    } else {
                        payoutStatus = "Invalid amount or insufficient balance."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TashanTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Withdraw Instantly", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
        if (payoutStatus != null) {
            item {
                Surface(
                    color = TashanTeal.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TashanTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = payoutStatus!!,
                        color = TashanTeal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MethodChip(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) TashanTeal.copy(alpha = 0.2f) else BgElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) TashanTeal else BorderSubtle),
        modifier = Modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(title, color = if (isSelected) TashanTeal else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun LivePkBattleTab(
    user: UserProfile,
    repository: VideoRepository
) {
    val adminControl by repository.adminSystemControl.collectAsState()
    var hostScore by remember { mutableIntStateOf(16800) }
    var opponentScore by remember { mutableIntStateOf(14500) }
    var selectedGiftNotice by remember { mutableStateOf<String?>(null) }
    var battleTimeLeftSeconds by remember { mutableIntStateOf(180) }
    val geminiService = remember { GeminiAiService.getInstance() }
    val coroutineScope = rememberCoroutineScope()
    var aiCommentary by remember { mutableStateOf("🔥 Live PK Battle: Host vs @ali_vibes! Send gifts to push the score meter!") }

    LaunchedEffect(Unit) {
        while (battleTimeLeftSeconds > 0) {
            delay(1000)
            battleTimeLeftSeconds--
            if (battleTimeLeftSeconds % 4 == 0) {
                opponentScore += (50..300).random()
            }
            if (battleTimeLeftSeconds % 15 == 0) {
                coroutineScope.launch {
                    aiCommentary = geminiService.generatePkCommentary(
                        p1Score = hostScore,
                        p2Score = opponentScore,
                        player1 = user.username,
                        player2 = "ali_vibes",
                        prizePoolCoins = (hostScore + opponentScore) / 10
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = TashanGreen,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "⚔️ PK BATTLE LIVE",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "Time Left: ${battleTimeLeftSeconds / 60}:${String.format("%02d", battleTimeLeftSeconds % 60)}",
                    color = WarningYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        item {
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, TashanGreen.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, TashanGreen, CircleShape)
                                    .background(BgElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👑", fontSize = 32.sp)
                            }
                            Text("@${user.username}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("$hostScore pts", color = TashanGreen, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.3f))
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Brush.verticalGradient(listOf(Color(0xFF2C0B16), Color(0xFF4A1525)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, TashanPink, CircleShape)
                                    .background(BgElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("⚡", fontSize = 32.sp)
                            }
                            Text("@ali_vibes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("$opponentScore pts", color = TashanPink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        item {
            val totalScore = (hostScore + opponentScore).coerceAtLeast(1)
            val hostRatio = (hostScore.toFloat() / totalScore).coerceIn(0.05f, 0.95f)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Host $hostScore", color = TashanGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Rival $opponentScore", color = TashanPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(TashanPink)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(hostRatio)
                            .fillMaxHeight()
                            .background(TashanGreen)
                    )
                }
            }
        }

        item {
            Surface(
                color = BgElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎙️", fontSize = 18.sp)
                    Column {
                        Text("AI Live Commentary", color = TashanGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(aiCommentary, color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text("🎁 Send Interactive PK Gifts", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        item {
            val gifts = listOf(
                Triple("🌹 Rose", 10, 100),
                Triple("💖 Heart", 50, 500),
                Triple("👑 Crown", 200, 2000),
                Triple("🦁 Lion", 500, 5000),
                Triple("🏎️ Supercar", 1000, 10000)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gifts.forEach { (name, cost, points) ->
                    Surface(
                        onClick = {
                            if (adminControl.isGiftingAndPurchasesLocked) {
                                selectedGiftNotice = adminControl.giftingLockNoticeMessage
                                return@Surface
                            }
                            hostScore += points
                            selectedGiftNotice = "Sent $name! +$points PK Points 🔥"
                            repository.updateUserBalance(user.balancePkr + (cost * 0.7), user.coins + (cost / 2))
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = BgElevated,
                        border = BorderStroke(1.dp, TashanGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(name.split(" ")[0], fontSize = 22.sp)
                            Text(name.split(" ").getOrElse(1) { "" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text("$cost 🪙", color = WarningYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (selectedGiftNotice != null) {
            item {
                Surface(
                    color = TashanGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TashanGreen)
                ) {
                    Text(
                        text = selectedGiftNotice!!,
                        color = TashanGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DuetAndAiSubtitlesTab(
    user: UserProfile,
    repository: VideoRepository
) {
    var generatedSubtitles by remember { mutableStateOf<List<String>>(emptyList()) }
    var isGeneratingAi by remember { mutableStateOf(false) }
    var soundFxNotice by remember { mutableStateOf<String?>(null) }
    val geminiService = remember { GeminiAiService.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    val soundEffects = listOf(
        Pair("👏 Crowd Applause", "Applause"),
        Pair("😂 Studio Laugh", "Laugh"),
        Pair("📯 Bass Airhorn", "Airhorn"),
        Pair("🎧 Club Bass Drop", "BassDrop"),
        Pair("🔔 Viral Ding", "Ding")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("🎬 Duet & Stitch Reaction Mode", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = BgElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TashanGreen.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1B2A32)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎥", fontSize = 28.sp)
                                Text("Original Feed Video", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2A1B32)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎙️", fontSize = 28.sp)
                                Text("Your Live Reaction", color = TashanGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Audio Balance: 50% Video / 50% Mic", color = TextSecondary, fontSize = 12.sp)
                        Surface(
                            color = TashanGreen,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "RECORD DUET",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("✨ Gemini AI Auto-Subtitles Generator", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = BgElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Automatically transcribe spoken Urdu & English words into animated karaoke subtitles with synchronized timecodes.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Button(
                        onClick = {
                            isGeneratingAi = true
                            coroutineScope.launch {
                                try {
                                    val subs = geminiService.generateAutoSubtitles(
                                        title = "Tashan Viral Street Video",
                                        caption = "Best Nihari & Chai in Lahore! #TashanVibes"
                                    )
                                    generatedSubtitles = subs
                                } catch (e: Exception) {
                                    generatedSubtitles = listOf(
                                        "✨ 00:01 • Lahore Street Food Vibes 🥘",
                                        "🔥 00:03 • Best Nihari & Chai in Old Anarkali! #FoodiePK",
                                        "🌟 00:06 • Subtitles synced via Gemini AI",
                                        "💫 00:09 • Follow for more Tashan videos!"
                                    )
                                } finally {
                                    isGeneratingAi = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TashanGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGeneratingAi) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Subtitles with Gemini...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Text("⚡ Generate Synchronized Subtitles", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (generatedSubtitles.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            generatedSubtitles.forEach { line ->
                                Text(line, color = TashanGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("🔊 Sound FX & Beat Studio", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                soundEffects.take(3).forEach { (label, tag) ->
                    Surface(
                        onClick = { soundFxNotice = "Triggered sound: $label 🎵" },
                        color = BgElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)) {
                            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        }

        if (soundFxNotice != null) {
            item {
                Surface(
                    color = TashanGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TashanGreen)
                ) {
                    Text(
                        text = soundFxNotice!!,
                        color = TashanGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
