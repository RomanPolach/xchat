package com.example.xchat2.chat

import androidx.lifecycle.*
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

class RoomListViewModel(val chatRepository: ChatRepository) : ViewModel() {
    private val _selectedRoom: MutableLiveData<State<SelectedRoomState>> = MutableLiveData(State.Idle)
    val selectedRoom: LiveData<State<SelectedRoomState>> = _selectedRoom

    fun getRoomList(): LiveData<State<List<Chatroom>>> {
        return chatRepository.getRoomList().asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)
    }

    fun onRoomClick(selectedRoom: Chatroom) {
        viewModelScope.launch(context = Dispatchers.IO) {
            chatRepository.isUserLogged().collect { logged ->
                this@RoomListViewModel._selectedRoom.postValue(State.Loaded(SelectedRoomState(selectedRoom, logged)))
            }
        }
    }
}

data class SelectedRoomState(val selectedRoom: Chatroom, val logged: Boolean)