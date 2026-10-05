package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.SubcomposeAsyncImage
import com.example.model.Message
import com.example.model.MessageType
import com.example.ui.components.AvatarWithStatus
import com.example.ui.components.CyberBadge
import com.example.ui.components.CyberOutlinedButton
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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

    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val isUploadingMedia by viewModel.isUploadingMedia.collectAsState()

    var viewingImageUrl by remember { mutableStateOf<Pair<String, String>?>(null) }
    var viewingVideoUrl by remember { mutableStateOf<Pair<String, String>?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, sizeStr, mime) = queryFileMetadata(context, uri, "photo_${System.currentTimeMillis()}.jpg")
            viewModel.uploadMedia(
                conversationId = conversationId,
                uri = uri,
                type = MessageType.IMAGE,
                fileName = name,
                fileSize = sizeStr,
                mimeType = mime.ifBlank { "image/jpeg" },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, sizeStr, mime) = queryFileMetadata(context, uri, "video_${System.currentTimeMillis()}.mp4")
            viewModel.uploadMedia(
                conversationId = conversationId,
                uri = uri,
                type = MessageType.VIDEO,
                fileName = name,
                fileSize = sizeStr,
                mimeType = mime.ifBlank { "video/mp4" },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, sizeStr, mime) = queryFileMetadata(context, uri, "document_${System.currentTimeMillis()}.bin")
            viewModel.uploadMedia(
                conversationId = conversationId,
                uri = uri,
                type = MessageType.FILE,
                fileName = name,
                fileSize = sizeStr,
                mimeType = mime.ifBlank { "application/octet-stream" },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
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
                val currentUid = Firebase.auth.currentUser?.uid ?: ""
                val peerId = conversation?.participantIds?.firstOrNull { it != currentUid && it.isNotBlank() }
                    ?: conversation?.participantIds?.firstOrNull() ?: ""

                IconButton(onClick = {
                    if (peerId.isNotBlank()) {
                        onStartAudioCall(peerId)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Voice Call",
                        tint = CyberNeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = {
                    if (peerId.isNotBlank()) {
                        onStartVideoCall(peerId)
                    }
                }) {
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
                    onOpenImage = { url, caption -> viewingImageUrl = Pair(url, caption) },
                    onOpenVideo = { url, title -> viewingVideoUrl = Pair(url, title) },
                    onOpenFile = { url, name -> openDocumentUrl(context, url, name) },
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

        // Media Upload Progress Banner
        if (isUploadingMedia) {
            Surface(
                color = CyberBgSurfaceElevated,
                border = BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        progress = { (uploadProgress ?: 0) / 100f },
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = CyberNeonCyan,
                        trackColor = CyberBorderSubtle
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "UPLOADING FILE ${uploadProgress ?: 0}%",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = CyberNeonCyan
                    )
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
                            showAttachmentSheet = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    AttachmentOptionItem(
                        icon = Icons.Default.Videocam,
                        label = "Video",
                        color = CyberElectricEmerald,
                        onClick = {
                            showAttachmentSheet = false
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    )

                    AttachmentOptionItem(
                        icon = Icons.Default.Description,
                        label = "Document",
                        color = CyberAmber,
                        onClick = {
                            showAttachmentSheet = false
                            documentPickerLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Full-Screen Image Viewer Dialog
    if (viewingImageUrl != null) {
        val (imgUrl, caption) = viewingImageUrl!!
        Dialog(
            onDismissRequest = { viewingImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                SubcomposeAsyncImage(
                    model = imgUrl,
                    contentDescription = "Full screen photo",
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CyberNeonCyan)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = caption.ifBlank { "Photo" },
                        color = CyberTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewingImageUrl = null },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberBgSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close photo",
                            tint = CyberNeonCyan
                        )
                    }
                }
            }
        }
    }

    // Full-Screen In-App Video Player Dialog
    if (viewingVideoUrl != null) {
        val (videoUrl, videoTitle) = viewingVideoUrl!!
        Dialog(
            onDismissRequest = { viewingVideoUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = videoTitle.ifBlank { "Video Player" },
                            color = CyberTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewingVideoUrl = null },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberBgSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close video",
                                tint = CyberNeonCyan
                            )
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val mediaController = MediaController(ctx)
                                mediaController.setAnchorView(this)
                                setMediaController(mediaController)
                                setVideoURI(Uri.parse(videoUrl))
                                setOnPreparedListener { mp ->
                                    mp.isLooping = false
                                    start()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CyberOutlinedButton(
                            text = "Open In System Player",
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(videoUrl), "video/*")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No external video player found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            icon = Icons.Default.PlayArrow,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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
    onOpenImage: (String, String) -> Unit = { _, _ -> },
    onOpenVideo: (String, String) -> Unit = { _, _ -> },
    onOpenFile: (String, String) -> Unit = { _, _ -> },
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
                                    .heightIn(min = 130.dp, max = 220.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberBgDark)
                                    .border(BorderStroke(1.dp, CyberBorderSubtle), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (message.mediaUrl.isNotBlank()) {
                                            onOpenImage(message.mediaUrl, message.content.ifEmpty { message.mediaFileName })
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (message.mediaUrl.isNotBlank()) {
                                    SubcomposeAsyncImage(
                                        model = message.mediaUrl,
                                        contentDescription = "Image attachment",
                                        contentScale = ContentScale.Crop,
                                        loading = {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = CyberNeonCyan, modifier = Modifier.size(24.dp))
                                            }
                                        },
                                        error = {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Image, contentDescription = null, tint = CyberNeonCyan, modifier = Modifier.size(32.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = message.mediaFileName.ifEmpty { "Photo" }, color = CyberTextSecondary, fontSize = 11.sp)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
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
                            }
                            if (message.content.isNotBlank() && message.content != "Photo") {
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
                                    .border(BorderStroke(1.dp, CyberElectricEmerald.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (message.mediaUrl.isNotBlank()) {
                                            onOpenVideo(message.mediaUrl, message.mediaFileName)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(CyberElectricEmerald)
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play video",
                                            tint = CyberBgDark,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tap to Play Video",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberElectricEmerald
                                    )
                                }
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
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberBgDark)
                                .border(BorderStroke(1.dp, CyberAmber.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                .clickable {
                                    if (message.mediaUrl.isNotBlank()) {
                                        onOpenFile(message.mediaUrl, message.mediaFileName)
                                    }
                                }
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = CyberAmber,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.mediaFileName.ifEmpty { "attachment.bin" },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyberTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${message.mediaFileSize} • Tap to Open",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = CyberAmber
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

private fun queryFileMetadata(context: Context, uri: Uri, fallbackName: String): Triple<String, String, String> {
    var fileName = fallbackName
    var sizeBytes = 0L
    var mimeType = context.contentResolver.getType(uri) ?: ""

    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) fileName = name
                }
                if (sizeIndex != -1) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }
    } catch (e: Exception) {
        // Fallback
    }

    val formattedSize = when {
        sizeBytes <= 0 -> ""
        sizeBytes < 1024 -> "$sizeBytes B"
        sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
        else -> String.format(java.util.Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
    }

    return Triple(fileName, formattedSize, mimeType)
}

private fun openDocumentUrl(context: Context, url: String, fileName: String) {
    if (url.isBlank()) {
        Toast.makeText(context, "File URL not available", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
    }
}
