package com.example.xchat2.chat

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.ChatRoomContent
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.Event
import com.example.xchat2.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.util.*

class ChatViewModel(val chatRepository: ChatRepository) : ViewModel(), LifecycleObserver {
    private val _roomContent = MutableStateFlow(ChatRoomContent(roomHtmlState = State.Idle))
    val roomContent: StateFlow<ChatRoomContent> = _roomContent

    // Add this to track the subscription
    private var currentRoomJob: Job? = null

    private var currentChatroom: Chatroom? = null
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message


    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        cleanupRoom() // Clean up when the app goes to the background
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        currentChatroom?.let { chatroom ->
            enterRoom(chatroom) // Re-enter the chat room when the app comes back to the foreground
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun enterRoom(chatroom: Chatroom) {
        // Cancel any existing subscription
        currentRoomJob?.cancel()

        currentChatroom = chatroom

        // Emit loading state first
        _roomContent.update { roomContent -> roomContent.copy(roomHtmlState = State.Loading) }

        // Start new subscription
        currentRoomJob = chatRepository.enterChatroom(chatroom)
            .retryWhen { cause, attempt ->
                delay(3000)
                cause is UnknownHostException && attempt < 3
            }
            .filter { it is State.Loaded }
            .flatMapLatest {
                chatRepository.subscribeRoomContent(chatroom)
            }
            .distinctUntilChanged()
            .catch {
                _roomContent.update { roomContent -> roomContent.copy(roomHtmlState = State.Error(it)) }
            }
            .onEach { content ->
                if (content is State.Loaded) {
                    _roomContent.update { roomContent -> roomContent.copy(roomHtmlState = State.Loaded(content.data)) }
                    loadUsers(chatroom.id)
                } else if (content is State.Error && content.error is IllegalAccessError) {
                    tryRelogin(chatroom)
                } else if (content is State.Error) {
                    _roomContent.update { roomContent -> roomContent.copy(roomHtmlState = State.Error(content.error)) }
                }
            }
            .launchIn(viewModelScope)
    }

    // Add cleanup function
    fun cleanupRoom() {
        currentRoomJob?.cancel()
        currentRoomJob = null
        _roomContent.update { ChatRoomContent(roomHtmlState = State.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        currentRoomJob?.cancel()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    private fun loadUsers(roomId: Int) {
        chatRepository.getRoomUsers(roomId)
            .catch {
            }
            .onEach {
                _roomContent.update { roomContent -> roomContent.copy(roomUsers = it) }
            }.launchIn(viewModelScope)
    }


    private suspend fun tryRelogin(chatroom: Chatroom) {
        chatRepository.tryLoginWithSavedInfo().collect()
        chatRepository.enterChatroom(chatroom).collect()
    }

    fun saveRoomToFavourites(selectedRoom: Chatroom) {
        chatRepository.saveRoomToFavourites(selectedRoom)
            .onEach {
                _roomContent.update { roomContent -> roomContent.copy(favouriteRoomSaved = Event(true)) }
            }
            .launchIn(viewModelScope)
    }

    fun onMessageChange(message: String) {
       _message.value = message
    }

    fun onSelectedUserChange(user: String) {
        _roomContent.update { roomContent -> roomContent.copy(selectedUser = user) }
    }

    fun onSmileClick(smile: Int) {
        _message.value += (" *$smile* ")
    }

    fun sendMessage(roomId: Int) {
        val currentContent = roomContent.value ?: return
        val baseMessage = _message.value
        val user = currentContent.selectedUser

        // Prepend "/m username" if a user is selected instead of "all users"
        val finalMessage = if (user != "Všem") {
            "/m $user $baseMessage"
        } else {
            baseMessage
        }

        chatRepository.sendMessage(finalMessage, roomId)
            .catch {
                // Nothing to do here
            }
            .onEach {
                // Clear message text after sending
               _message.value = ""
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
                    _roomContent.update { roomContent -> roomContent.copy(chatBottomSheetState = it.data) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSmilesClick() {
       _roomContent.update { roomContent -> roomContent.copy(chatBottomSheetState = ChatBottomSheetState.SmileScreen()) }
    }

    fun onCloseBottomSheetClick() {
        _roomContent.update { roomContent -> roomContent.copy(chatBottomSheetState = ChatBottomSheetState.Closed) }
    }

    fun exitRoom(selectedRoom: Chatroom) {
        chatRepository.exitRoom(selectedRoom)
            .onEach {
                if (it is State.Error) {
                    _roomContent.update { roomContent ->
                        roomContent.copy(roomExitState = Event.createEvent(false))
                    }
                } else if (it is State.Loaded) {
                    cleanupRoom() // Add this line
                    _roomContent.update { roomContent ->
                        roomContent.copy(roomExitState = Event.createEvent(true))
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}