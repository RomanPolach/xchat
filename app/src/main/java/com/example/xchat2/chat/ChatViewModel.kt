package com.example.xchat2.chat

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.ChatRoomContent
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.Event
import com.example.xchat2.util.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownHostException

const val ALL_USERS = "Všem"

data class ChatUiState(
    val roomContent: ChatRoomContent = ChatRoomContent(roomHtmlState = State.Idle),
    val isRefreshing: Boolean = false,
    val message: String = "",
    val currentChatroom: Chatroom? = null,
    val lastHtmlState: String = "",
    val showSuggestions: Boolean = false,
    val filteredUsers: List<String> = emptyList(),
    val userDropdownExpanded: Boolean = false,
    val shouldShowToast: Event<String>? = null,
    val shouldNavigateExit: Event<Boolean>? = null
)

class ChatViewModel(val chatRepository: ChatRepository) : ViewModel(), DefaultLifecycleObserver {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentRoomJob: Job? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        cleanupRoom()
    }

    override fun onStart(owner: LifecycleOwner) {
        _uiState.value.currentChatroom?.let { chatroom ->
            enterRoom(chatroom, restartPolling = true)
        }
    }

    fun initializeRoom(roomId: Int, roomName: String) {
        cleanupRoom()
        enterRoom(Chatroom(roomId, roomName))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun enterRoom(chatroom: Chatroom, restartPolling: Boolean = true) {
        if (_uiState.value.currentChatroom == chatroom && currentRoomJob?.isActive == true && !restartPolling) return

        cleanupJobs()
        _uiState.update {
            it.copy(
                currentChatroom = chatroom,
                lastHtmlState = "",
                isRefreshing = false,
                roomContent = it.roomContent.copy(roomHtmlState = State.Loading)
            )
        }

        currentRoomJob = viewModelScope.launch {
            var retryAttempt = 0
            var enterResult: State<Unit>? = null

            while (retryAttempt < 3 && enterResult !is State.Loaded) {
                enterResult = chatRepository.enterChatroom(chatroom)
                when (enterResult) {
                    is State.Loaded -> break
                    is State.Error -> {
                        if (enterResult.error is UnknownHostException && retryAttempt < 2) {
                            retryAttempt++
                            kotlinx.coroutines.delay(1000)
                        } else {
                            _uiState.update {
                                it.copy(
                                    roomContent = it.roomContent.copy(roomHtmlState = State.Error(enterResult.error)),
                                    isRefreshing = false
                                )
                            }
                            return@launch
                        }
                    }
                    else -> break
                }
            }

            if (enterResult !is State.Loaded) {
                return@launch
            }

            chatRepository.subscribeRoomContent(chatroom)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            roomContent = it.roomContent.copy(roomHtmlState = State.Error(error)),
                            isRefreshing = false
                        )
                    }
                }
                .collect { state ->
                    _uiState.update {
                        it.copy(
                            roomContent = it.roomContent.copy(roomHtmlState = state),
                            lastHtmlState = if (state is State.Loaded) state.data else it.lastHtmlState,
                            isRefreshing = false
                        )
                    }
                    if (state is State.Loaded) {
                        loadUsers(chatroom.id)
                    }
                }
        }
    }


    fun refreshRoomContent() {
        val currentChatroom = _uiState.value.currentChatroom ?: return
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val state = chatRepository.fetchRoomContentOnce(currentChatroom)
            _uiState.update {
                it.copy(
                    roomContent = it.roomContent.copy(roomHtmlState = state),
                    lastHtmlState = if (state is State.Loaded) state.data else it.lastHtmlState,
                    isRefreshing = false
                )
            }
            if (state is State.Loaded) {
                loadUsers(currentChatroom.id)
            }
        }
    }

    private fun cleanupJobs() {
        currentRoomJob?.cancel()
        currentRoomJob = null
    }

    fun cleanupRoom() {
        cleanupJobs()
        _uiState.value = ChatUiState()
    }

    private fun loadUsers(roomId: Int) {
        viewModelScope.launch {
            val users = chatRepository.getRoomUsers(roomId)
            _uiState.update {
                it.copy(roomContent = it.roomContent.copy(roomUsers = users))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanupRoom()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    fun saveRoomToFavourites(selectedRoom: Chatroom) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            chatRepository.saveRoomToFavourites(selectedRoom)
            _uiState.update {
                it.copy(
                    roomContent = it.roomContent.copy(favouriteRoomSaved = Event(true)),
                    shouldShowToast = Event("Room saved to favourites"),
                    isRefreshing = false
                )
            }
        }
    }

    fun onMessageChange(message: String) {
        val showSuggestions = message.isNotEmpty() && message.length in 3..8
        val filteredUsers = if (showSuggestions) {
            _uiState.value.roomContent.roomUsers.filter { it.startsWith(message, ignoreCase = true) }
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(
                message = message,
                showSuggestions = filteredUsers.isNotEmpty(),
                filteredUsers = filteredUsers
            )
        }
    }

    fun onSelectedUserChange(user: String) {
        _uiState.update {
            it.copy(
                roomContent = it.roomContent.copy(selectedUser = user),
                userDropdownExpanded = false
            )
        }
    }

    fun onUserDropdownExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(userDropdownExpanded = expanded) }
    }

    fun onSuggestionClick(user: String) {
        _uiState.update {
            it.copy(
                message = "$user: ",
                showSuggestions = false,
                filteredUsers = emptyList()
            )
        }
    }

    fun onSmileClick(smile: Int) {
        _uiState.update { it.copy(message = it.message + " *$smile* ") }
    }

    fun sendMessage(roomId: Int) {
        val currentState = _uiState.value
        val baseMessage = currentState.message
        if (baseMessage.isBlank()) return

        val user = currentState.roomContent.selectedUser
        val finalMessage = if (user != ALL_USERS) {
            "/m $user $baseMessage"
        } else {
            baseMessage
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val result = chatRepository.sendMessage(finalMessage, roomId)
            when (result) {
                is State.Loaded -> {
                    _uiState.update { it.copy(message = "", isRefreshing = false) }
                }
                is State.Error -> {
                    _uiState.update {
                        it.copy(
                            shouldShowToast = Event("Failed to send message: ${result.error.message}"),
                            isRefreshing = false
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }

    fun getUserList(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val state = chatRepository.getRoomInfo(id)
            when (state) {
                is State.Loaded -> {
                    _uiState.update { currentState ->
                        currentState.copy(
                            roomContent = currentState.roomContent.copy(chatBottomSheetState = state.data),
                            isRefreshing = false
                        )
                    }
                }
                is State.Error -> {
                    _uiState.update {
                        it.copy(
                            shouldShowToast = Event("Failed to load room info: ${state.error.message}"),
                            isRefreshing = false
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }

    fun onSmilesClick() {
        _uiState.update { it.copy(roomContent = it.roomContent.copy(chatBottomSheetState = ChatBottomSheetState.SmileScreen())) }
    }

    fun onCloseBottomSheetClick() {
        _uiState.update { it.copy(roomContent = it.roomContent.copy(chatBottomSheetState = ChatBottomSheetState.Closed)) }
    }

    fun onToastShown() {
        _uiState.update { it.copy(shouldShowToast = null) }
    }

    fun onExitHandled() {
        _uiState.update { it.copy(shouldNavigateExit = null) }
    }

    fun exitRoom(selectedRoom: Chatroom) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val result = chatRepository.exitRoom(selectedRoom)
            when (result) {
                is State.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            roomContent = state.roomContent.copy(roomExitState = Event.createEvent(false)),
                            shouldShowToast = Event("Failed to exit room"),
                            isRefreshing = false
                        )
                    }
                }
                is State.Loaded -> {
                    _uiState.update { state ->
                        state.copy(
                            shouldNavigateExit = Event(true),
                            isRefreshing = false
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }
}