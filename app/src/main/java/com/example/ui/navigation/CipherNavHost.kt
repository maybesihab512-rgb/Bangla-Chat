package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CipherAppContainer
import com.example.data.repository.AuthStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.CreateProfileScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OtpVerificationScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
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
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.CallViewModel
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.SettingsViewModel

sealed class AppDestination {
    object Splash : AppDestination()
    object Login : AppDestination()
    data class Otp(val phone: String) : AppDestination()
    object CreateProfile : AppDestination()
    object Dashboard : AppDestination()
    object Chats : AppDestination()
    data class ChatDetail(val conversationId: String) : AppDestination()
    object Calls : AppDestination()
    data class ActiveCall(val contactId: String, val isVideo: Boolean) : AppDestination()
    object Contacts : AppDestination()
    object Profile : AppDestination()
    object Settings : AppDestination()
}

@Composable
fun CipherNavHost(
    modifier: Modifier = Modifier
) {
    // ViewModels initialized with shared container repositories
    val authViewModel = remember { AuthViewModel(CipherAppContainer.authRepository) }
    val chatViewModel = remember {
        ChatViewModel(
            CipherAppContainer.chatRepository,
            CipherAppContainer.userRepository
        )
    }
    val callViewModel = remember {
        CallViewModel(
            CipherAppContainer.callRepository,
            CipherAppContainer.callingService,
            CipherAppContainer.userRepository
        )
    }
    val dashboardViewModel = remember {
        DashboardViewModel(
            CipherAppContainer.authRepository,
            CipherAppContainer.chatRepository,
            CipherAppContainer.callRepository,
            CipherAppContainer.userRepository,
            CipherAppContainer.callingService
        )
    }
    val settingsViewModel = remember { SettingsViewModel(CipherAppContainer.settingsRepository) }

    val initialDestination = remember {
        if (authViewModel.hasValidSession()) {
            AppDestination.Dashboard
        } else {
            AppDestination.Splash
        }
    }
    var currentDestination by remember { mutableStateOf<AppDestination>(initialDestination) }
    var backStack by remember { mutableStateOf<List<AppDestination>>(emptyList()) }

    fun navigateTo(dest: AppDestination) {
        backStack = backStack + currentDestination
        currentDestination = dest
    }

    fun navigateBack() {
        if (backStack.isNotEmpty()) {
            val prev = backStack.last()
            backStack = backStack.dropLast(1)
            currentDestination = prev
        } else {
            currentDestination = AppDestination.Dashboard
        }
    }

    fun exitCallScreen() {
        if (currentDestination is AppDestination.ActiveCall) {
            if (backStack.isNotEmpty()) {
                val prev = backStack.last()
                backStack = backStack.dropLast(1)
                currentDestination = if (prev is AppDestination.ActiveCall) AppDestination.Dashboard else prev
            } else {
                currentDestination = AppDestination.Dashboard
            }
        }
    }

    val authStatus by authViewModel.authStatus.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val activeCallSession by callViewModel.activeSession.collectAsState()

    // Handle authentication session transitions automatically
    LaunchedEffect(authStatus) {
        val hasSession = authViewModel.hasValidSession()
        if (hasSession && (currentDestination is AppDestination.Login || currentDestination is AppDestination.Splash)) {
            currentDestination = AppDestination.Dashboard
            backStack = emptyList()
        } else if (!hasSession && currentDestination !in listOf(AppDestination.Login, AppDestination.Splash)) {
            if (currentDestination !is AppDestination.Otp) {
                currentDestination = AppDestination.Login
                backStack = emptyList()
            }
        }
    }

    // Show bottom navigation bar on primary tabs
    val showBottomBar = currentDestination in listOf(
        AppDestination.Dashboard,
        AppDestination.Chats,
        AppDestination.Calls,
        AppDestination.Contacts,
        AppDestination.Profile
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBgDark,
        bottomBar = {
            if (showBottomBar) {
                CipherBottomBar(
                    currentDestination = currentDestination,
                    onSelect = { dest ->
                        if (currentDestination != dest) {
                            currentDestination = dest
                        }
                    },
                    unreadChatCount = dashboardState.totalUnreadMessages,
                    missedCallCount = dashboardState.missedCallsCount
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { dest ->
                when (dest) {
                    is AppDestination.Splash -> {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val activity = context as? android.app.Activity
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            if (activity != null) {
                                authViewModel.attemptAutoSignIn(activity)
                            }
                        }
                        SplashScreen(
                            onNavigateNext = {
                                if (authViewModel.hasValidSession() || (authStatus is AuthStatus.Success && authViewModel.currentUser.value != null)) {
                                    currentDestination = AppDestination.Dashboard
                                } else {
                                    currentDestination = AppDestination.Login
                                }
                            }
                        )
                    }

                    is AppDestination.Login -> {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val activity = context as? android.app.Activity
                        LoginScreen(
                            authStatus = authStatus,
                            onContinueWithGoogle = {
                                if (activity != null) {
                                    authViewModel.continueWithGoogle(
                                        activity = activity,
                                        onComplete = { currentDestination = AppDestination.Dashboard }
                                    )
                                }
                            },
                            onContinueWithPhone = { phone ->
                                if (activity != null) {
                                    authViewModel.requestPhoneOtp(
                                        activity = activity,
                                        phoneNumber = phone,
                                        onCodeSent = { navigateTo(AppDestination.Otp(phone)) },
                                        onError = { /* Error state captured in authStatus */ }
                                    )
                                }
                            }
                        )
                    }

                    is AppDestination.Otp -> {
                        BackHandler { navigateBack() }
                        OtpVerificationScreen(
                            phoneNumber = dest.phone,
                            onVerifyCode = { code -> authViewModel.verifyOtp(code, dest.phone) },
                            onVerifySuccess = { currentDestination = AppDestination.Dashboard },
                            onBack = { navigateBack() }
                        )
                    }

                    is AppDestination.CreateProfile -> {
                        BackHandler { navigateBack() }
                        val currentUser by authViewModel.currentUser.collectAsState()
                        CreateProfileScreen(
                            currentUser = currentUser,
                            onSaveProfile = { name, handle, status ->
                                authViewModel.completeProfile(name, handle, status)
                                currentDestination = AppDestination.Dashboard
                            }
                        )
                    }

                    is AppDestination.Dashboard -> {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onNavigateToChats = { currentDestination = AppDestination.Chats },
                            onNavigateToCalls = { currentDestination = AppDestination.Calls },
                            onNavigateToContacts = { currentDestination = AppDestination.Contacts },
                            onNavigateToProfile = { currentDestination = AppDestination.Profile },
                            onNavigateToSettings = { navigateTo(AppDestination.Settings) },
                            onOpenConversation = { convId ->
                                chatViewModel.selectConversation(convId)
                                navigateTo(AppDestination.ChatDetail(convId))
                            },
                            onStartCall = { contactId, isVideo ->
                                callViewModel.startCallById(contactId, isVideo)
                                navigateTo(AppDestination.ActiveCall(contactId, isVideo))
                            }
                        )
                    }

                    is AppDestination.Chats -> {
                        BackHandler { currentDestination = AppDestination.Dashboard }
                        ChatsScreen(
                            viewModel = chatViewModel,
                            onOpenConversation = { convId ->
                                chatViewModel.selectConversation(convId)
                                navigateTo(AppDestination.ChatDetail(convId))
                            },
                            onNavigateBack = { currentDestination = AppDestination.Dashboard },
                            onNewChatClick = { currentDestination = AppDestination.Contacts }
                        )
                    }

                    is AppDestination.ChatDetail -> {
                        BackHandler { navigateBack() }
                        ChatDetailScreen(
                            conversationId = dest.conversationId,
                            viewModel = chatViewModel,
                            onNavigateBack = { navigateBack() },
                            onStartAudioCall = { contactId ->
                                callViewModel.startCallById(contactId, false)
                                navigateTo(AppDestination.ActiveCall(contactId, false))
                            },
                            onStartVideoCall = { contactId ->
                                callViewModel.startCallById(contactId, true)
                                navigateTo(AppDestination.ActiveCall(contactId, true))
                            }
                        )
                    }

                    is AppDestination.Calls -> {
                        BackHandler { currentDestination = AppDestination.Dashboard }
                        CallsScreen(
                            viewModel = callViewModel,
                            onNavigateBack = { currentDestination = AppDestination.Dashboard },
                            onNewCallClick = { navigateTo(AppDestination.Contacts) },
                            onStartCall = { contactId, isVideo ->
                                callViewModel.startCallById(contactId, isVideo)
                                navigateTo(AppDestination.ActiveCall(contactId, isVideo))
                            }
                        )
                    }

                    is AppDestination.ActiveCall -> {
                        BackHandler {
                            callViewModel.endCall()
                            exitCallScreen()
                        }
                        ActiveCallScreen(
                            viewModel = callViewModel,
                            onCallTerminated = { exitCallScreen() }
                        )
                    }

                    is AppDestination.Contacts -> {
                        BackHandler { currentDestination = AppDestination.Dashboard }
                        ContactsScreen(
                            userRepository = CipherAppContainer.userRepository,
                            onNavigateBack = { currentDestination = AppDestination.Dashboard },
                            onStartChat = { user ->
                                val convId = chatViewModel.startChatWithContact(user)
                                chatViewModel.selectConversation(convId)
                                navigateTo(AppDestination.ChatDetail(convId))
                            },
                            onStartCall = { user, isVideo ->
                                callViewModel.startCall(user, if (isVideo) com.example.model.CallType.VIDEO else com.example.model.CallType.AUDIO)
                                navigateTo(AppDestination.ActiveCall(user.id, isVideo))
                            }
                        )
                    }

                    is AppDestination.Profile -> {
                        BackHandler { currentDestination = AppDestination.Dashboard }
                        ProfileScreen(
                            authViewModel = authViewModel,
                            onNavigateBack = { currentDestination = AppDestination.Dashboard },
                            onEditProfileClick = { navigateTo(AppDestination.CreateProfile) }
                        )
                    }

                    is AppDestination.Settings -> {
                        BackHandler { navigateBack() }
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateBack = { navigateBack() }
                        )
                    }
                }
            }

            // Global Incoming Call Overlay
            if (activeCallSession != null && activeCallSession!!.isIncoming && activeCallSession!!.connectionState == com.example.model.ConnectionState.CONNECTING) {
                IncomingCallOverlay(
                    session = activeCallSession!!,
                    onAccept = {
                        val session = activeCallSession!!
                        callViewModel.answerCall()
                        navigateTo(AppDestination.ActiveCall(session.contactId, session.callType == com.example.model.CallType.VIDEO))
                    },
                    onDecline = {
                        callViewModel.declineCall()
                    }
                )
            }
        }
    }
}

