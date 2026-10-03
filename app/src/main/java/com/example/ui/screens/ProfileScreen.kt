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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.ui.components.AvatarWithStatus
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberOutlinedButton
import com.example.ui.theme.CyberAmber
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
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var showQrDialog by remember { mutableStateOf(false) }
    var copiedToast by remember { mutableStateOf(false) }

    val user = currentUser ?: User(
        id = "usr_local",
        name = "Alex Vance",
        handle = "alex_vance",
        statusMessage = "Available",
        e2eeFingerprint = "7F4A:9C2E:11D8:6E90:4B21:AA33"
    )

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
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberBgSurface)
                        .border(BorderStroke(1.dp, CyberBorderSubtle), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyberNeonCyan
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "PROFILE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp,
                    color = CyberTextPrimary
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onEditProfileClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberBgSurface)
                        .border(BorderStroke(1.dp, CyberBorderSubtle), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Card
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                borderGlow = true,
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AvatarWithStatus(
                        initials = user.avatarInitials.ifEmpty { "OP" },
                        colorHex = user.avatarColorHex,
                        size = 80.dp,
                        isOnline = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )

                    Text(
                        text = "@${user.handle}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CyberNeonCyan,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "\"${user.statusMessage}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CyberBadge(text = "VERIFIED ACCOUNT", color = CyberElectricEmerald)
                        CyberBadge(text = "END-TO-END ENCRYPTED", color = CyberNeonCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // E2EE Public Safety Number Card
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = CyberElectricEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SAFETY NUMBER",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = CyberElectricEmerald
                            )
                        }

                        IconButton(
                            onClick = { showQrDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "View QR",
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberBgDark)
                            .border(BorderStroke(0.5.dp, CyberBorderSubtle), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = user.e2eeFingerprint,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyberNeonCyan,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Safety number used to verify end-to-end encryption for your messages and calls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account & Connection details
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DetailRow(
                        label = "Phone Number",
                        value = user.phone.ifEmpty { "+1 (555) 019-2834" },
                        icon = Icons.Default.Phone
                    )
                    DetailRow(
                        label = "Call Quality",
                        value = "HD Audio / Adaptive Data Saver",
                        icon = Icons.Default.Shield
                    )
                    DetailRow(
                        label = "Chat Storage",
                        value = "Encrypted Local Storage",
                        icon = Icons.Default.Lock
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cloud Status
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                backgroundColor = CyberBgSurfaceElevated,
                borderColor = CyberNeonCyan
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CLOUD SYNC STATUS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberNeonCyan
                        )
                        CyberBadge(text = "CONNECTED", color = CyberElectricEmerald)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your account is connected. Incoming and outgoing messages sync across devices in real time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Disconnect / Logout Button
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? android.app.Activity
            CyberOutlinedButton(
                text = "Log Out",
                onClick = {
                    authViewModel.logOut(activity)
                    onNavigateBack()
                },
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                borderColor = CyberCrimson,
                textColor = CyberCrimson
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // QR Code Modal Simulation
    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Text(
                    text = "PROFILE QR CODE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberNeonCyan
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberBgDark)
                            .border(BorderStroke(1.5.dp, CyberNeonCyan), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = CyberNeonCyan,
                            modifier = Modifier.size(120.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scan to verify security with another user",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("Done", color = CyberNeonCyan)
                }
            },
            containerColor = CyberBgSurface
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyberNeonCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextMuted,
                fontSize = 11.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = CyberTextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
