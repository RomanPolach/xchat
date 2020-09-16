package com.example.xchat2.ui.main.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.String

@Entity(tableName = "favourite_rooms")
data class UserFavouriteRoom(val userId: Int, @PrimaryKey val roomId: Int, val roomName: String)
