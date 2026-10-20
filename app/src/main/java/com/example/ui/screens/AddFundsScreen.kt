package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoRepository
import com.example.model.PaymentGatewayRequest
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFundsScreen(
    repository: VideoRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by repository.currentUser.collectAsState()
    val adminControl by repository.adminSystemControl.collectAsState()
    val paymentRequests by repository.paymentRequests.collectAsState()
    val scope = rememberCoroutineScope()

    var activeMode by remember { mutableStateOf("REAL_CHECK") }

    var selectedAmount by remember { mutableIntStateOf(500) }
    var selectedMethod by remember { mutableStateOf("JazzCash") }
    var accountNo by remember { mutableStateOf("0300-1234567") }
    var paypalEmail by remember { mutableStateOf("") }
    var accountTitle by remember { mutableStateOf(currentUser.displayName.ifBlank { "Tashan Creator" }) }
    var realTrxIdInput by remember { mutableStateOf("") }

    var isProcessing by remember { mutableStateOf(false) }
    var processingStep by remember { mutableStateOf(0) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showTrailerLockDialog by remember { mutableStateOf(false) }
    var generatedTrxId by remember { mutableStateOf("") }

    val amountOptions = listOf(100, 500, 1000, 2000, 5000, 10000)
    val pendingCount = paymentRequests.count { it.status == "PENDING" }

    LaunchedEffect(selectedMethod) {
        accountNo = when (selectedMethod) {
            "JazzCash" -> "0300-1234567"
            "EasyPaisa" -> "0345-9876543"
            else -> ""
        }
        paypalEmail = when (selectedMethod) {
            "PayPal" -> "${currentUser.username.lowercase()}@example.com"
            else -> ""
        }
        if (realTrxIdInput.isEmpty()) {
            realTrxIdInput = when (selectedMethod) {
                "JazzCash" -> "JC-${(10000000000L..99999999999L).random()}"
                "EasyPaisa" -> "EP-${(1000000000L..9999999999L).random()}"
                else -> "PP-${(10000000L..99999999L).random()}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Add Funds & Coins",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        if (selectedMethod == "JazzCash") {
                            JazzCashRealLogo(size = 22.dp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("add_funds_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F111E)
                )
            )
        },
        containerColor = Color(0xFF0F111E),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Trailer Mode Admin Lock Top Banner
                if (adminControl.isGiftingAndPurchasesLocked) {
                    Surface(
                        color = Color(0xFF2A1519),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF453A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🔒", fontSize = 22.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Coin Purchases & Wallet Top-up Locked (Trailer Mode)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(adminControl.giftingLockNoticeMessage, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Wallet Balance Summary Header Card
                Surface(
                    color = Color(0xFF16192B),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, TashanYellow.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TashanYellow.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🪙", fontSize = 20.sp)
                                }
                                Column {
                                    Text("Tashan Coin Balance", color = Color.Gray, fontSize = 11.sp)
                                    Text(
                                        text = "${currentUser.coins} Coins",
                                        color = TashanYellow,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Available PKR", color = Color.Gray, fontSize = 11.sp)
                                Text(
                                    text = "PKR ${currentUser.balancePkr.toInt()}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF232742))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Standard Exchange Rate: 1 PKR = 5 Coins",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Instant Top-Up",
                                color = TashanCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Mode Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF16192B), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = if (activeMode == "REAL_CHECK") JazzCashRed else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeMode = "REAL_CHECK" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Real Transaction Check", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        color = if (activeMode == "INSTANT") TashanCyan.copy(alpha = 0.25f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        border = if (activeMode == "INSTANT") BorderStroke(1.dp, TashanCyan) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeMode = "INSTANT" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Instant Simulation", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Step 1: Select Amount to Add
                Text(
                    text = "1. Select Deposit Amount (PKR)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val chunked = amountOptions.chunked(3)
                    for (row in chunked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (amount in row) {
                                val isSelected = selectedAmount == amount
                                val equivalentCoins = amount * 5
                                Surface(
                                    color = if (isSelected) JazzCashRed.copy(alpha = 0.15f) else Color(0xFF16192B),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSelected) JazzCashRed else Color(0xFF232742)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedAmount = amount }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "PKR $amount",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "+$equivalentCoins",
                                            color = TashanYellow,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 2: Select Gateway Channel
                Text(
                    text = "2. Select Official Payment Gateway",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isJazzSelected = selectedMethod == "JazzCash"
                    Surface(
                        color = if (isJazzSelected) Color(0xFF280B0D) else Color(0xFF16192B),
                        border = BorderStroke(
                            2.dp,
                            if (isJazzSelected) JazzCashRed else Color(0xFF232742)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMethod = "JazzCash" }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            JazzCashRealLogo(size = 36.dp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Jazz", color = JazzCashRed, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Text("Cash", color = JazzCashGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            if (isJazzSelected) {
                                Text("ACTIVE", color = JazzCashRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    val isEasySelected = selectedMethod == "EasyPaisa"
                    Surface(
                        color = if (isEasySelected) Color(0xFF072314) else Color(0xFF16192B),
                        border = BorderStroke(
                            2.dp,
                            if (isEasySelected) EasyPaisaGreen else Color(0xFF232742)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMethod = "EasyPaisa" }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            EasyPaisaRealLogo(size = 36.dp)
                            Text("easypaisa", color = EasyPaisaGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (isEasySelected) {
                                Text("ACTIVE", color = EasyPaisaGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    val isPaypalSelected = selectedMethod == "PayPal"
                    Surface(
                        color = if (isPaypalSelected) Color(0xFF04192F) else Color(0xFF16192B),
                        border = BorderStroke(
                            2.dp,
                            if (isPaypalSelected) PayPalBlue else Color(0xFF232742)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedMethod = "PayPal" }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PayPalRealLogo(size = 36.dp)
                            Text("PayPal", color = PayPalBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (isPaypalSelected) {
                                Text("ACTIVE", color = PayPalBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // REAL TRANSACTION CHECK SECTION
                if (activeMode == "REAL_CHECK") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16192B)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, JazzCashRed.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GatewayLogoBadge(gateway = selectedMethod, size = 26.dp)
                                Text(
                                    text = "Official $selectedMethod Deposit Instructions",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Surface(
                                color = Color(0xFF0F111E),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Official Merchant Till:", color = Color.Gray, fontSize = 11.sp)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.clickable {
                                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cb.setPrimaryClip(ClipData.newPlainText("Till", "03001234567"))
                                                Toast.makeText(context, "Copied Merchant Till Number!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text("0300-1234567", color = TashanCyan, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TashanCyan, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Account Title:", color = Color.Gray, fontSize = 11.sp)
                                        Text("Tashan Media Ent. Ltd", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Amount to Send:", color = Color.Gray, fontSize = 11.sp)
                                        Text("PKR $selectedAmount", color = JazzCashGold, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    }
                                }
                            }

                            Text(
                                text = "3. Enter Real Transaction ID (TID) from SMS Receipt",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            OutlinedTextField(
                                value = realTrxIdInput,
                                onValueChange = { realTrxIdInput = it.uppercase() },
                                label = { Text("Transaction ID / TID (e.g. JC-84920194827)", color = Color.Gray) },
                                leadingIcon = {
                                    Icon(Icons.Default.Receipt, contentDescription = "Receipt", tint = JazzCashGold)
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            realTrxIdInput = "JC-${(10000000000L..99999999999L).random()}"
                                        }
                                    ) {
                                        Icon(Icons.Default.Autorenew, contentDescription = "Generate Sample", tint = TashanCyan)
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = JazzCashRed,
                                    unfocusedBorderColor = Color(0xFF232742),
                                    focusedContainerColor = Color(0xFF0F111E),
                                    unfocusedContainerColor = Color(0xFF0F111E)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = accountNo,
                                onValueChange = { accountNo = it },
                                label = { Text("Your $selectedMethod Mobile Number", color = Color.Gray) },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone", tint = TashanYellow) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = TashanYellow,
                                    unfocusedBorderColor = Color(0xFF232742),
                                    focusedContainerColor = Color(0xFF0F111E),
                                    unfocusedContainerColor = Color(0xFF0F111E)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = accountTitle,
                                onValueChange = { accountTitle = it },
                                label = { Text("Your Account Name / Title", color = Color.Gray) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = Color.LightGray) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = TashanPink,
                                    unfocusedBorderColor = Color(0xFF232742),
                                    focusedContainerColor = Color(0xFF0F111E),
                                    unfocusedContainerColor = Color(0xFF0F111E)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            var isAiAuditing by remember { mutableStateOf(false) }
                            val coroutineScope = rememberCoroutineScope()

                            Button(
                                onClick = {
                                    if (adminControl.isGiftingAndPurchasesLocked) {
                                        showTrailerLockDialog = true
                                        return@Button
                                    }
                                    if (realTrxIdInput.trim().isEmpty()) {
                                        Toast.makeText(context, "Please enter a Transaction ID (TID)!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (accountNo.length < 8) {
                                        Toast.makeText(context, "Please enter your sender mobile number!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isAiAuditing = true
                                    coroutineScope.launch {
                                        repository.aiAuditAndProcessPayment(
                                            gateway = selectedMethod,
                                            amountPkr = selectedAmount.toDouble(),
                                            coins = selectedAmount * 5,
                                            senderAccount = accountNo,
                                            trxId = realTrxIdInput.trim(),
                                            autoCreditIfApproved = true
                                        ) { result ->
                                            isAiAuditing = false
                                            if (result.isVerified) {
                                                Toast.makeText(context, "AI Verified & Approved! ${selectedAmount * 5} Coins Credited to Wallet.", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "AI Notice: ${result.aiExplanation}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isAiAuditing,
                                colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("ai_instant_verify_payment_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isAiAuditing) {
                                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Text("Gemini AI Auditing TRX...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black)
                                        Text(
                                            text = "AI Instant Verify & Credit (Gemini)",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    if (realTrxIdInput.trim().isEmpty()) {
                                        Toast.makeText(context, "Please enter a Transaction ID (TID)!", Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }
                                    if (accountNo.length < 8) {
                                        Toast.makeText(context, "Please enter your sender mobile number!", Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }
                                    repository.submitPaymentGatewayRequest(
                                        gateway = selectedMethod,
                                        accountTitle = accountTitle,
                                        accountNumberOrIban = accountNo,
                                        amountCoins = selectedAmount * 5,
                                        amountPkr = selectedAmount.toDouble(),
                                        trxId = realTrxIdInput.trim()
                                    ) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                border = BorderStroke(1.dp, JazzCashRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = JazzCashRed, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Queue for Manual Admin Audit",
                                        color = Color.White,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16192B)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF232742)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GatewayLogoBadge(gateway = selectedMethod, size = 28.dp)
                                Text(
                                    text = "$selectedMethod Quick Deposit",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            HorizontalDivider(color = Color(0xFF232742))
                            if (selectedMethod == "PayPal") {
                                OutlinedTextField(
                                    value = paypalEmail,
                                    onValueChange = { paypalEmail = it },
                                    label = { Text("PayPal Secure Email Address", color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = TashanCyan) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OutlinedTextField(
                                    value = accountNo,
                                    onValueChange = { accountNo = it },
                                    label = { Text("$selectedMethod Mobile Number", color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone", tint = TashanYellow) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        processingStep = 1
                                        delay(1000)
                                        processingStep = 2
                                        delay(1200)
                                        processingStep = 3
                                        delay(1000)
                                        val equivCoins = selectedAmount * 5
                                        val genTrx = "TRX-${(10000000000L..99999999999L).random()}"
                                        generatedTrxId = genTrx
                                        repository.submitPaymentGatewayRequest(
                                            gateway = selectedMethod,
                                            accountTitle = accountTitle,
                                            accountNumberOrIban = if (selectedMethod == "PayPal") paypalEmail else accountNo,
                                            amountCoins = equivCoins,
                                            amountPkr = selectedAmount.toDouble(),
                                            trxId = genTrx
                                        ) { success, msg -> }
                                        isProcessing = false
                                        showSuccessDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (selectedMethod) {
                                        "JazzCash" -> JazzCashRed
                                        "EasyPaisa" -> EasyPaisaGreen
                                        else -> PayPalBlue
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                                    Text(
                                        text = "Add PKR $selectedAmount via $selectedMethod",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Transaction Status & History
                Text(
                    text = "Real Transaction Status (Admin Desk)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Surface(
                    color = Color(0xFF16192B),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF252A48)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pending Review: $pendingCount",
                                color = if (pendingCount > 0) JazzCashGold else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (paymentRequests.isEmpty()) {
                            Text("No transactions yet.", color = Color.Gray, fontSize = 12.sp)
                        } else {
                            paymentRequests.take(4).forEach { req ->
                                TransactionStatusRow(request = req)
                            }
                        }
                    }
                }
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = JazzCashRed,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = when (processingStep) {
                                1 -> "Establishing Secure Connection to $selectedMethod..."
                                2 -> "Awaiting Payout Consent Authorization from $accountTitle..."
                                else -> "Validating Deposit Receipt and Minting Tashan Coins..."
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Do not close this application or go back.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false; onBack() },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00C853).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF00C853),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Deposit Request Submitted!",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Your request of PKR $selectedAmount has been submitted with ID $generatedTrxId. Once verified by admin, +${selectedAmount * 5} coins will be credited to your wallet.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFF0F111E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Transaction ID:", color = Color.Gray, fontSize = 11.sp)
                                Text(generatedTrxId, color = TashanCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Account Title:", color = Color.Gray, fontSize = 11.sp)
                                Text(accountTitle, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Status:", color = Color.Gray, fontSize = 11.sp)
                                Text("PENDING VERIFICATION", color = TashanYellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Coins to Credit:", color = Color.Gray, fontSize = 11.sp)
                                Text("+${selectedAmount * 5}", color = TashanYellow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TashanPink)
                ) {
                    Text("Awesome, thanks!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF16192B)
        )
    }

    if (showTrailerLockDialog) {
        AlertDialog(
            onDismissRequest = { showTrailerLockDialog = false },
            icon = { Text("🔒", fontSize = 36.sp) },
            title = { Text("Coin Purchases Locked (Trailer Mode)", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = adminControl.giftingLockNoticeMessage,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showTrailerLockDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TashanCyan)
                ) {
                    Text("Understood", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1E202E)
        )
    }
}

@Composable
private fun TransactionStatusRow(
    request: PaymentGatewayRequest
) {
    val isApproved = request.status == "APPROVED"
    val isRejected = request.status == "REJECTED"

    Surface(
        color = Color(0xFF0F111E),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GatewayLogoBadge(gateway = request.gateway, size = 26.dp)
                Column {
                    Text(
                        text = request.transactionTrxId,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "PKR ${request.amountPkr.toInt()} (+${request.amountCoins})",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
            Surface(
                color = when {
                    isApproved -> Color(0xFF00C853).copy(alpha = 0.2f)
                    isRejected -> Color(0xFFFF5252).copy(alpha = 0.2f)
                    else -> JazzCashGold.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = when {
                        isApproved -> "Approved"
                        isRejected -> "Rejected"
                        else -> "Pending Check"
                    },
                    color = when {
                        isApproved -> Color(0xFF00E676)
                        isRejected -> Color(0xFFFF5252)
                        else -> JazzCashGold
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}
