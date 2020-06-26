package com.example.xchat2.chat

data class ChatUser(val nickname: String, val sex: Sex)

enum class Sex {
    MUZ,
    ZENA
}
