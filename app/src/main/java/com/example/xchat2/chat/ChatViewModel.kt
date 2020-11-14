package com.example.xchat2.chat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.ChatRoomContent
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.Event
import com.example.xchat2.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.util.*

class ChatViewModel(val chatRepository: ChatRepository) : ViewModel() {
    val roomContent = MutableLiveData<ChatRoomContent>(ChatRoomContent(roomHtmlState = State.Idle))

    fun enterRoom(chatroom: Chatroom) {
        chatRepository.enterChatroom(chatroom)
            .retryWhen { cause, attempt ->
                delay(3000)
                cause is UnknownHostException && attempt < 3
            }
            .filter { it is State.Loaded }
            .flatMapLatest {
                chatRepository.subscribeRoomContent(chatroom)
            }
            .catch {
                roomContent.postValue(roomContent.value?.copy(roomHtmlState = State.Error(it)))
            }
            .onEach { content ->
                if (content is State.Loaded) {
                    roomContent.postValue(roomContent.value?.copy(roomHtmlState = State.Loaded(content.data)))
                    loadUsers(chatroom.id)
                } else if (content is State.Error && content.error is IllegalAccessError) {
                    tryRelogin(chatroom)
                } else if (content is State.Error) {
                    roomContent.postValue(roomContent.value?.copy(roomHtmlState = State.Error(content.error)))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadUsers(roomId: Int) {
        chatRepository.getRoomUsers(roomId)
            .catch {
            }
            .onEach {
                roomContent.postValue(roomContent.value?.copy(roomUsers = it))
            }.launchIn(viewModelScope)
    }

    private suspend fun tryRelogin(chatroom: Chatroom) {
        chatRepository.tryLoginWithSavedInfo().collect()
        chatRepository.enterChatroom(chatroom).collect()
    }

    fun saveRoomToFavourites(selectedRoom: Chatroom) {
        chatRepository.saveRoomToFavourites(selectedRoom)
            .onEach {
                roomContent.postValue(roomContent.value?.copy(favouriteRoomSaved = Event(true)))
            }
            .launchIn(viewModelScope)
    }

    fun sendMessage(text: String, roomId: Int) {
        chatRepository.sendMessage(message = text, roomId = roomId)
            .catch {
// Nothing to do here
            }
            .onEach {
                roomContent.postValue(roomContent.value?.copy(sendingMessageState = Event(true)))
            }
            .launchIn(viewModelScope)
    }

    fun getUserList(id: Int) {
        chatRepository.getRoomInfo(id)
            .catch {
// Nothing to do here
            }
            .onEach {
                if (it is State.Loaded) {
                    roomContent.postValue(roomContent.value?.copy(chatBottomSheetState = it.data))
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSmilesClick() {
        roomContent.postValue(roomContent.value?.copy(chatBottomSheetState = ChatBottomSheetState.SmileScreen()))
    }

    fun onCloseBottomSheetClick() {
        roomContent.postValue(roomContent.value?.copy(chatBottomSheetState = ChatBottomSheetState.Closed))
    }

    fun exitRoom(selectedRoom: Chatroom) {
        chatRepository.exitRoom(selectedRoom).onEach {
            if (it is State.Error) {
                roomContent.postValue(roomContent.value?.copy(roomExitState = Event.createEvent(false)))
            } else if (it is State.Loaded) {
                roomContent.postValue(roomContent.value?.copy(roomExitState = Event.createEvent(true)))
            }
        }
            .launchIn(viewModelScope)
    }
}