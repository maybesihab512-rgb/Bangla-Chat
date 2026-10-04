package com.example.ui.components

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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.DeliveryStatus
import com.example.model.NetworkQualityMetrics
import com.example.model.NetworkTier
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.CyberBgSurface
import com.example.ui.theme.CyberBgSurfaceElevated
import com.example.ui.theme.CyberBorderGlow
import com.example.ui.theme.CyberBorderSubtle
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyanSubtle
import com.example.ui.theme.CyberElectricEmerald
import com.example.ui.theme.CyberNeonCyan
import com.example.ui.theme.CyberOnline
import com.example.ui.theme.CyberTextDisabled
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderGlow: Boolean = false,
    borderColor: Color = if (borderGlow) CyberBorderGlow else CyberBorderSubtle,
    backgroundColor: Color = CyberBgCard,
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val cardModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
    } else {
        modifier
            .clip(shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .background(backgroundColor)
    }

    Box(modifier = cardModifier) {
        content()
    }
}

@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accentColor: Color = CyberNeonCyan,
    textColor: Color = CyberBgDark
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = textColor,
            disabledContainerColor = CyberBgSurfaceElevated,
            disabledContentColor = CyberTextMuted
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun CyberOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    borderColor: Color = CyberBorderGlow,
    textColor: Color = CyberNeonCyan
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = textColor
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
fun CyberBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CyberNeonCyan,
    backgroundColor: Color = color.copy(alpha = 0.12f),
    hasDot: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = text,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun AvatarWithStatus(
    initials: String,
    modifier: Modifier = Modifier,
    photoUrl: String? = null,
    colorHex: Long = 0xFF00F0FF,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    showShield: Boolean = false
) {
    val baseColor = Color(colorHex)
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Avatar circle
        if (!photoUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(BorderStroke(1.5.dp, baseColor.copy(alpha = 0.8f)), CircleShape)
            ) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                baseColor.copy(alpha = 0.25f),
                                CyberBgSurfaceElevated
                            )
                        )
                    )
                    .border(BorderStroke(1.5.dp, baseColor.copy(alpha = 0.6f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials.take(2).uppercase().ifEmpty { "U" },
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (size.value * 0.38f).sp
                )
            }
        }

        // Online dot or shield
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.3f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(CyberOnline)
                    .border(BorderStroke(2.dp, CyberBgDark), CircleShape)
            )
        } else if (showShield) {
            Box(
                modifier = Modifier
                    .size(size * 0.34f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(CyberBgDark)
                    .border(BorderStroke(1.dp, CyberNeonCyan), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Verified E2EE",
                    tint = CyberNeonCyan,
                    modifier = Modifier.size(size * 0.22f)
                )
            }
        }
    }
}

@Composable
fun NetworkQualityBadge(
    metrics: NetworkQualityMetrics,
    modifier: Modifier = Modifier
) {
    val tierColor = when (metrics.tier) {
        NetworkTier.EXCELLENT -> CyberElectricEmerald
        NetworkTier.GOOD -> CyberNeonCyan
        NetworkTier.CONGESTED -> CyberAmber
        NetworkTier.CRITICAL_WEAK -> CyberCrimson
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = CyberBgSurfaceElevated,
        border = BorderStroke(1.dp, tierColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal bars
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(12.dp)
            ) {
                for (i in 1..4) {
                    val barHeight = (i * 3).dp
                    val isLit = i <= metrics.bars
                    Box(
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .width(2.5.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isLit) tierColor else CyberTextDisabled)
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${metrics.rttMs}ms • ${metrics.currentAudioBitrateKbps}kbps",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = tierColor
            )
            if (metrics.isAudioPriorityActive) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(CyberAmber.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "PRIORITY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberAmber
                    )
                }
            }
        }
    }
}

@Composable
fun DeliveryTick(
    status: DeliveryStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        DeliveryStatus.SENDING -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Sending",
                tint = CyberTextMuted,
                modifier = modifier.size(13.dp)
            )
        }
        DeliveryStatus.SENT -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = CyberTextSecondary,
                modifier = modifier.size(13.dp)
            )
        }
        DeliveryStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = CyberTextSecondary,
                modifier = modifier.size(14.dp)
            )
        }
        DeliveryStatus.SEEN -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Seen",
                tint = CyberNeonCyan,
                modifier = modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun CyberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp),
            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
            placeholder = { Text(placeholder, color = CyberTextMuted, style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CyberBgCard,
                unfocusedContainerColor = CyberBgSurface,
                focusedBorderColor = CyberNeonCyan,
                unfocusedBorderColor = CyberBorderSubtle,
                focusedLabelColor = CyberNeonCyan,
                unfocusedLabelColor = CyberTextSecondary,
                focusedTextColor = CyberTextPrimary,
                unfocusedTextColor = CyberTextPrimary,
                cursorColor = CyberNeonCyan,
                errorBorderColor = CyberCrimson
            )
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = CyberCrimson,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}
