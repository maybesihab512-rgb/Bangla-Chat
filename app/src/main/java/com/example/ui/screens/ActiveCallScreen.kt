package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.calling.ActiveCallSession
import com.example.model.CallType
import com.example.model.ConnectionState
import com.example.ui.components.AvatarWithStatus
import com.example.ui.components.CallStatusOverlay
import com.example.ui.components.CyberBadge
import com.example.ui.components.NetworkQualityBadge
import com.example.ui.theme.CyberAmber
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
import com.example.ui.viewmodel.CallViewModel
import org.webrtc.SurfaceViewRenderer

@Composable
fun ActiveCallScreen(
    viewModel: CallViewModel,
    onCallTerminated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session by viewModel.activeSession.collectAsState()
    val hasRemoteVideo by viewModel.hasRemoteVideo.collectAsState()

    // Safely trigger navigation back when call terminates without re-entrant loop
    LaunchedEffect(session) {
        if (session == null) {
            onCallTerminated()
        }
    }

    if (session == null) {
        return
    }

    val active = session!!
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "callingPulse"
    )

    val permissionsToRequest = remember(active.callType) {
        if (active.callType == CallType.VIDEO) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }
    val callPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!micGranted) {
            Log.w("ActiveCallScreen", "Microphone permission denied")
        }
    }

    LaunchedEffect(active.callType) {
        val hasAll = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasAll) {
            callPermissionsLauncher.launch(permissionsToRequest)
        }
    }

    val isVideoCall = active.callType == CallType.VIDEO

    val durationText = String.format(
        "%02d:%02d",
        active.durationSeconds / 60,
        active.durationSeconds % 60
    )

    val connectionLabel = when (active.connectionState) {
        ConnectionState.CONNECTING -> "CONNECTING..."
        ConnectionState.SECURE_HANDSHAKE -> "SECURING CALL..."
        ConnectionState.CONNECTED -> "CONNECTED ($durationText)"
        ConnectionState.RECONNECTING -> "RECONNECTING..."
        else -> "CALL ENDED"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // VIDEO CALL: Real WebRTC SurfaceViewRenderers
        if (isVideoCall) {
            // 1. Full-screen Remote Video Surface
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            viewModel.attachRemoteVideoRenderer(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { renderer ->
                        viewModel.detachRemoteVideoRenderer(renderer)
                    }
                )

                // Placeholder / Waiting HUD when remote video has not arrived yet
                if (!hasRemoteVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        CyberBgSurfaceElevated.copy(alpha = 0.95f),
                                        CyberBgDark.copy(alpha = 0.95f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .scale(pulseScale),
                                contentAlignment = Alignment.Center
                            ) {
                                AvatarWithStatus(
                                    initials = active.contactAvatarInitials,
                                    colorHex = active.avatarColorHex,
                                    size = 110.dp,
                                    isOnline = true
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (active.connectionState == ConnectionState.CONNECTED)
                                    "Waiting for remote video stream..."
                                else
                                    "Connecting secure video call...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberNeonCyan
                            )
                        }
                    }
                }
            }

            // 2. Picture-in-Picture (PiP) Local Video Surface
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 110.dp, end = 16.dp)
                    .size(width = 110.dp, height = 150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberBgDark)
                    .border(BorderStroke(1.5.dp, CyberNeonCyan), RoundedCornerShape(14.dp))
            ) {
                if (active.isVideoMuted) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = "Camera Muted",
                            tint = CyberTextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Camera Off",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberTextMuted
                        )
                    }
                } else {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceViewRenderer(ctx).apply {
                                viewModel.attachLocalVideoRenderer(this)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        onRelease = { renderer ->
                            viewModel.detachLocalVideoRenderer(renderer)
                        }
                    )
                }
            }
        }

        // Top HUD Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CyberBadge(
                    text = "ENCRYPTED",
                    color = CyberElectricEmerald
                )

                NetworkQualityBadge(metrics = active.metrics)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real-time WebRTC Connection Strength Overlay
            CallStatusOverlay(
                metrics = active.metrics,
                connectionState = active.connectionState,
                isVideoCall = isVideoCall,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isVideoCall) {
                // Audio Avatar with futuristic pulse
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(if (active.connectionState == ConnectionState.CONNECTED) 1f else pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(CyberBgSurface)
                            .border(BorderStroke(1.5.dp, CyberNeonCyan.copy(alpha = 0.5f)), CircleShape)
                    )
                    AvatarWithStatus(
                        initials = active.contactAvatarInitials,
                        colorHex = active.avatarColorHex,
                        size = 100.dp,
                        isOnline = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = active.contactName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CyberTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = connectionLabel,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (active.connectionState == ConnectionState.CONNECTED) CyberElectricEmerald else CyberNeonCyan,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Adaptive Bitrate / Weak Network Mode Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CyberBgSurfaceElevated,
                border = BorderStroke(1.dp, CyberBorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Call Quality: ${if (active.metrics.bars >= 3) "Good" else "Weak"}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CyberTextSecondary
                    )

                    if (active.metrics.isAudioPriorityActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[DATA SAVER ACTIVE]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = CyberAmber
                        )
                    }
                }
            }
        }

        // Bottom Controls HUD
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Audio Priority manual override toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberBgSurface)
                    .border(
                        BorderStroke(
                            1.dp,
                            if (active.manualAudioPriorityOverride) CyberAmber else CyberBorderSubtle
                        ),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        viewModel.toggleAudioPriorityMode(!active.manualAudioPriorityOverride)
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = if (active.manualAudioPriorityOverride) CyberAmber else CyberTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (active.manualAudioPriorityOverride) "Data Saver: ON" else "Data Saver: AUTO",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active.manualAudioPriorityOverride) CyberAmber else CyberTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic toggle
                CallControlButton(
                    icon = if (active.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (active.isMicMuted) "Unmute" else "Mute",
                    isActive = active.isMicMuted,
                    onClick = { viewModel.toggleMic() }
                )

                // Video toggle (only on video calls or to enable video)
                if (isVideoCall) {
                    CallControlButton(
                        icon = if (active.isVideoMuted) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        label = if (active.isVideoMuted) "Video Off" else "Video On",
                        isActive = active.isVideoMuted,
                        onClick = { viewModel.toggleVideo() }
                    )
                }

                // Speaker toggle
                CallControlButton(
                    icon = if (active.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    label = if (active.isSpeakerOn) "Speaker" else "Earpiece",
                    isActive = active.isSpeakerOn,
                    onClick = { viewModel.toggleSpeaker() }
                )

                // Flip Camera (only on video calls)
                if (isVideoCall) {
                    CallControlButton(
                        icon = Icons.Default.Cameraswitch,
                        label = "Flip",
                        isActive = false,
                        onClick = { viewModel.switchCamera() }
                    )
                }

                // End Call Button
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(CyberCrimson)
                        .clickable {
                            viewModel.endCall()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = CyberTextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isActive) CyberNeonCyan else CyberBgSurfaceElevated)
                .border(
                    BorderStroke(1.dp, if (isActive) CyberNeonCyan else CyberBorderSubtle),
                    CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) CyberBgDark else CyberTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = CyberTextSecondary
        )
    }
}
