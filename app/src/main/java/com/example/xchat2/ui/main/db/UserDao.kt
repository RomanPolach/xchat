package com.example.xchat2.ui.main.db

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

    @Query("SELECT * FROM user")
    abstract fun getUserFlow(): Flow<User?>

    @Query("SELECT * FROM favourite_rooms WHERE roomName LIKE '%' || :search || '%' AND userId == :userId")
    abstract fun getSearchedFavouriteRooms(search: String, userId: Int): List<UserFavouriteRoom>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveRoomToFavourites(room: UserFavouriteRoom): Long

    @Query("SELECT * FROM favourite_rooms WHERE roomName LIKE '%' || :search || '%' AND userId == :userId")
    abstract fun getSearchedRooms(search: String, userId: Int): Flow<List<UserFavouriteRoom>>

    @Query("SELECT * FROM favourite_rooms WHERE userId == :id")
    abstract fun getUserFavouriteRooms(id: Int): List<UserFavouriteRoom>
}