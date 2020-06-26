package com.example.xchat2.ui.main.repos

import com.example.xchat2.chat.ChatBottomSheetState
import com.example.xchat2.util.Event
import com.example.xchat2.util.State
import kotlin.String

data class ChatRoomContent(
    val roomHtml: String,
    val roomExitState: Event<Boolean?> = Event.createDefaultState(),
    val favouriteRoomSaved: Event<Boolean?> = Event.createDefaultState(),
    val sendingMessageState: Event<Boolean?> = Event.createDefaultState(),
    val retryingTimeout: Event<Boolean?> = Event.createDefaultState(),
    var chatBottomSheetState: ChatBottomSheetState = ChatBottomSheetState.Closed
)
