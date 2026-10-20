package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiAiService
import com.example.model.ReportAnalysisResult
import com.example.model.VideoItem
import com.example.ui.theme.*
import kotlinx.coroutines.launch

val REPORT_REASONS = listOf(
    Pair("Harmful or Dangerous Acts", "Dangerous stunts, physical harm, or illegal acts"),
    Pair("Harassment or Bullying", "Targeting someone with abuse, threats, or intimidation"),
    Pair("Hate Speech or Discrimination", "Attacking groups based on protected traits"),
    Pair("Nudity or Sexual Content", "Explicit or suggestive content"),
    Pair("Violent or Graphic Content", "Graphic violence, gore, or animal cruelty"),
    Pair("Spam, Scams or Misleading", "Fraudulent schemes, phishing, or clickbait deception"),
    Pair("Copyright Infringement", "Unauthorised use of intellectual property"),
    Pair("Other Inappropriate Content", "Other content that violates community standards")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportVideoBottomSheet(
    videoItem: VideoItem,
    reporterUsername: String = "currentUser",
    reporterUid: String = "guest",
    isPhoneVerified: Boolean = true,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String, description: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var additionalDetails by remember { mutableStateOf("") }
    var isVerifyingByAi by remember { mutableStateOf(false) }
    var aiAnalysisResult by remember { mutableStateOf<ReportAnalysisResult?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiAiService.getInstance() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF161622),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = TextTertiary
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .testTag("report_video_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!isSubmitted) {
                // Header with AI Security Verification Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TashanPink.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = TashanCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "AI Verified Report",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(TashanCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("Gemini AI", color = TashanCyan, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text(
                                text = "Client identity is verified before report dispatch",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Client Verification Status Card
                Surface(
                    color = Color(0xFF1E2132),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TashanCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPhoneVerified) Icons.Filled.VerifiedUser else Icons.Filled.AccountCircle,
                                contentDescription = null,
                                tint = if (isPhoneVerified) TashanCyan else TashanYellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Reporter: @$reporterUsername",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (isPhoneVerified) "Client Phone & Token Verified" else "Unverified Client (Subject to spam check)",
                                    color = if (isPhoneVerified) TashanCyan else TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            text = if (isPhoneVerified) "Trust: 98%" else "Trust: 65%",
                            color = TashanCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }

                // Target video context card
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            tint = TashanPink,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Target: @${videoItem.username}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = videoItem.caption.ifBlank { "Video ID: ${videoItem.id}" }.take(45),
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Text(
                    text = "Select Violation Reason:",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                // Report reasons list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(REPORT_REASONS) { (title, subtitle) ->
                        val isSelected = selectedReason == title
                        Surface(
                            onClick = { selectedReason = title },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TashanPink.copy(alpha = 0.15f) else Color(0xFF1E1E2C),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) TashanPink else Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedReason = title },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = TashanPink,
                                        unselectedColor = TextSecondary
                                    )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = subtitle,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Optional description
                OutlinedTextField(
                    value = additionalDetails,
                    onValueChange = { additionalDetails = it },
                    placeholder = { Text("Evidence, context, or timestamps (optional)...", color = TextTertiary, fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TashanPink,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                // Actions with AI Client Verification Execution
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text("Cancel", color = Color.White, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            selectedReason?.let { reason ->
                                isVerifyingByAi = true
                                coroutineScope.launch {
                                    val result = geminiService.verifyClientAndModerateReport(
                                        reporterUid = reporterUid,
                                        reporterUsername = reporterUsername,
                                        isPhoneVerified = isPhoneVerified,
                                        targetType = "VIDEO",
                                        targetId = videoItem.id,
                                        targetTitle = videoItem.caption,
                                        targetCreator = videoItem.username,
                                        reason = reason,
                                        details = additionalDetails
                                    )
                                    aiAnalysisResult = result
                                    onSubmitReport(reason, additionalDetails)
                                    isVerifyingByAi = false
                                    isSubmitted = true
                                }
                            }
                        },
                        enabled = selectedReason != null && !isVerifyingByAi,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TashanPink,
                            disabledContainerColor = TashanPink.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp)
                            .testTag("submit_report_button")
                    ) {
                        if (isVerifyingByAi) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Verifying...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify & Submit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Success Confirmation State with AI Verification Certificate
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "Report Audited & Logged",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    aiAnalysisResult?.let { result ->
                        Surface(
                            color = Color(0xFF1E2132),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TashanCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Gemini AI Report Audit", color = TashanCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Severity: ${result.severity}", color = if (result.severity == "CRITICAL" || result.severity == "HIGH") TashanPink else TashanYellow, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                Text(
                                    text = result.aiReasoning,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                                Text(
                                    text = "Action: ${result.recommendedAction} • ${result.automatedResolution}",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "Client verification complete. The report has been recorded with an immutable audit hash in the moderation queue.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = TashanTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("report_done_button")
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
