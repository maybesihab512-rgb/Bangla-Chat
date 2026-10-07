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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberBgSurface
import com.example.ui.theme.CyberBgSurfaceElevated
import com.example.ui.theme.CyberBorderSubtle
import com.example.ui.theme.CyberElectricEmerald
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val callingConfig by viewModel.callingConfig.collectAsState()
    val securitySettings by viewModel.securitySettings.collectAsState()

    var messageNotifications by remember { mutableStateOf(true) }
    var callRingtone by remember { mutableStateOf(true) }
    var useLessMobileData by remember { mutableStateOf(callingConfig.dataSavingModeEnabled) }

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

                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = "Preferences & Privacy",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 1. My Account
            SectionHeader(title = "MY ACCOUNT")
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingInfoRow(
                        title = "Account Status",
                        subtitle = "Active & Verified",
                        icon = Icons.Default.Person
                    )
                    SettingInfoRow(
                        title = "Online Status",
                        subtitle = "Visible to contacts",
                        icon = Icons.Default.Security
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Notifications
            SectionHeader(title = "NOTIFICATIONS")
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingToggleRow(
                        title = "Message Notifications",
                        description = "Show alerts and previews for new incoming messages.",
                        icon = Icons.Default.Notifications,
                        checked = messageNotifications,
                        onCheckedChange = { messageNotifications = it }
                    )
                    SettingToggleRow(
                        title = "Call Ringtone & Sounds",
                        description = "Play ringtone and vibrate for incoming voice & video calls.",
                        icon = Icons.Default.VolumeUp,
                        checked = callRingtone,
                        onCheckedChange = { callRingtone = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Privacy & Security
            SectionHeader(title = "PRIVACY & SECURITY")
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingToggleRow(
                        title = "Screen Lock",
                        description = "Require fingerprint or face unlock to open the app.",
                        icon = Icons.Default.Fingerprint,
                        checked = securitySettings.biometricLockEnabled,
                        onCheckedChange = { viewModel.toggleBiometricLock(it) }
                    )
                    SettingToggleRow(
                        title = "Screen Security",
                        description = "Blocks screenshots and hides chat previews in recent apps.",
                        icon = Icons.Default.Lock,
                        checked = securitySettings.screenSecurityBlockScreenshots,
                        onCheckedChange = { /* fixed active for security */ }
                    )
                    SettingClickableRow(
                        title = "Blocked Users",
                        subtitle = "None",
                        icon = Icons.Default.Block,
                        onClick = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Appearance
            SectionHeader(title = "APPEARANCE")
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingInfoRow(
                        title = "Theme",
                        subtitle = "Dark (Default)",
                        icon = Icons.Default.Palette
                    )
                    SettingInfoRow(
                        title = "Font Size",
                        subtitle = "Standard",
                        icon = Icons.Default.Info
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Language
            SectionHeader(title = "LANGUAGE")
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingInfoRow(
                        title = "App Language",
                        subtitle = "English (Default)",
                        icon = Icons.Default.Language
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Calls & Data
            SectionHeader(title = "CALLS & DATA")
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingInfoRow(
                        title = "Call Quality",
                        subtitle = "High Quality (Adaptive)",
                        icon = Icons.Default.Speed
                    )
                    SettingToggleRow(
                        title = "Data Saver",
                        description = "Reduces data usage during voice and video calls.",
                        icon = Icons.Default.DataSaverOn,
                        checked = callingConfig.dataSavingModeEnabled,
                        onCheckedChange = { viewModel.toggleDataSavingMode(it) }
                    )
                    SettingToggleRow(
                        title = "Use Less Mobile Data",
                        description = "Optimizes call quality and limits bandwidth on mobile networks.",
                        icon = Icons.Default.Phone,
                        checked = useLessMobileData,
                        onCheckedChange = {
                            useLessMobileData = it
                            viewModel.toggleDataSavingMode(it)
                        }
                    )
                    SettingInfoRow(
                        title = "Wi-Fi Preference",
                        subtitle = "Automatically switch for best connection",
                        icon = Icons.Default.Wifi
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 7. About
            SectionHeader(title = "ABOUT")
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                backgroundColor = CyberBgSurfaceElevated
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CipherLink for Android v1.0.0",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = "Private End-to-End Encrypted Messaging & HD Calling.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Developer: Maybe Sihab  •  +8801646864645",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = CyberNeonCyan,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) CyberNeonCyan else CyberTextMuted,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CyberTextPrimary,
                fontSize = 14.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberBgDark,
                checkedTrackColor = CyberNeonCyan,
                uncheckedThumbColor = CyberTextSecondary,
                uncheckedTrackColor = CyberBgSurfaceElevated,
                uncheckedBorderColor = CyberBorderSubtle
            )
        )
    }
}

@Composable
private fun SettingInfoRow(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyberNeonCyan,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CyberTextPrimary,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SettingClickableRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyberNeonCyan,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CyberTextPrimary,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = CyberTextSecondary,
                fontSize = 11.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = CyberTextMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}
