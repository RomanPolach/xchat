package com.example.xchat2.chat

sealed class ChatBottomSheetState {
    object Closed: ChatBottomSheetState()

    data class SmileScreen(val smileResources: IntRange = 1..5648): ChatBottomSheetState()

    data class RoomInfo(val users: List<ChatUser>, val admin: String, val idleTime: String): ChatBottomSheetState()
}