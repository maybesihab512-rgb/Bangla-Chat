package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberButton
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberBgSurface
import com.example.ui.theme.CyberBgSurfaceElevated
import com.example.ui.theme.CyberBorderGlow
import com.example.ui.theme.CyberBorderSubtle
import com.example.ui.theme.CyberElectricEmerald
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateNext: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        delay(2200)
        onNavigateNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Glowing Cyber Emblem
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(CyberBgSurface)
                        .border(BorderStroke(2.dp, CyberBorderGlow), CircleShape)
                )

                // Inner core
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    CyberNeonCyan.copy(alpha = 0.35f),
                                    CyberBgSurfaceElevated
                                )
                            )
                        )
                        .border(BorderStroke(1.5.dp, CyberNeonCyan), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "CipherLink Logo",
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "CIPHERLINK",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                letterSpacing = 4.sp,
                color = CyberTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Secure Messaging & Calling",
                style = MaterialTheme.typography.bodySmall,
                color = CyberNeonCyan,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CyberBadge(text = "END-TO-END ENCRYPTED", color = CyberElectricEmerald)
                CyberBadge(text = "HD AUDIO & VIDEO", color = CyberNeonCyan)
            }

            Spacer(modifier = Modifier.height(48.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CyberNeonCyan,
                trackColor = CyberBgSurfaceElevated
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "CONNECTING...",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = CyberTextMuted,
                letterSpacing = 1.sp
            )
        }

        // Tap to skip
        Text(
            text = "TAP TO CONTINUE",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = CyberNeonCyan.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(BorderStroke(0.5.dp, CyberBorderSubtle), RoundedCornerShape(8.dp))
                .background(CyberBgSurface.copy(alpha = 0.8f))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
