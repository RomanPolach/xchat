package com.example.xchat2.chat

import androidx.lifecycle.*
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.ChatRoomContent
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.Event
import com.example.xchat2.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ChatViewModel(val chatRepository: ChatRepository) : ViewModel() {
    val roomContent = MutableLiveData<ChatRoomContent>()

    init {
        roomContent.postValue(ChatRoomContent(roomHtml = ""))
    }

    fun enterRoom(chatroom: Chatroom) {
        viewModelScope.launch(viewModelScope.coroutineContext + Dispatchers.IO) {
            chatRepository.enterChatroom(chatroom)
                .retryWhen { cause, attempt ->
                    delay(3000)
                    cause is UnknownHostException && attempt < 3
                }
                .map { roomEnterStatus ->
                    if (roomEnterStatus is State.Loaded) {
                        chatRepository.subscribeRoomContent(chatroom)
                            .retryWhen { cause, attempt ->
                                roomContent.postValue(roomContent.value?.copy(retryingTimeout = Event(cause is SocketTimeoutException)))
                                attempt < 3
                            }
                            .onEach { content ->
                                if (content is State.Loaded) {
                                    roomContent.postValue(roomContent.value?.copy(roomHtml = content.data))
                                } else if (content is State.Error && content.error is IllegalAccessError) {
                                    tryRelogin(chatroom)
                                }
                            }
                            .collect()
                    }
                }
                .collect()
        }
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

    fun sendMessage(text: kotlin.String, roomId: Int) {
        viewModelScope.launch(context = Dispatchers.Default) {
            chatRepository.sendMessage(message = text, roomId = roomId).collect {
                roomContent.postValue(roomContent.value?.copy(sendingMessageState = Event(true)))
            }
        }
    }

    fun getUserList(id: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            chatRepository.getRoomInfo(id).collect {
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
        viewModelScope.launch(Dispatchers.Default) {
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