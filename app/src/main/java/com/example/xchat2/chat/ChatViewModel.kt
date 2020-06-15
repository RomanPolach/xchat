package com.example.xchat2.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.ChatRoomContent
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ChatViewModel(val chatRepository: ChatRepository) : ViewModel() {
    val roomContent = MutableLiveData<ChatRoomContent>()

    fun enterRoom(chatroom: Chatroom) {
        viewModelScope.launch(viewModelScope.coroutineContext + Dispatchers.IO) {
            chatRepository.enterChatroom(chatroom).collect { roomEnterStatus ->
                if (roomEnterStatus is State.Loaded) {
                    chatRepository.subscribeRoomContent(chatroom).onEach { content ->
                        if (content is State.Loaded) {
                            roomContent.postValue(content.data)
                        }
                    }.collect()
                }
            }
        }
    }

    fun saveRoomToFavourites(selectedRoom: Chatroom) {
        viewModelScope.launch(context = Dispatchers.IO) {
            chatRepository.saveRoomToFavourites(selectedRoom).collect {
                roomContent.postValue(roomContent.value?.copy(favouriteRoomSaved = true))
            }
        }
    }

    fun roomSavedConsumed() {
        roomContent.postValue(roomContent.value?.copy(favouriteRoomSaved = false))
    }
}