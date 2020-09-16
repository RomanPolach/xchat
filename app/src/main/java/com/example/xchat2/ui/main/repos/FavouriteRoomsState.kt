package com.example.xchat2.ui.main.repos

import com.example.xchat2.ui.main.db.UserFavouriteRoom

sealed class FavouriteRoomsState {

    class FavouriteRoomsLoaded(val rooms: List<UserFavouriteRoom>) : FavouriteRoomsState()

    object AnonymousUser: FavouriteRoomsState()
}