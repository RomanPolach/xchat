package com.example.xchat2.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoomListUiState(
    val roomsState: State<List<Chatroom>> = State.Idle,
    val selectedRoomState: State<SelectedRoomState> = State.Idle
)

data class SelectedRoomState(val selectedRoom: Chatroom? = null, val logged: Boolean)

class RoomListViewModel(val chatRepository: ChatRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RoomListUiState())
    val uiState: StateFlow<RoomListUiState> = _uiState.asStateFlow()

    init {
        loadRoomList()
    }

    private fun loadRoomList() {
        viewModelScope.launch {
            val roomsState = chatRepository.getRoomList()
            _uiState.value = _uiState.value.copy(roomsState = roomsState)
        }
    }

    fun onRoomClick(selectedRoom: Chatroom) {
        viewModelScope.launch(Dispatchers.IO) {
            val logged = chatRepository.isUserLogged()
            _uiState.value = _uiState.value.copy(
                selectedRoomState = State.Loaded(SelectedRoomState(selectedRoom, logged))
            )
        }
    }

    fun resetSelectedRoom() {
        _uiState.value = _uiState.value.copy(selectedRoomState = State.Idle)
    }
}