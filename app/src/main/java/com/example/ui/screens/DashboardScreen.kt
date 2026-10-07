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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallDirection
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Conversation
import com.example.ui.components.AvatarWithStatus
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
import com.example.ui.components.DeliveryTick
import com.example.ui.components.NetworkQualityBadge
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
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToChats: () -> Unit,
    onNavigateToCalls: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onStartCall: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            DashboardHeader(
                userName = state.user?.name ?: "User",
                statusMessage = state.user?.statusMessage ?: "Online",
                avatarInitials = state.user?.avatarInitials ?: "U",
                photoUrl = state.user?.photoUrl,
                avatarColorHex = state.user?.avatarColorHex ?: 0xFF00F0FF,
                onProfileClick = onNavigateToProfile,
                onSettingsClick = onNavigateToSettings
            )
        }

        // Live Network Quality & Call Info Card
        item {
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
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = CyberElectricEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Connection & Call Quality",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberElectricEmerald
                            )
                        }

                        CyberBadge(text = "Encrypted", color = CyberNeonCyan)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Network Latency",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberTextSecondary
                            )
                            Text(
                                text = "${state.networkMetrics.rttMs}ms • ${state.networkMetrics.currentAudioBitrateKbps}kbps Opus",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = CyberTextPrimary
                            )
                        }

                        NetworkQualityBadge(metrics = state.networkMetrics)
                    }
                }
            }
        }

        // Quick Navigation Tiles (Chats, Calls, Contacts)
        item {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = CyberTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickNavCard(
                    title = "Chats",
                    subtitle = if (state.totalUnreadMessages > 0) "${state.totalUnreadMessages} unread" else "All caught up",
                    icon = Icons.Default.Chat,
                    accentColor = CyberNeonCyan,
                    badgeCount = state.totalUnreadMessages,
                    onClick = onNavigateToChats,
                    modifier = Modifier.weight(1f)
                )

                QuickNavCard(
                    title = "Calls",
                    subtitle = if (state.missedCallsCount > 0) "${state.missedCallsCount} missed" else "Ready",
                    icon = Icons.Default.Phone,
                    accentColor = if (state.missedCallsCount > 0) CyberAmber else CyberElectricEmerald,
                    badgeCount = state.missedCallsCount,
                    onClick = onNavigateToCalls,
                    modifier = Modifier.weight(1f)
                )

                QuickNavCard(
                    title = "Contacts",
                    subtitle = "${state.onlineContactsCount} online",
                    icon = Icons.Default.Contacts,
                    accentColor = CyberPurple,
                    badgeCount = 0,
                    onClick = onNavigateToContacts,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Conversations Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToChats)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Chats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberTextPrimary
                )
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = CyberNeonCyan
                )
            }
        }

        // Recent Conversations List
        if (state.recentConversations.isEmpty()) {
            item {
                CyberCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToChats),
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No chats yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tap to search contacts & start a conversation",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberNeonCyan.copy(alpha = 0.15f))
                                .border(BorderStroke(1.dp, CyberNeonCyan), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "NEW CHAT",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = CyberNeonCyan
                            )
                        }
                    }
                }
            }
        } else {
            items(state.recentConversations) { conv ->
                ConversationItemRow(
                    conversation = conv,
                    onClick = { onOpenConversation(conv.id) }
                )
            }
        }

        // Recent Calls Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Calls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberTextPrimary
                )
                Text(
                    text = "Call history",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = CyberNeonCyan,
                    modifier = Modifier
                        .clickable(onClick = onNavigateToCalls)
                        .padding(4.dp)
                )
            }
        }

        // Recent Calls List
        if (state.recentCalls.isEmpty()) {
            item {
                EmptyStateCard(text = "No recent calls")
            }
        } else {
            items(state.recentCalls) { call ->
                CallItemRow(
                    call = call,
                    onCallBack = { onStartCall(call.contactId, call.callType == CallType.VIDEO) }
                )
            }
        }

        // Developer Credit (Strictly adhering to user requirements):
        // Developer: Maybe Sihab
        // +8801646864645
        // Small, clean, readable, visually integrated.
        // No WhatsApp buttons, developer profile buttons, or contact buttons.
        item {
            Spacer(modifier = Modifier.height(12.dp))
            DeveloperCreditFooter()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DashboardHeader(
    userName: String,
    statusMessage: String,
    avatarInitials: String,
    avatarColorHex: Long,
    photoUrl: String? = null,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onProfileClick)
                .padding(vertical = 4.dp)
        ) {
            AvatarWithStatus(
                initials = avatarInitials,
                photoUrl = photoUrl,
                colorHex = avatarColorHex,
                size = 46.dp,
                isOnline = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary
                )
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberNeonCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberBgSurface)
                    .border(BorderStroke(1.dp, CyberBorderSubtle), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = CyberNeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CyberCard(
        modifier = modifier,
        onClick = onClick,
        cornerRadius = 14.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = CyberBgDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CyberTextPrimary
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
private fun ConversationItemRow(
    conversation: Conversation,
    onClick: () -> Unit
) {
    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarWithStatus(
                initials = conversation.avatarInitials,
                colorHex = conversation.avatarColorHex,
                size = 46.dp,
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
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = conversation.lastMessage?.formattedTime ?: "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
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
                        if (conversation.lastMessage?.isOutgoing == true) {
                            DeliveryTick(status = conversation.lastMessage.deliveryStatus)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = conversation.lastMessage?.content ?: "No messages yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (conversation.unreadCount > 0) CyberTextPrimary else CyberTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CyberNeonCyan)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = CyberBgDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallItemRow(
    call: CallRecord,
    onCallBack: () -> Unit
) {
    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val directionIcon = when (call.direction) {
                CallDirection.INCOMING -> Icons.Default.CallReceived
                CallDirection.OUTGOING -> Icons.Default.CallMade
                CallDirection.MISSED -> Icons.Default.CallMissed
            }
            val directionColor = when (call.direction) {
                CallDirection.MISSED -> CyberCrimson
                CallDirection.INCOMING -> CyberElectricEmerald
                CallDirection.OUTGOING -> CyberNeonCyan
            }

            AvatarWithStatus(
                initials = call.contactAvatarInitials,
                colorHex = call.avatarColorHex,
                size = 42.dp,
                isOnline = false
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.contactName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = directionIcon,
                        contentDescription = null,
                        tint = directionColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${call.formattedDate} • ${if (call.durationSeconds > 0) call.formattedDuration else "Missed"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onCallBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CyberBgSurfaceElevated)
            ) {
                Icon(
                    imageVector = if (call.callType == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = "Call back",
                    tint = CyberNeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberBgCard)
            .border(BorderStroke(1.dp, CyberBorderSubtle), RoundedCornerShape(12.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = CyberTextMuted,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Developer Credit as strictly requested by the user prompt:
 *
 * DEVELOPER CREDIT:
 * At the very bottom of the dashboard, display only:
 *
 * Developer: Maybe Sihab
 * +8801646864645
 *
 * Keep this credit small, clean, readable, and visually integrated into the design.
 * Do not add WhatsApp buttons, developer profile buttons, contact buttons, or any extra action to this credit.
 */
@Composable
private fun DeveloperCreditFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Developer: Maybe Sihab",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = CyberTextSecondary,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "+8801646864645",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = CyberTextMuted,
            letterSpacing = 0.5.sp
        )
    }
}
