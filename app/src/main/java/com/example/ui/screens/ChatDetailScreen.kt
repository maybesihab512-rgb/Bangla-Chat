package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.Message
import com.example.model.MessageType
import com.example.ui.components.AvatarWithStatus
import com.example.ui.components.CyberBadge
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
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onStartAudioCall: (String) -> Unit,
    onStartVideoCall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val conversation = remember(conversationId) { viewModel.getConversation(conversationId) }
    val messagesFlow = remember(conversationId) { viewModel.getMessages(conversationId) }
    val messages by messagesFlow.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    val isRecordingAudio by viewModel.isRecordingAudio.collectAsState()
    val recordingSeconds by viewModel.audioRecordingSeconds.collectAsState()
    val isTypingFlow = remember(conversationId) { viewModel.observeTypingForConversation(conversationId) }
    val isTyping by isTypingFlow.collectAsState()

    val context = LocalContext.current
    val playingMessageId by viewModel.audioPlaybackManager.currentPlayingMessageId.collectAsState()
    val isPlayingAudio by viewModel.audioPlaybackManager.isPlaying.collectAsState()
    val audioProgress by viewModel.audioPlaybackManager.progress.collectAsState()

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecording()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAudio()
        }
    }

    var textInput by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showSecurityKeyDialog by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<Message?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }
    var messageSearchQuery by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val displayMessages = if (searchMode && messageSearchQuery.isNotBlank()) {
        messages.filter { it.content.contains(messageSearchQuery, ignoreCase = true) }
    } else messages

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Chat Header
        Surface(
            color = CyberBgSurface,
            border = BorderStroke(1.dp, CyberBorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyberNeonCyan
                    )
                }

                AvatarWithStatus(
                    initials = conversation?.avatarInitials ?: "SC",
                    colorHex = conversation?.avatarColorHex ?: 0xFF00F0FF,
                    size = 40.dp,
                    isOnline = conversation?.isOnline == true
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = conversation?.title ?: "Chat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (conversation?.isOnline == true) "Online" else "Offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (conversation?.isOnline == true) CyberElectricEmerald else CyberTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Security fingerprint icon
                IconButton(onClick = { showSecurityKeyDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Verify Encryption",
                        tint = CyberElectricEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Call Actions
                IconButton(onClick = { onStartAudioCall(conversation?.participantIds?.firstOrNull { it != "user_me" } ?: "usr_peer") }) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Voice Call",
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = { onStartVideoCall(conversation?.participantIds?.firstOrNull { it != "user_me" } ?: "usr_peer") }) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video Call",
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Encryption banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberBgSurfaceElevated)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "MESSAGES ARE END-TO-END ENCRYPTED",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = CyberNeonCyan.copy(alpha = 0.8f),
                letterSpacing = 0.5.sp
            )
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (displayMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberBgCard)
                                .border(BorderStroke(1.dp, CyberBorderSubtle), RoundedCornerShape(14.dp))
                                .padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CyberElectricEmerald,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "END-TO-END ENCRYPTED",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CyberElectricEmerald
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Messages in this chat are end-to-end encrypted.\nSend a message to start chatting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(displayMessages) { msg ->
                MessageBubble(
                    message = msg,
                    isAudioPlaying = isPlayingAudio && playingMessageId == msg.id,
                    isAudioActive = playingMessageId == msg.id,
                    audioProgress = if (playingMessageId == msg.id) audioProgress else 0f,
                    onPlayAudio = { audioUrl -> viewModel.playAudio(msg.id, audioUrl) },
                    onLongClick = { selectedMessageForAction = msg },
                    onReply = { viewModel.setReplyingTo(msg) }
                )
            }

            if (isTyping) {
                item {
                    TypingIndicatorBubble(peerName = conversation?.title ?: "Contact")
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Reply Banner
        if (replyingTo != null) {
            Surface(
                color = CyberBgSurfaceElevated,
                border = BorderStroke(1.dp, CyberBorderGlow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replying to ${replyingTo?.senderName}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = CyberNeonCyan
                        )
                        Text(
                            text = replyingTo?.content ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberTextSecondary,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setReplyingTo(null) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel reply",
                            tint = CyberTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Recording Audio Mode Bar
        if (isRecordingAudio) {
            Surface(
                color = CyberBgSurface,
                border = BorderStroke(1.dp, CyberCrimson),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(CyberCrimson)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RECORDING VOICE MESSAGE 00:${String.format("%02d", recordingSeconds)}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CyberCrimson
                        )
                    }

                    Row {
                        IconButton(onClick = { viewModel.cancelVoiceRecording() }) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = CyberTextMuted)
                        }
                        IconButton(onClick = { viewModel.stopVoiceRecordingAndSend(conversationId) }) {
                            Icon(Icons.Default.Stop, contentDescription = "Send Memo", tint = CyberElectricEmerald)
                        }
                    }
                }
            }
        } else {
            // Standard Composer Bar
            Surface(
                color = CyberBgSurface,
                border = BorderStroke(1.dp, CyberBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAttachmentSheet = true },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach media",
                            tint = CyberNeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                text = "Message...",
                                color = CyberTextMuted,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CyberBgCard,
                            unfocusedContainerColor = CyberBgCard,
                            focusedBorderColor = CyberNeonCyan,
                            unfocusedBorderColor = CyberBorderSubtle,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary,
                            cursorColor = CyberNeonCyan
                        ),
                        singleLine = false,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )

                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.sendMessage(
                                    conversationId = conversationId,
                                    content = textInput.trim()
                                )
                                textInput = ""
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CyberNeonCyan)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = CyberBgDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val hasMicPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMicPermission) {
                                    viewModel.startVoiceRecording()
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CyberBgSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Voice Memo",
                                tint = CyberNeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Attachment Modal Bottom Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = CyberBgSurface,
            contentColor = CyberTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "SHARE MEDIA",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = CyberNeonCyan,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOptionItem(
                        icon = Icons.Default.Image,
                        label = "Photo",
                        color = CyberNeonCyan,
                        onClick = {
                            viewModel.sendMessage(
                                conversationId = conversationId,
                                content = "Shared a photo",
                                type = MessageType.IMAGE,
                                mediaFileName = "photo_${System.currentTimeMillis() % 1000}.png",
                                mediaFileSize = "620 KB"
                            )
                            showAttachmentSheet = false
                        }
                    )

                    AttachmentOptionItem(
                        icon = Icons.Default.Videocam,
                        label = "Video",
                        color = CyberElectricEmerald,
                        onClick = {
                            viewModel.sendMessage(
                                conversationId = conversationId,
                                content = "Shared a video",
                                type = MessageType.VIDEO,
                                mediaFileName = "video_${System.currentTimeMillis() % 1000}.mp4",
                                mediaFileSize = "2.4 MB",
                                mediaDuration = 32
                            )
                            showAttachmentSheet = false
                        }
                    )

                    AttachmentOptionItem(
                        icon = Icons.Default.Description,
                        label = "Document",
                        color = CyberAmber,
                        onClick = {
                            viewModel.sendMessage(
                                conversationId = conversationId,
                                content = "Shared a document",
                                type = MessageType.FILE,
                                mediaFileName = "document.pdf",
                                mediaFileSize = "1.2 MB"
                            )
                            showAttachmentSheet = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Security Verification Dialog
    if (showSecurityKeyDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityKeyDialog = false },
            title = {
                Text(
                    text = "SECURITY VERIFICATION",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = CyberElectricEmerald
                )
            },
            text = {
                Column {
                    Text(
                        text = "Safety Number:",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberBgCard)
                            .border(BorderStroke(1.dp, CyberBorderSubtle), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "7F82 : AA19 : 92C3\n4081 : EE01 : 5512\n9F30 : 0081 : 41BA",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CyberNeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Compare this safety number with your contact to verify that messages and calls are encrypted end-to-end.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberTextMuted
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecurityKeyDialog = false }) {
                    Text("Verified", color = CyberElectricEmerald)
                }
            },
            containerColor = CyberBgSurface,
            textContentColor = CyberTextPrimary
        )
    }

    // Message Action Modal (Reply, Forward, Delete)
    if (selectedMessageForAction != null) {
        val targetMsg = selectedMessageForAction!!
        AlertDialog(
            onDismissRequest = { selectedMessageForAction = null },
            title = {
                Text(
                    text = "MESSAGE ACTIONS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = CyberNeonCyan
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "\"${targetMsg.content}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberTextSecondary,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setReplyingTo(targetMsg)
                                selectedMessageForAction = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = CyberNeonCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Reply to Message", color = CyberTextPrimary)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Forward
                                selectedMessageForAction = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Forward, contentDescription = null, tint = CyberElectricEmerald)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Forward Message", color = CyberTextPrimary)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDeleteConfirmDialog = true
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = CyberCrimson)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Delete Message", color = CyberCrimson)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForAction = null }) {
                    Text("Close", color = CyberTextMuted)
                }
            },
            containerColor = CyberBgSurface
        )
    }

    // Delete Confirmation
    if (showDeleteConfirmDialog && selectedMessageForAction != null) {
        val targetMsg = selectedMessageForAction!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete message?", color = CyberTextPrimary) },
            text = { Text("Do you want to delete this message for yourself or for everyone?", color = CyberTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMessage(conversationId, targetMsg.id, forEveryone = true)
                    showDeleteConfirmDialog = false
                    selectedMessageForAction = null
                }) {
                    Text("Delete for Everyone", color = CyberCrimson)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.deleteMessage(conversationId, targetMsg.id, forEveryone = false)
                    showDeleteConfirmDialog = false
                    selectedMessageForAction = null
                }) {
                    Text("Delete for Me", color = CyberTextPrimary)
                }
            },
            containerColor = CyberBgSurface
        )
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isAudioPlaying: Boolean = false,
    isAudioActive: Boolean = false,
    audioProgress: Float = 0f,
    onPlayAudio: (String) -> Unit = {},
    onLongClick: () -> Unit,
    onReply: () -> Unit
) {
    val isOutgoing = message.isOutgoing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongClick() },
                        onDoubleTap = { onReply() }
                    )
                },
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isOutgoing) 14.dp else 2.dp,
                bottomEnd = if (isOutgoing) 2.dp else 14.dp
            ),
            color = if (isOutgoing) CyberBgCard else CyberBgSurfaceElevated,
            border = BorderStroke(
                1.dp,
                if (isOutgoing) CyberNeonCyan.copy(alpha = 0.35f) else CyberBorderSubtle
            )
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Reply quote header
                if (message.replyToMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberBgDark.copy(alpha = 0.6f))
                            .border(BorderStroke(0.5.dp, CyberNeonCyan), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Text(
                                text = message.replyToMessage.senderName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberNeonCyan
                            )
                            Text(
                                text = message.replyToMessage.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberTextSecondary,
                                maxLines = 1,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Message payload based on type
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message.isDeleted) CyberTextMuted else CyberTextPrimary,
                            fontStyle = if (message.isDeleted) androidx.compose.ui.text.font.FontStyle.Italic else null
                        )
                    }
                    MessageType.AUDIO -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isAudioPlaying) CyberCrimson else CyberNeonCyan)
                                    .clickable {
                                        val audioSource = message.mediaUrl.ifEmpty { message.mediaFileName }
                                        onPlayAudio(audioSource)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isAudioPlaying) "Pause voice memo" else "Play voice memo",
                                    tint = CyberBgDark,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.widthIn(min = 130.dp, max = 220.dp)) {
                                if (isAudioActive) {
                                    LinearProgressIndicator(
                                        progress = { audioProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = CyberNeonCyan,
                                        trackColor = CyberBorderSubtle
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = CyberNeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "${message.mediaDurationSeconds}s • ${message.mediaFileSize.ifEmpty { "Audio memo" }}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = CyberTextSecondary
                                )
                            }
                        }
                    }
                    MessageType.IMAGE -> {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberBgDark)
                                    .border(BorderStroke(1.dp, CyberBorderSubtle), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = CyberNeonCyan,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message.mediaFileName.ifEmpty { "Photo" },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = CyberTextSecondary
                                    )
                                }
                            }
                            if (message.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyberTextPrimary
                                )
                            }
                        }
                    }
                    MessageType.VIDEO -> {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberBgDark)
                                    .border(BorderStroke(1.dp, CyberBorderSubtle), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = CyberElectricEmerald,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${message.mediaFileName} (${message.mediaFileSize})",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = CyberTextSecondary
                            )
                        }
                    }
                    MessageType.FILE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberBgDark)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = CyberAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = message.mediaFileName.ifEmpty { "attachment.bin" },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = CyberTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = message.mediaFileSize,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = CyberTextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer with time & tick
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.formattedTime,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = CyberTextMuted
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        DeliveryTick(status = message.deliveryStatus)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(BorderStroke(1.dp, color), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = CyberTextSecondary
        )
    }
}

@Composable
private fun TypingIndicatorBubble(peerName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CyberBgSurfaceElevated,
            border = BorderStroke(1.dp, CyberBorderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$peerName is typing",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CyberNeonCyan
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CyberNeonCyan)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CyberElectricEmerald)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CyberNeonCyan)
                )
            }
        }
    }
}
