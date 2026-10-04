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
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallDirection
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.components.AvatarWithStatus
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberCard
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
import com.example.ui.viewmodel.CallFilter
import com.example.ui.viewmodel.CallViewModel

@Composable
fun CallsScreen(
    viewModel: CallViewModel,
    onNavigateBack: () -> Unit,
    onStartCall: (contactId: String, isVideo: Boolean) -> Unit,
    onNewCallClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val callList by viewModel.filteredCalls.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBgDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewCallClick,
                containerColor = CyberElectricEmerald,
                contentColor = CyberBgDark,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "New Call",
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
                        text = "CALLS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = "Recent voice and video calls",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberElectricEmerald,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Filter (All Calls vs Missed)
            TabRow(
                selectedTabIndex = if (selectedFilter == CallFilter.ALL) 0 else 1,
                containerColor = CyberBgSurface,
                contentColor = CyberNeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (selectedFilter == CallFilter.ALL) 0 else 1]),
                        color = CyberNeonCyan,
                        height = 2.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedFilter == CallFilter.ALL,
                    onClick = { viewModel.setFilter(CallFilter.ALL) },
                    text = {
                        Text(
                            text = "ALL CALLS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (selectedFilter == CallFilter.ALL) CyberNeonCyan else CyberTextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedFilter == CallFilter.MISSED,
                    onClick = { viewModel.setFilter(CallFilter.MISSED) },
                    text = {
                        Text(
                            text = "MISSED ONLY",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (selectedFilter == CallFilter.MISSED) CyberCrimson else CyberTextSecondary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (callList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = CyberTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No recent calls",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = CyberTextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(callList) { call ->
                        CallRecordCard(
                            call = call,
                            onCallBack = { onStartCall(call.contactId, call.callType == CallType.VIDEO) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CallRecordCard(
    call: CallRecord,
    onCallBack: () -> Unit
) {
    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    size = 46.dp,
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
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = directionIcon,
                            contentDescription = null,
                            tint = directionColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${call.formattedDate} • ${if (call.durationSeconds > 0) call.formattedDuration else "Missed call"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onCallBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CyberBgSurfaceElevated)
                        .border(BorderStroke(1.dp, CyberBorderSubtle), CircleShape)
                ) {
                    Icon(
                        imageVector = if (call.callType == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "Return Call",
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Telemetry rating banner
            if (call.durationSeconds > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberBgDark)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = call.networkQualityRating,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = CyberTextSecondary
                    )

                    if (call.wasAdaptiveFallbackTriggered) {
                        CyberBadge(text = "DATA SAVER MODE", color = CyberAmber, hasDot = false)
                    } else {
                        CyberBadge(text = "HD AUDIO", color = CyberElectricEmerald, hasDot = false)
                    }
                }
            }
        }
    }
}
