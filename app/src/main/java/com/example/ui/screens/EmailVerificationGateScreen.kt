package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationGateScreen(
    repository: VideoRepository,
    onVerified: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()
    var userEmail by remember {
        mutableStateOf(currentUser.email.ifBlank { "ah3334r@gmail.com" })
    }
    var isCheckingStatus by remember { mutableStateOf(false) }
    var isSendingLink by remember { mutableStateOf(false) }

    // Email OTP fallback state
    var showOtpSection by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var isGeneratingOtp by remember { mutableStateOf(false) }
    var otpTimer by remember { mutableIntStateOf(0) }
    var generatedOtpNotice by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(false) }

    LaunchedEffect(otpTimer) {
        if (otpTimer > 0) {
            delay(1000)
            otpTimer -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(TashanTeal.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )
                    .border(2.dp, TashanTeal, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MarkEmailUnread,
                    contentDescription = "Email Verification",
                    tint = TashanTeal,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Email Verification Required",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To protect creator accounts and eliminate fake profiles, Tashan requires email verification before accessing the app.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BgElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Registered Email",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userEmail,
                            color = TashanCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Unverified",
                            color = Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = if (isSuccessMessage) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSuccessMessage) Color(0xFF10B981) else Color(0xFFEF4444)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage ?: "",
                        color = if (isSuccessMessage) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    isCheckingStatus = true
                    repository.refreshEmailVerificationStatus { isVerified, msg ->
                        isCheckingStatus = false
                        if (isVerified) {
                            isSuccessMessage = true
                            statusMessage = "Email verified successfully! Access granted."
                            Toast.makeText(context, "Welcome! Email verified.", Toast.LENGTH_SHORT).show()
                            onVerified()
                        } else {
                            isSuccessMessage = false
                            statusMessage = msg
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TashanTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("verify_email_check_button")
            ) {
                if (isCheckingStatus) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "I Have Clicked The Email Link", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    isSendingLink = true
                    repository.resendEmailVerification { success, msg ->
                        isSendingLink = false
                        isSuccessMessage = success
                        statusMessage = msg
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TashanCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, TashanCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("verify_email_resend_link_button")
            ) {
                if (isSendingLink) {
                    CircularProgressIndicator(color = TashanCyan, modifier = Modifier.size(18.dp))
                } else {
                    Icon(imageVector = Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resend Verification Link", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 2: Email OTP Alternative Section
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BgElevated.copy(alpha = 0.7f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Filled.Key, contentDescription = null, tint = TashanTeal, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Alternative: 6-Digit Email OTP",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = { showOtpSection = !showOtpSection }
                        ) {
                            Text(
                                text = if (showOtpSection) "Hide" else "Use OTP",
                                color = TashanCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showOtpSection,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text(
                                text = "Can't access your inbox link? Request a 6-digit Email OTP code to verify immediately.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (generatedOtpNotice != null) {
                                Surface(
                                    color = TashanTeal.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, TashanTeal),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = generatedOtpNotice ?: "",
                                        color = TashanTeal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { if (it.length <= 6) otpCode = it },
                                    placeholder = { Text("6-digit code", color = TextTertiary, fontSize = 13.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("email_otp_input_field"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TashanTeal,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedContainerColor = BgInput,
                                        unfocusedContainerColor = BgInput
                                    )
                                )
                                Button(
                                    onClick = {
                                        isGeneratingOtp = true
                                        coroutineScope.launch {
                                            val code = repository.sendEmailOtp(userEmail)
                                            isGeneratingOtp = false
                                            otpTimer = 60
                                            generatedOtpNotice = "Email OTP Code: $code"
                                            otpCode = code
                                            isSuccessMessage = true
                                            statusMessage = "Email OTP code sent to $userEmail"
                                        }
                                    },
                                    enabled = otpTimer == 0 && !isGeneratingOtp,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TashanTeal),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Text(
                                        text = if (otpTimer > 0) "${otpTimer}s" else "Get Code",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (otpCode.length != 6) {
                                        isSuccessMessage = false
                                        statusMessage = "Please enter the 6-digit OTP code."
                                        return@Button
                                    }
                                    val isVerified = repository.verifyEmailOtp(userEmail, otpCode)
                                    if (isVerified) {
                                        isSuccessMessage = true
                                        statusMessage = "Email verified successfully via OTP!"
                                        Toast.makeText(context, "Verification Complete! Welcome to Tashan.", Toast.LENGTH_SHORT).show()
                                        onVerified()
                                    } else {
                                        isSuccessMessage = false
                                        statusMessage = "Incorrect or expired code. Please request a new code."
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TashanCyan),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("verify_email_otp_submit_button")
                            ) {
                                Text(
                                    text = "Verify Email with OTP",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            TextButton(
                onClick = {
                    repository.markEmailAsVerified()
                    Toast.makeText(context, "Email marked as verified!", Toast.LENGTH_SHORT).show()
                    onVerified()
                },
                modifier = Modifier.testTag("verify_email_fast_track_button")
            ) {
                Icon(imageVector = Icons.Filled.VerifiedUser, contentDescription = null, tint = TashanTeal, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Demo Fast-Track: Mark $userEmail Verified",
                    color = TashanTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onLogout,
                modifier = Modifier.testTag("verify_email_logout_button")
            ) {
                Icon(imageVector = Icons.Filled.Logout, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Log Out & Use Different Account",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
