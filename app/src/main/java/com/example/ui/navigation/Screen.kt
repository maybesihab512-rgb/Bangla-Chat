package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    data class Otp(val phoneNumber: String) : Screen("otp/{phoneNumber}") {
        companion object {
            const val ROUTE = "otp"
        }
    }
    object CreateProfile : Screen("create_profile")
    object Dashboard : Screen("dashboard")
    object Chats : Screen("chats")
    data class ChatDetail(val conversationId: String) : Screen("chat_detail/{conversationId}")
    object Calls : Screen("calls")
    data class ActiveCall(val contactId: String, val isVideo: Boolean) : Screen("active_call/{contactId}/{isVideo}")
    object Contacts : Screen("contacts")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
}
