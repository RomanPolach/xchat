package com.example.xchat2.ui.main.repos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUser(user: User)

    @Query("SELECT * FROM user")
    abstract fun getUser(): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveRoomToFavourites(room: UserFavouriteRoom): Long

    @Query("SELECT * FROM favourite_rooms WHERE userId == :id")
    abstract fun getUserFavouriteRooms(id: Int): List<UserFavouriteRoom>
}