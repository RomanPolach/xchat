import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.xchat2.ui.main.db.UserFavouriteRoom
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.ui.main.repos.FavouriteRoomsState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

class FavouriteRoomsViewModel(private val chatRepository: ChatRepository) : ViewModel() {
    private val searchQueryFlow = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val filterRoomsLiveData = searchQueryFlow
        .debounce(300) // Add a small delay to avoid querying too often
        .flatMapLatest { search ->
            chatRepository.searchRooms(search)
        }
        .distinctUntilChanged()
        .catch { throwable ->
            Log.d("FRANTA", "NEJDE: ${throwable.message}")
        }
        .asLiveData()

    fun setSearchQuery(search: String) {
        searchQueryFlow.value = search
    }

    fun getFavouriteRooms(): LiveData<FavouriteRoomsState> {
        return chatRepository.getFavouriteRooms().asLiveData(Dispatchers.IO)
    }
}

fun List<UserFavouriteRoom>.toChatRoomList() = map { Chatroom(it.roomId, it.roomName) }
