package com.example.xchat2.ui.main.repos

sealed class FavouriteRoomsState {

    class FavouriteRoomsLoaded(val rooms: List<UserFavouriteRoom>) : FavouriteRoomsState()

    object AnonymousUser: FavouriteRoomsState()
}