@Composable
private fun CipherBottomBar(
    currentDestination: AppDestination,
    onSelect: (AppDestination) -> Unit,
    unreadChatCount: Int,
    missedCallCount: Int
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = CyberBgSurface,
        tonalElevation = 0.dp
    ) {
        BottomNavItem(
            label = "Overview",
            icon = Icons.Default.Dashboard,
            isSelected = currentDestination == AppDestination.Dashboard,
            badgeCount = 0,
            onClick = { onSelect(AppDestination.Dashboard) }
        )

        BottomNavItem(
            label = "Chats",
            icon = Icons.Default.Chat,
            isSelected = currentDestination == AppDestination.Chats,
            badgeCount = unreadChatCount,
            onClick = { onSelect(AppDestination.Chats) }
        )

        BottomNavItem(
            label = "Calls",
            icon = Icons.Default.Call,
            isSelected = currentDestination == AppDestination.Calls,
            badgeCount = missedCallCount,
            onClick = { onSelect(AppDestination.Calls) }
        )

        BottomNavItem(
            label = "Contacts",
            icon = Icons.Default.Contacts,
            isSelected = currentDestination == AppDestination.Contacts,
            badgeCount = 0,
            onClick = { onSelect(AppDestination.Contacts) }
        )

        BottomNavItem(
            label = "Profile",
            icon = Icons.Default.Person,
            isSelected = currentDestination == AppDestination.Profile,
            badgeCount = 0,
            onClick = { onSelect(AppDestination.Profile) }
        )
    }
}

