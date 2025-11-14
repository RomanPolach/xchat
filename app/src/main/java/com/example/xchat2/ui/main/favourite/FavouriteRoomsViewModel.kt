package com.example.xchat2.ui.main.favourite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xchat2.ui.main.db.UserFavouriteRoom
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.ui.main.repos.FavouriteRoomsState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavouriteRoomsUiState(
    val favouriteRoomsState: FavouriteRoomsState = FavouriteRoomsState.AnonymousUser,
    val filterRoomsState: FavouriteRoomsState = FavouriteRoomsState.AnonymousUser,
    val searchQuery: String = ""
)

class FavouriteRoomsViewModel(private val chatRepository: ChatRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FavouriteRoomsUiState())
    val uiState: StateFlow<FavouriteRoomsUiState> = _uiState.asStateFlow()

    init {
        loadFavouriteRooms()
        setupSearchFlow()
    }

    private fun loadFavouriteRooms() {
        viewModelScope.launch {
            val favouriteRoomsState = chatRepository.getFavouriteRooms()
            _uiState.update { it.copy(favouriteRoomsState = favouriteRoomsState) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun setupSearchFlow() {
        _uiState
            .map { it.searchQuery }
            .distinctUntilChanged()
            .debounce(300)
            .onEach { search ->
                viewModelScope.launch {
                    val filterRoomsState = chatRepository.searchRooms(search)
                    _uiState.update { it.copy(filterRoomsState = filterRoomsState) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun setSearchQuery(search: String) {
        _uiState.update { it.copy(searchQuery = search) }
    }
}

fun List<UserFavouriteRoom>.toChatRoomList() = map { Chatroom(it.roomId, it.roomName) }
