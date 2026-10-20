package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Official Brand Colors
val JazzCashRed = Color(0xFFE30613)
val JazzCashGold = Color(0xFFFFD100)
val JazzCashDark = Color(0xFF121212)
val EasyPaisaGreen = Color(0xFF00A651)
val EasyPaisaDark = Color(0xFF0A2E18)
val PayPalBlue = Color(0xFF0079C1)
val PayPalDarkBlue = Color(0xFF00457C)

/**
 * Authentic JazzCash Brand Logo
 */
@Composable
fun JazzCashRealLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showLabel: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.25f))
                .background(JazzCashDark)
                .border(1.dp, JazzCashRed.copy(alpha = 0.4f), RoundedCornerShape(size * 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_jazzcash_badge),
                contentDescription = "JazzCash Official Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        if (showLabel) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Jazz",
                    color = JazzCashRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Cash",
                    color = JazzCashGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * Authentic EasyPaisa Brand Logo
 */
@Composable
fun EasyPaisaRealLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showLabel: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.25f))
                .background(EasyPaisaDark)
                .border(1.dp, EasyPaisaGreen.copy(alpha = 0.4f), RoundedCornerShape(size * 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_easypaisa_badge),
                contentDescription = "EasyPaisa Official Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        if (showLabel) {
            Text(
                text = "easypaisa",
                color = EasyPaisaGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

/**
 * PayPal Brand Badge
 */
@Composable
fun PayPalRealLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showLabel: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.25f))
                .background(Color(0xFF001C38))
                .border(1.dp, PayPalBlue.copy(alpha = 0.4f), RoundedCornerShape(size * 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "P",
                color = Color(0xFF0079C1),
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.6f).sp
            )
        }
        if (showLabel) {
            Text(
                text = "PayPal",
                color = Color(0xFF0079C1),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

/**
 * Universal Gateway Badge selector with authentic logos
 */
@Composable
fun GatewayLogoBadge(
    gateway: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showLabel: Boolean = false
) {
    when (gateway.lowercase().trim()) {
        "jazzcash" -> JazzCashRealLogo(modifier = modifier, size = size, showLabel = showLabel)
        "easypaisa" -> EasyPaisaRealLogo(modifier = modifier, size = size, showLabel = showLabel)
        "paypal" -> PayPalRealLogo(modifier = modifier, size = size, showLabel = showLabel)
        else -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFF232742)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💳",
                    fontSize = (size.value * 0.5f).sp
                )
            }
        }
    }
}
