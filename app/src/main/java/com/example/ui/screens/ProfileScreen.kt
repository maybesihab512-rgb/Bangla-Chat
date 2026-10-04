package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.components.CyberTextField
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isUpdatingProfile by authViewModel.isUpdatingProfile.collectAsState()
    val profileUpdateError by authViewModel.profileUpdateError.collectAsState()
    val context = LocalContext.current

    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    var editDisplayName by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var editPhoneNumber by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }
    var editStatusMessage by remember(currentUser) { mutableStateOf(currentUser?.statusMessage ?: "Available") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val current = currentUser
            if (current != null) {
                authViewModel.updateProfile(
                    displayName = current.name,
                    phoneNumber = current.phone,
                    photoUri = uri,
                    statusMessage = current.statusMessage,
                    onSuccess = {
                        Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    val user = currentUser ?: User(
        id = "",
        name = "User",
        handle = "user",
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
                    onClick = {
                        editDisplayName = user.name
                        editPhoneNumber = user.phone
                        editStatusMessage = user.statusMessage
                        showEditProfileSheet = true
                    },
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
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AvatarWithStatus(
                            initials = user.avatarInitials.ifEmpty { "U" },
                            photoUrl = user.photoUrl,
                            colorHex = user.avatarColorHex,
                            size = 88.dp,
                            isOnline = true
                        )

                        // Camera change photo badge button
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
                            if (isUpdatingProfile) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberBgDark
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change profile photo",
                                    tint = CyberBgDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = user.name.ifEmpty { "User" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )

                    Text(
                        text = if (user.handle.isNotBlank()) "@${user.handle}" else "",
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
                        label = "Display Name",
                        value = user.name.ifEmpty { "Not set" },
                        icon = Icons.Default.Person
                    )
                    DetailRow(
                        label = "Phone Number",
                        value = user.phone.ifEmpty { "Not set (Tap edit to add)" },
                        icon = Icons.Default.Phone
                    )
                    DetailRow(
                        label = "Call Quality",
                        value = "HD Audio / Adaptive Data Saver",
                        icon = Icons.Default.Shield
                    )
                    DetailRow(
                        label = "Chat Storage",
                        value = "Encrypted Cloud & Local Storage",
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
                        text = "Your account is connected to Firebase. Real profile data and encrypted messages sync across devices in real time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Disconnect / Logout Button
            val activity = context as? android.app.Activity
            CyberOutlinedButton(
                text = "Disconnect & Sign Out",
                onClick = { authViewModel.logOut(activity) },
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                borderColor = CyberCrimson,
                textColor = CyberCrimson
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Edit Profile Modal Bottom Sheet
    if (showEditProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditProfileSheet = false },
            containerColor = CyberBgSurface,
            contentColor = CyberTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "EDIT PROFILE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = CyberNeonCyan,
                    letterSpacing = 1.sp
                )

                // Photo preview and change action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AvatarWithStatus(
                        initials = editDisplayName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "U" },
                        photoUrl = user.photoUrl,
                        size = 60.dp,
                        isOnline = true
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    CyberOutlinedButton(
                        text = "Change Photo",
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        icon = Icons.Default.CameraAlt,
                        modifier = Modifier.weight(1f)
                    )
                }

                CyberTextField(
                    value = editDisplayName,
                    onValueChange = { editDisplayName = it },
                    label = "Display Name",
                    placeholder = "Your full name",
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(18.dp))
                    }
                )

                CyberTextField(
                    value = editPhoneNumber,
                    onValueChange = { editPhoneNumber = it },
                    label = "Phone Number",
                    placeholder = "+1 (555) 123-4567",
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(18.dp))
                    }
                )

                CyberTextField(
                    value = editStatusMessage,
                    onValueChange = { editStatusMessage = it },
                    label = "Status Message",
                    placeholder = "Available",
                    leadingIcon = {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(18.dp))
                    }
                )

                if (profileUpdateError != null) {
                    Text(
                        text = profileUpdateError ?: "",
                        color = CyberCrimson,
                        fontSize = 12.sp
                    )
                }

                CyberButton(
                    text = if (isUpdatingProfile) "Saving..." else "Save Changes",
                    onClick = {
                        authViewModel.updateProfile(
                            displayName = editDisplayName.trim(),
                            phoneNumber = editPhoneNumber.trim(),
                            photoUri = null,
                            statusMessage = editStatusMessage.trim(),
                            onSuccess = {
                                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                showEditProfileSheet = false
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isUpdatingProfile && editDisplayName.isNotBlank(),
                    icon = Icons.Default.Check
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // QR Verification Dialog
    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Text(
                    text = "SAFETY KEY VERIFICATION",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
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
                            .background(Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Code",
                            tint = Color.Black,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = user.e2eeFingerprint,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberNeonCyan
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Scan this code with a peer's device to verify E2EE authenticity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("Close", color = CyberNeonCyan)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                fontSize = 10.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = CyberTextPrimary,
                fontSize = 13.sp
            )
        }
    }
}
