package com.example.xchat2.ui.main.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class,
        UserFavouriteRoom::class
    ], version = 1
)
abstract class MyDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        fun buildDatabase(context: Context): MyDatabase {
            return Room.databaseBuilder(
                context,
                MyDatabase::class.java,
                "MyDb"
            ).build()
        }
    }
}