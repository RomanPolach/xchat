package com.example.xchat2.ui.main.favourite

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.ui.main.repos.FavouriteRoomsState
import com.example.xchat2.ui.main.repos.UserFavouriteRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map

class FavouriteRoomsViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    fun getFavouriteRooms(): LiveData<FavouriteRoomsState> {
        return chatRepository.getFavouriteRooms().asLiveData(Dispatchers.IO)
    }
}

fun List<UserFavouriteRoom>.toChatRoomList() = map { Chatroom(it.roomId, it.roomName) }
