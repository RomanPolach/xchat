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
        viewModelScope.launch(Dispatchers.IO) {
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
                .collect { content ->
                    if (content is State.Loaded) {
                        roomContent.postValue(roomContent.value?.copy(roomHtmlState = State.Loaded(content.data)))
                        loadUsers(chatroom.id)
                    } else if (content is State.Error && content.error is IllegalAccessError) {
                        tryRelogin(chatroom)
                    } else if (content is State.Error) {
                        roomContent.postValue(roomContent.value?.copy(roomHtmlState = State.Error(content.error)))
                    }
                }
        }
    }

    private suspend fun loadUsers(roomId: Int) {
        chatRepository.getRoomUsers(roomId)
            .catch {
            }
            .onEach {
                roomContent.postValue(roomContent.value?.copy(roomUsers = it))
            }.collect()
    }

    private suspend fun tryRelogin(chatroom: Chatroom) {
        chatRepository.tryLoginWithSavedInfo().collect()
        chatRepository.enterChatroom(chatroom).collect()
    }

    fun saveRoomToFavourites(selectedRoom: Chatroom) {
        viewModelScope.launch(context = Dispatchers.IO) {
            chatRepository.saveRoomToFavourites(selectedRoom).collect {
                roomContent.postValue(roomContent.value?.copy(favouriteRoomSaved = Event(true)))
            }
        }
    }

    fun sendMessage(text: String, roomId: Int) {
        viewModelScope.launch(context = Dispatchers.IO) {
            chatRepository.sendMessage(message = text, roomId = roomId)
                .catch {
// Nothing to do here
                }
                .collect {
                    roomContent.postValue(roomContent.value?.copy(sendingMessageState = Event(true)))
                }
        }
    }

    fun getUserList(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.getRoomInfo(id)
                .catch {
// Nothing to do here
                }
                .collect {
                    if (it is State.Loaded) {
                        roomContent.postValue(roomContent.value?.copy(chatBottomSheetState = it.data))
                    }
                }
        }
    }

    fun onSmilesClick() {
        roomContent.postValue(roomContent.value?.copy(chatBottomSheetState = ChatBottomSheetState.SmileScreen()))
    }

    fun onCloseBottomSheetClick() {
        roomContent.postValue(roomContent.value?.copy(chatBottomSheetState = ChatBottomSheetState.Closed))
    }

    fun exitRoom(selectedRoom: Chatroom) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.exitRoom(selectedRoom).collect {
                if (it is State.Error) {
                    roomContent.postValue(roomContent.value?.copy(roomExitState = Event.createEvent(false)))
                } else if (it is State.Loaded) {
                    roomContent.postValue(roomContent.value?.copy(roomExitState = Event.createEvent(true)))
                }
            }
        }
    }
}