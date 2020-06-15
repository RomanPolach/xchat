package com.example.xchat2.ui.main.repos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(val name: String, val password: String, val token: String, @PrimaryKey val id: Int = 1)
