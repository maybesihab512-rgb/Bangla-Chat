package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.ui.components.CyberTextField
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberBgSurface
import com.example.ui.theme.CyberBgSurfaceElevated
import com.example.ui.theme.CyberBorderGlow
import com.example.ui.theme.CyberBorderSubtle
import com.example.ui.theme.CyberElectricEmerald
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
fun CreateProfileScreen(
    currentUser: User?,
    onSaveProfile: (name: String, handle: String, status: String, photoUri: Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayName by remember { mutableStateOf(currentUser?.name ?: "") }
    var handle by remember { mutableStateOf(currentUser?.handle ?: "") }
    var statusMessage by remember { mutableStateOf(currentUser?.statusMessage ?: "Available") }
    var selectedColorHex by remember { mutableLongStateOf(currentUser?.avatarColorHex ?: 0xFF00F0FF) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isError by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val colorOptions = listOf(0xFF00F0FF, 0xFF00E699, 0xFFFFB020, 0xFFA855F7, 0xFFFF3366)

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

            Text(
                text = "Set up your profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CyberTextPrimary
            )

            Text(
                text = "Enter your display name and public handle",
                style = MaterialTheme.typography.bodyMedium,
                color = CyberTextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Avatar Preview with color picker & photo change badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AvatarWithStatus(
                        initials = displayName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "OP" },
                        photoUrl = selectedPhotoUri?.toString() ?: (currentUser?.photoUrl ?: ""),
                        colorHex = selectedColorHex,
                        size = 88.dp,
                        isOnline = true
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(CyberNeonCyan)
                            .border(BorderStroke(2.dp, CyberBgDark), CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Choose photo",
                            tint = CyberBgDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colorOptions.forEach { hex ->
                        val isSelected = selectedColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    BorderStroke(
                                        if (isSelected) 2.5.dp else 1.dp,
                                        if (isSelected) CyberTextPrimary else Color.Transparent
                                    ),
                                    CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Text fields
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CyberTextField(
                        value = displayName,
                        onValueChange = {
                            displayName = it
                            isError = false
                        },
                        label = "Display Name",
                        placeholder = "Your full name",
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(18.dp))
                        },
                        isError = isError && displayName.isBlank(),
                        errorMessage = if (isError && displayName.isBlank()) "Name is required" else null
                    )

                    CyberTextField(
                        value = handle,
                        onValueChange = {
                            handle = it.filter { char -> char.isLetterOrDigit() || char == '_' }
                        },
                        label = "Username",
                        placeholder = "username",
                        leadingIcon = {
                            Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(18.dp))
                        }
                    )

                    CyberTextField(
                        value = statusMessage,
                        onValueChange = { statusMessage = it },
                        label = "Status Message",
                        placeholder = "e.g., Available",
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Preview Card
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                backgroundColor = CyberBgSurfaceElevated
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyberElectricEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Privacy & Security",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberElectricEmerald
                        )
                        Text(
                            text = "End-to-end encrypted messaging",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            CyberButton(
                text = "Save Profile & Continue",
                onClick = {
                    if (displayName.isNotBlank()) {
                        onSaveProfile(displayName.trim(), handle.trim(), statusMessage.trim(), selectedPhotoUri)
                    } else {
                        isError = true
                    }
                },
                icon = Icons.Default.CheckCircle,
                accentColor = CyberNeonCyan
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
