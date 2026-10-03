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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthStatus
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberOutlinedButton
import com.example.ui.components.CyberTextField
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

@Composable
fun LoginScreen(
    onContinueWithGoogle: () -> Unit,
    onContinueWithPhone: (phoneNumber: String) -> Unit,
    authStatus: AuthStatus = AuthStatus.Idle,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf("+1 555-019-2834") }
    var selectedCountryCode by remember { mutableStateOf("+1") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val statusError = (authStatus as? AuthStatus.Error)?.message
    val activeError = errorMessage ?: statusError
    val isLoading = authStatus is AuthStatus.Loading

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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Tech Header
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberBgSurfaceElevated)
                    .border(BorderStroke(1.dp, CyberNeonCyan), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = CyberNeonCyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "SIGN IN",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 2.sp,
                color = CyberTextPrimary
            )

            Text(
                text = "Choose how you want to sign in",
                style = MaterialTheme.typography.bodyMedium,
                color = CyberTextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (activeError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberCrimson.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, CyberCrimson), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = activeError,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CyberCrimson,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Method: Continue with Google (Zero-friction 1-Tap)
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                borderGlow = true,
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "GOOGLE SIGN-IN",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberElectricEmerald,
                            letterSpacing = 1.sp
                        )
                        CyberBadge(text = "RECOMMENDED", color = CyberElectricEmerald)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Quick and secure sign-in with your Google account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CyberButton(
                        text = if (isLoading) "Signing in..." else "Continue with Google",
                        onClick = onContinueWithGoogle,
                        icon = Icons.Default.Public,
                        accentColor = CyberElectricEmerald,
                        textColor = CyberBgDark,
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = CyberBorderSubtle
                )
                Text(
                    text = "  OR  ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = CyberTextMuted,
                    letterSpacing = 1.5.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = CyberBorderSubtle
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Secondary Method: Continue with Phone Number + OTP
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                borderGlow = false,
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PHONE NUMBER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberNeonCyan,
                            letterSpacing = 1.sp
                        )
                        CyberBadge(text = "SMS CODE", color = CyberNeonCyan)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Phone Number (with country code)",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberTextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    CyberTextField(
                        value = phoneNumber,
                        onValueChange = {
                            phoneNumber = it
                            errorMessage = null
                        },
                        label = "Phone Number",
                        placeholder = "+1 555-010-0000",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    CyberButton(
                        text = if (isLoading) "Sending code..." else "Continue with Phone Number",
                        onClick = {
                            if (phoneNumber.trim().length >= 6) {
                                errorMessage = null
                                onContinueWithPhone(phoneNumber.trim())
                            } else {
                                errorMessage = "Please enter a valid phone number with country code"
                            }
                        },
                        icon = Icons.Default.Key,
                        accentColor = CyberNeonCyan,
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "We will send an SMS verification code to your phone.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = CyberTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Trust badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = CyberTextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "End-to-End Encrypted • Private & Secure",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = CyberTextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
