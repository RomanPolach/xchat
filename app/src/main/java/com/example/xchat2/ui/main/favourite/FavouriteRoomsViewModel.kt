package com.example.xchat2.ui.main.favourite

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.ui.main.repos.FavouriteRoomsState
import com.example.xchat2.ui.main.db.UserFavouriteRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

class FavouriteRoomsViewModel(private val chatRepository: ChatRepository) : ViewModel() {
    private val searchChanel = ConflatedBroadcastChannel<String>()

    val filterRoomsLiveData = searchChanel.asFlow() //asFlow() converts received elements from broadcast channels into a flow.
        .flatMapLatest { search ->
            chatRepository.searchRooms(search)
        }
        .distinctUntilChanged()
        .catch { throwable ->
            Log.d("FRANTA", "NEJDE")
        }.asLiveData()

    fun setSearchQuery(search: String) {
        searchChanel.offer(search)
    }

    fun getFavouriteRooms(): LiveData<FavouriteRoomsState> {
        return chatRepository.getFavouriteRooms().asLiveData(Dispatchers.IO)
    }
}

fun List<UserFavouriteRoom>.toChatRoomList() = map { Chatroom(it.roomId, it.roomName) }
