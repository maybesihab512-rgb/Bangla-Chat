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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserRepository
import com.example.model.User
import com.example.ui.components.AvatarWithStatus
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberTextField
import com.example.ui.theme.CyberBgCard
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

@Composable
fun ContactsScreen(
    userRepository: UserRepository,
    onNavigateBack: () -> Unit,
    onStartChat: (User) -> Unit,
    onStartCall: (User, isVideo: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val contacts by userRepository.contacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddContactDialog by remember { mutableStateOf(false) }

    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.handle.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBgDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddContactDialog = true },
                containerColor = CyberNeonCyan,
                contentColor = CyberBgDark,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Contact",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

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
                        text = "CONTACTS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = "${contacts.size} Contacts",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberElectricEmerald,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search contacts by name, username or phone...", color = CyberTextMuted, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(18.dp))
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CyberBgSurface,
                    unfocusedContainerColor = CyberBgSurface,
                    focusedBorderColor = CyberNeonCyan,
                    unfocusedBorderColor = CyberBorderSubtle,
                    focusedTextColor = CyberTextPrimary,
                    unfocusedTextColor = CyberTextPrimary,
                    cursorColor = CyberNeonCyan
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Contacts list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredContacts) { contact ->
                    ContactCard(
                        contact = contact,
                        onChatClick = { onStartChat(contact) },
                        onAudioCallClick = { onStartCall(contact, false) },
                        onVideoCallClick = { onStartCall(contact, true) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = {
                Text(
                    text = "ADD CONTACT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = CyberNeonCyan
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CyberTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = "Contact Name",
                        placeholder = "e.g., John Reese"
                    )
                    CyberTextField(
                        value = newContactPhone,
                        onValueChange = { newContactPhone = it },
                        label = "Phone or Username",
                        placeholder = "+1 555-019-9922"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newContactName.isNotBlank()) {
                            userRepository.addContact(newContactName.trim(), newContactPhone.trim())
                            newContactName = ""
                            newContactPhone = ""
                            showAddContactDialog = false
                        }
                    }
                ) {
                    Text("Add Contact", color = CyberElectricEmerald, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel", color = CyberTextMuted)
                }
            },
            containerColor = CyberBgSurface
        )
    }
}

@Composable
private fun ContactCard(
    contact: User,
    onChatClick: () -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarWithStatus(
                initials = contact.avatarInitials,
                colorHex = contact.avatarColorHex,
                size = 46.dp,
                isOnline = contact.isOnline,
                showShield = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${contact.handle} • ${contact.lastSeenText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (contact.isOnline) CyberElectricEmerald else CyberTextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contact.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp
                )
            }

            // Quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberBgSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat",
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onAudioCallClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberBgSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Audio Call",
                        tint = CyberElectricEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onVideoCallClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberBgSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
