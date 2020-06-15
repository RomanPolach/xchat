package com.example.xchat2.ui.main.repos

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Chatroom(val id: Int, val name: String, val roomUsers: String? = null): Parcelable
