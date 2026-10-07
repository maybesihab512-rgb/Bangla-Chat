package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberBgSurface
import com.example.ui.theme.CyberBgSurfaceElevated
import com.example.ui.theme.CyberBorderGlow
import com.example.ui.theme.CyberBorderSubtle
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberElectricEmerald
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import kotlinx.coroutines.delay

@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    onVerifySuccess: () -> Unit,
    onBack: () -> Unit,
    onVerifyCode: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var otpCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableIntStateOf(45) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Nav
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberBgSurface)
                        .border(BorderStroke(1.dp, CyberBorderSubtle.copy(alpha = 0.8f)), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyberNeonCyan
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Verify Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(CyberBgSurfaceElevated)
                    .border(BorderStroke(1.dp, CyberBorderSubtle.copy(alpha = 0.8f)), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = CyberNeonCyan,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Enter Verification Code",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CyberTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "A 6-digit code was sent to:",
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextSecondary
            )

            Text(
                text = phoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = CyberNeonCyan,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 6-digit OTP Box
            BasicTextField(
                value = otpCode,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        otpCode = it
                        errorMessage = null
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                decorationBox = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 0 until 6) {
                            val char = otpCode.getOrNull(i)?.toString() ?: ""
                            val isFocused = otpCode.length == i || (otpCode.length == 6 && i == 5)
                            val borderColor = when {
                                errorMessage != null -> CyberCrimson
                                isFocused -> CyberNeonCyan
                                char.isNotEmpty() -> CyberElectricEmerald
                                else -> CyberBorderSubtle.copy(alpha = 0.8f)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CyberBgSurfaceElevated)
                                    .border(BorderStroke(1.2.dp, borderColor), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (char.isNotEmpty()) CyberTextPrimary else CyberTextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = CyberCrimson,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))



            Spacer(modifier = Modifier.height(24.dp))

            CyberButton(
                text = "Verify and Continue",
                onClick = {
                    if (otpCode.length == 6) {
                        val success = onVerifyCode(otpCode)
                        if (success) {
                            onVerifySuccess()
                        } else {
                            errorMessage = "Invalid verification code"
                        }
                    } else {
                        errorMessage = "Please enter the complete 6-digit code"
                    }
                },
                icon = Icons.Default.CheckCircle,
                enabled = otpCode.length == 6
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Resend
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (countdown > 0) "Resend code in " else "Didn't receive code? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextSecondary
                )
                if (countdown > 0) {
                    Text(
                        text = "${countdown}s",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberNeonCyan
                    )
                } else {
                    Text(
                        text = "Resend Code",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberNeonCyan,
                        modifier = Modifier
                            .clickable { countdown = 45 }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