@Composable
private fun RowScope.BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = {
            if (badgeCount > 0) {
                BadgedBox(
                    badge = {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CyberNeonCyan)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = badgeCount.toString(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberBgDark
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        label = {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                letterSpacing = 0.5.sp
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = CyberNeonCyan,
            unselectedIconColor = CyberTextSecondary,
            selectedTextColor = CyberNeonCyan,
            unselectedTextColor = CyberTextMuted,
            indicatorColor = CyberBgSurfaceElevated
        )
    )
}

@Composable
private fun IncomingCallOverlay(
    session: com.example.data.calling.ActiveCallSession,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBgDark.copy(alpha = 0.96f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            com.example.ui.components.AvatarWithStatus(
                initials = session.contactAvatarInitials,
                colorHex = session.avatarColorHex,
                size = 100.dp,
                isOnline = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = session.contactName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CyberTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (session.callType == com.example.model.CallType.VIDEO) "Incoming Video Call..." else "Incoming Voice Call...",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberNeonCyan,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.example.ui.components.CyberBadge(text = "END-TO-END ENCRYPTED", color = CyberElectricEmerald)
                com.example.ui.components.CyberBadge(text = "HD AUDIO", color = CyberNeonCyan)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Accept & Decline buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(CyberCrimson)
                            .clickable(onClick = onDecline),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Decline",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberTextSecondary
                    )
                }

                // Accept button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(CyberElectricEmerald)
                            .clickable(onClick = onAccept),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (session.callType == com.example.model.CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = "Accept Call",
                            tint = CyberBgDark,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Accept",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberElectricEmerald
                    )
                }
            }
        }
    }
}

