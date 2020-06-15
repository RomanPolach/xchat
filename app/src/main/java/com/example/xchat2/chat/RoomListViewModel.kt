package com.example.xchat2.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.State
import kotlinx.coroutines.Dispatchers

class RoomListViewModel(val chatRepository: ChatRepository) : ViewModel() {

    fun getRoomList() : LiveData<State<List<Chatroom>>> {
        return chatRepository.getRoomList().asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)
    }
}