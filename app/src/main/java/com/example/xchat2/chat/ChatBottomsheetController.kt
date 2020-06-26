package com.example.xchat2.chat

import android.content.Context
import com.airbnb.epoxy.EpoxyController
import com.example.xchat2.R
import com.example.xchat2.util.header

class ChatBottomsheetController(val context: Context) : EpoxyController() {
    var chatBottomSheetState: ChatBottomSheetState? = null
    var onSmileClick: (Int) -> Unit = {}

    override fun buildModels() {
        if (chatBottomSheetState is ChatBottomSheetState.SmileScreen) {
            (chatBottomSheetState as ChatBottomSheetState.SmileScreen).smileResources.forEach {
                smile {
                    id(it)
                    smileNumber(it)
                    onSmileClick(onSmileClick)
                }
            }
        } else if(chatBottomSheetState is ChatBottomSheetState.RoomInfo) {
            val info = chatBottomSheetState as ChatBottomSheetState.RoomInfo
            header {
                id("admin")
                title("${context.getString(R.string.admin_title)} ${info.admin}")
            }

            header {
                id("idle")
                title("${context.getString(R.string.idle_time_title)} ${info.idleTime}")
            }

            header {
                id("header")
                title(context.getString(R.string.users_in_room))
            }

            info.users.forEach {
                user {
                    id(it.nickname)
                    user(it)
                }
            }
        }
    }
}