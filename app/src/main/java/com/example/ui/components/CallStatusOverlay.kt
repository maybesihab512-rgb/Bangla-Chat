package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.NetworkQualityMetrics
import com.example.model.NetworkTier
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberBgSurface
import com.example.ui.theme.CyberBgSurfaceElevated
import com.example.ui.theme.CyberBorderSubtle
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberElectricEmerald
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

/**
 * CallStatusOverlay component displaying real-time connection strength (ping/packet loss/jitter)
 * queried directly from WebRTC Stats API within the active call screen.
 * Supports compact pill mode and expandable HUD diagnostics sheet.
 */
@Composable
fun CallStatusOverlay(
    metrics: NetworkQualityMetrics,
    connectionState: ConnectionState,
    isVideoCall: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val signalColor by animateColorAsState(
        targetValue = when {
            connectionState == ConnectionState.RECONNECTING -> CyberAmber
            metrics.rttMs > 180 || metrics.packetLossPercent > 5f -> CyberCrimson
            metrics.rttMs > 80 || metrics.packetLossPercent > 2f -> CyberAmber
            else -> CyberElectricEmerald
        },
        animationSpec = tween(300),
        label = "signalColor"
    )

    val pingLabel = "${metrics.rttMs}ms"
    val lossLabel = "${String.format("%.1f", metrics.packetLossPercent)}% loss"

    Column(
        modifier = modifier
            .animateContentSize()
            .testTag("call_status_overlay"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Compact Status Pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CyberBgSurface.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, signalColor.copy(alpha = 0.5f)),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { isExpanded = !isExpanded }
                .testTag("call_status_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Signal strength bars (1 to 4 bars)
                ConnectionBars(bars = metrics.bars, activeColor = signalColor)

                // Ping / RTT indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = signalColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = pingLabel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary
                    )
                }

                // Divider dot
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(CyberTextMuted)
                )

                // Packet Loss indicator
                Text(
                    text = lossLabel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (metrics.packetLossPercent > 3f) CyberCrimson else CyberTextSecondary
                )

                // Expand indicator icon
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Connection info",
                    tint = CyberNeonCyan,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Expanded Diagnostics HUD Card
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .testTag("call_status_expanded_card"),
                shape = RoundedCornerShape(12.dp),
                color = CyberBgCard.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NetworkCheck,
                                contentDescription = null,
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WEBRTC REAL-TIME TELEMETRY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberNeonCyan
                            )
                        }

                        CyberBadge(
                            text = when (connectionState) {
                                ConnectionState.CONNECTED -> "SECURE"
                                ConnectionState.RECONNECTING -> "RECONNECT"
                                else -> "CONNECTING"
                            },
                            color = signalColor,
                            hasDot = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(
                            label = "ROUND TRIP (RTT)",
                            value = "${metrics.rttMs} ms",
                            color = signalColor
                        )
                        StatItem(
                            label = "PACKET LOSS",
                            value = "${String.format("%.2f", metrics.packetLossPercent)}%",
                            color = if (metrics.packetLossPercent > 2f) CyberCrimson else CyberElectricEmerald
                        )
                        StatItem(
                            label = "JITTER",
                            value = "${metrics.jitterMs} ms",
                            color = CyberTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(
                            label = "AUDIO CODEC",
                            value = "${metrics.currentAudioBitrateKbps} kbps Opus",
                            color = CyberNeonCyan
                        )
                        if (isVideoCall) {
                            StatItem(
                                label = "VIDEO RESOLUTION",
                                value = metrics.currentResolution,
                                color = CyberElectricEmerald
                            )
                        } else {
                            StatItem(
                                label = "TRANSPORT",
                                value = "DTLS / SRTP",
                                color = CyberElectricEmerald
                            )
                        }
                    }

                    if (metrics.isAudioPriorityActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "⚡ Low-bandwidth data saver active: Audio prioritized",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = CyberAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionBars(bars: Int, activeColor: Color) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val barHeights = listOf(5.dp, 8.dp, 11.dp, 14.dp)
        barHeights.forEachIndexed { index, height ->
            val isActive = (index + 1) <= bars
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isActive) activeColor else CyberBorderSubtle)
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = CyberTextMuted
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
