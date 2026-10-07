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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.example.model.Conversation
import com.example.model.User
import com.example.ui.components.AvatarWithStatus
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
import com.example.ui.components.DeliveryTick
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
import com.example.ui.viewmodel.ChatFilter
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    viewModel: ChatViewModel,
    onOpenConversation: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNewChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.filteredConversations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val availablePeers by viewModel.availablePeers.collectAsState()
    val peerSearchResults by viewModel.peerSearchResults.collectAsState()
    val isSearchingPeers by viewModel.isSearchingPeers.collectAsState()

    var showNewChatSheet by remember { mutableStateOf(false) }
    var peerSearchQuery by remember { mutableStateOf("") }

    val displayPeers = if (peerSearchQuery.isNotBlank()) peerSearchResults else availablePeers

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBgDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatSheet = true },
                containerColor = CyberNeonCyan,
                contentColor = CyberBgDark,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
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

            // Screen Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBgSurface)
                        .border(BorderStroke(1.dp, CyberBorderSubtle), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyberNeonCyan
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = "End-to-End Encrypted",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberElectricEmerald,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { showNewChatSheet = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBgSurface)
                        .border(BorderStroke(1.dp, CyberBorderSubtle), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "New Chat",
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberBgSurfaceElevated)
                    .border(BorderStroke(1.dp, CyberBorderSubtle), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CyberTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                "Search conversations...",
                                color = CyberTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary,
                            cursorColor = CyberNeonCyan,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = CyberTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ChatFilter.values()) { filter ->
                    val isSelected = selectedFilter == filter
                    val filterLabel = when (filter) {
                        ChatFilter.ALL -> "All"
                        ChatFilter.DIRECT -> "Direct"
                        ChatFilter.GROUPS -> "Groups"
                        ChatFilter.UNREAD -> "Unread"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isSelected) CyberNeonCyan else CyberBgSurfaceElevated)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isSelected) CyberNeonCyan else CyberBorderSubtle
                                ),
                                RoundedCornerShape(100.dp)
                            )
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filterLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) CyberBgDark else CyberTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Conversations List / Empty State
            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CyberCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(CyberBgSurfaceElevated)
                                    .border(BorderStroke(1.5.dp, CyberNeonCyan), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = CyberNeonCyan,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "NO CONVERSATIONS YET",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CyberTextPrimary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Connect with your contacts to start chatting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            CyberButton(
                                text = "Start a Chat",
                                onClick = { showNewChatSheet = true },
                                icon = Icons.Default.Add,
                                accentColor = CyberNeonCyan
                            )
                        }
                    }
                }
            } else {
                val context = LocalContext.current
                val activity = context as? FragmentActivity

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(conversations) { conv ->
                        ConversationCard(
                            conversation = conv,
                            onClick = {
                                if (conv.isLocked && !viewModel.isChatUnlockedInSession(conv.id) && activity != null) {
                                    viewModel.authenticateToOpenChat(
                                        activity = activity,
                                        conversationTitle = conv.title,
                                        onSuccess = {
                                            viewModel.markUnlockedForSession(conv.id)
                                            onOpenConversation(conv.id)
                                        },
                                        onError = { err ->
                                            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    onOpenConversation(conv.id)
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: New Chat / User Search
    if (showNewChatSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showNewChatSheet = false
                peerSearchQuery = ""
            },
            containerColor = CyberBgSurface,
            contentColor = CyberTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEW CHAT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = CyberNeonCyan,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = { showNewChatSheet = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CyberTextMuted
                        )
                    }
                }

                Text(
                    text = "Select a contact or search for a user to start chatting",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Search Peer Input
                OutlinedTextField(
                    value = peerSearchQuery,
                    onValueChange = {
                        peerSearchQuery = it
                        viewModel.searchPeers(it)
                    },
                    placeholder = {
                        Text("Search by name, username or phone...", color = CyberTextMuted, fontSize = 13.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(18.dp))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CyberBgCard,
                        unfocusedContainerColor = CyberBgCard,
                        focusedBorderColor = CyberNeonCyan,
                        unfocusedBorderColor = CyberBorderSubtle,
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary,
                        cursorColor = CyberNeonCyan
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (peerSearchQuery.isNotBlank()) "SEARCH RESULTS (${displayPeers.size})" else "CONTACTS (${displayPeers.size})",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = CyberTextMuted,
                        letterSpacing = 1.sp
                    )
                    if (isSearchingPeers) {
                        Text(
                            text = "Searching...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberNeonCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    if (displayPeers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isSearchingPeers) "Searching users..." else "No contacts found",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = CyberTextMuted
                                )
                            }
                        }
                    } else {
                        items(displayPeers) { peer ->
                            PeerSelectionRow(
                                peer = peer,
                                onSelect = {
                                    val convId = viewModel.startChatWithContact(peer)
                                    showNewChatSheet = false
                                    peerSearchQuery = ""
                                    onOpenConversation(convId)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun PeerSelectionRow(
    peer: User,
    onSelect: () -> Unit
) {
    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarWithStatus(
                initials = peer.avatarInitials,
                colorHex = peer.avatarColorHex,
                size = 40.dp,
                isOnline = peer.isOnline,
                showShield = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary,
                    fontSize = 14.sp
                )
                Text(
                    text = "@${peer.handle} • ${peer.statusMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberNeonCyan.copy(alpha = 0.15f))
                    .border(BorderStroke(1.dp, CyberNeonCyan), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "CHAT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = CyberNeonCyan
                )
            }
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    onClick: () -> Unit
) {
    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarWithStatus(
                initials = conversation.avatarInitials,
                colorHex = conversation.avatarColorHex,
                size = 48.dp,
                isOnline = conversation.isOnline,
                showShield = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = conversation.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (conversation.isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (conversation.isFavorite) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = CyberAmber,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (conversation.isLocked) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = CyberElectricEmerald,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (conversation.isGroup) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Group",
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = conversation.lastMessage?.formattedTime ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (conversation.isLocked) {
                            Text(
                                text = "🔒 Locked conversation",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = CyberTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (conversation.draft.isNotBlank()) {
                            Text(
                                text = "Draft: ${conversation.draft}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberCrimson,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            if (conversation.lastMessage?.isOutgoing == true) {
                                DeliveryTick(status = conversation.lastMessage.deliveryStatus)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = conversation.lastMessage?.content ?: "No messages yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (conversation.unreadCount > 0) CyberTextPrimary else CyberTextSecondary,
                                fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(CyberNeonCyan)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberBgDark
                            )
                        }
                    }
                }
            }
        }
    }
}
