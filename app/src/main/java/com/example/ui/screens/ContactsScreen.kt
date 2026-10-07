package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserRepository
import com.example.model.User
import com.example.ui.components.AvatarWithStatus
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberTextField
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberBgSurface
import com.example.ui.theme.CyberBgSurfaceElevated
import com.example.ui.theme.CyberBorderSubtle
import com.example.ui.theme.CyberElectricEmerald
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var remoteSearchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var isSearchingRemote by remember { mutableStateOf(false) }

    // Real search across Firebase registered users when query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearchingRemote = true
            delay(300) // Debounce
            remoteSearchResults = userRepository.searchUsers(searchQuery)
            isSearchingRemote = false
        } else {
            remoteSearchResults = emptyList()
            isSearchingRemote = false
        }
    }

    val filteredLocalContacts = remember(contacts, searchQuery) {
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
                    contentDescription = "Find & Add User",
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
                        text = "Contacts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = "${contacts.size} contacts",
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
                    Text("Search users by name or phone...", color = CyberTextMuted, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (isSearchingRemote) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = CyberNeonCyan
                        )
                    }
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

            // Contacts / Search Results list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // If searching, show Firebase registered user search results
                if (searchQuery.isNotBlank()) {
                    val combinedList = (filteredLocalContacts + remoteSearchResults).distinctBy { it.id }

                    if (combinedList.isEmpty() && !isSearchingRemote) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No registered users found matching \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyberTextMuted
                                )
                            }
                        }
                    } else {
                        items(combinedList) { contact ->
                            ContactCard(
                                contact = contact,
                                onChatClick = { onStartChat(contact) },
                                onAudioCallClick = { onStartCall(contact, false) },
                                onVideoCallClick = { onStartCall(contact, true) }
                            )
                        }
                    }
                } else {
                    if (contacts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No contacts yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CyberTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Search users above or tap '+' to find registered users",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CyberTextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredLocalContacts) { contact ->
                            ContactCard(
                                contact = contact,
                                onChatClick = { onStartChat(contact) },
                                onAudioCallClick = { onStartCall(contact, false) },
                                onVideoCallClick = { onStartCall(contact, true) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Add Contact Dialog: Search real Firebase users by name or phone
    if (showAddContactDialog) {
        var addSearchQuery by remember { mutableStateOf("") }
        var addSearchResults by remember { mutableStateOf<List<User>>(emptyList()) }
        var isSearchingAdd by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = {
                Text(
                    text = "FIND & ADD USER",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = CyberNeonCyan
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Search registered users by display name or phone number",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary,
                        fontSize = 12.sp
                    )

                    CyberTextField(
                        value = addSearchQuery,
                        onValueChange = {
                            addSearchQuery = it
                            if (it.isNotBlank()) {
                                isSearchingAdd = true
                                scope.launch {
                                    delay(250)
                                    addSearchResults = userRepository.searchUsers(it)
                                    isSearchingAdd = false
                                }
                            } else {
                                addSearchResults = emptyList()
                            }
                        },
                        label = "Search User",
                        placeholder = "Name or phone number"
                    )

                    if (isSearchingAdd) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberNeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Searching Firebase...", fontSize = 11.sp, color = CyberTextMuted)
                        }
                    }

                    if (addSearchResults.isNotEmpty()) {
                        Text(
                            text = "REGISTERED USERS (${addSearchResults.size})",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberNeonCyan
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        ) {
                            items(addSearchResults) { foundUser ->
                                CyberCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        userRepository.addContact(foundUser)
                                        Toast.makeText(context, "${foundUser.name} added to contacts", Toast.LENGTH_SHORT).show()
                                        showAddContactDialog = false
                                    },
                                    cornerRadius = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarWithStatus(
                                            initials = foundUser.avatarInitials,
                                            photoUrl = foundUser.photoUrl,
                                            size = 36.dp,
                                            isOnline = foundUser.isOnline
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = foundUser.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = CyberTextPrimary
                                            )
                                            Text(
                                                text = if (foundUser.phone.isNotBlank()) foundUser.phone else "@${foundUser.handle}",
                                                fontSize = 11.sp,
                                                color = CyberTextSecondary
                                            )
                                        }
                                        Text(
                                            text = "+ Add",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = CyberElectricEmerald
                                        )
                                    }
                                }
                            }
                        }
                    } else if (addSearchQuery.isNotBlank() && !isSearchingAdd) {
                        Text(
                            text = "No registered user found with that name or number",
                            fontSize = 12.sp,
                            color = CyberTextMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Close", color = CyberTextMuted)
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
                photoUrl = contact.photoUrl,
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
                    text = if (contact.phone.isNotBlank()) "${contact.phone} • ${contact.lastSeenText}" else "@${contact.handle} • ${contact.lastSeenText}",
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
