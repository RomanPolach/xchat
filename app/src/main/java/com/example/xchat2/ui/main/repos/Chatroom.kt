package com.example.xchat2.ui.main.repos

import android.os.Parcel
import android.os.Parcelable

import kotlin.String

data class Chatroom(val id: Int, val name: String, val roomUsers: String? = null): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(roomUsers)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Chatroom> {
        override fun createFromParcel(parcel: Parcel): Chatroom {
            return Chatroom(parcel)
        }

        override fun newArray(size: Int): Array<Chatroom?> {
            return arrayOfNulls(size)
        }
    }
}
