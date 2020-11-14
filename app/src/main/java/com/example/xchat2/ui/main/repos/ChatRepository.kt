package com.example.xchat2.ui.main.repos

import com.example.xchat2.chat.ChatBottomSheetState
import com.example.xchat2.ui.main.db.User
import com.example.xchat2.ui.main.db.UserDao
import com.example.xchat2.ui.main.db.UserFavouriteRoom
import com.example.xchat2.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.jsoup.Jsoup
import java.net.UnknownHostException
import kotlin.String

/**
 * Repository for chat
 */
interface ChatRepository {
    fun login(name: String, password: String): Flow<State<User>>

    fun tryLoginWithSavedInfo(): Flow<State<User>>

    fun getRoomList(): Flow<State<List<Chatroom>>>

    fun enterChatroom(chatroom: Chatroom): Flow<State<Unit>>

    fun subscribeRoomContent(chatroom: Chatroom): Flow<State<String>>

    fun saveRoomToFavourites(selectedRoom: Chatroom): Flow<Long>

    fun getFavouriteRooms(): Flow<FavouriteRoomsState>

    suspend fun getSendToken(roomId: Int)

    fun sendMessage(message: String, roomId: Int): Flow<State<Unit>>

    fun getRoomInfo(roomId: Int): Flow<State<ChatBottomSheetState.RoomInfo>>

    fun exitRoom(selectedRoom: Chatroom): Flow<State<Unit>>

    fun getRoomUsers(roomId: Int): Flow<List<String>>

    fun isUserLogged(): Flow<Boolean>

    fun searchRooms(search: String): Flow<FavouriteRoomsState>
}

class ChatRepositoryImpl(val userDao: UserDao) : ChatRepository {

    private var sendToken: String = ""

    @Suppress("BlockingMethodInNonBlockingContext")
    override fun login(name: String, password: String): Flow<State<User>> {
        return flow {
            emit(State.Loading)
            val response = createLoginRequest(name, password).execute()

            if (response.isSucessful()) {
                val user = User(name, password, response.getUserHashtag())
                userDao.insertUser(user)
                emit(State.Loaded(user))
            } else {
                emit(State.Error(IllegalAccessError("nejde to, kámo")))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun tryLoginWithSavedInfo(): Flow<State<User>> {
        return flow {
            val user = userDao.getUser()
            if (user != null) {
                login(user.name, user.password).collect { state ->
                    emit(state)
                }
            } else {
                emit(State.Error(IllegalAccessError("Nejde to")))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun isUserLogged(): Flow<Boolean> {
        return flow {
            val user = userDao.getUser()
            emit(user != null)
        }.flowOn(Dispatchers.IO)
    }

    override fun searchRooms(search: String): Flow<FavouriteRoomsState> {

        return flow {
            val user = userDao.getUser()
            (if (user != null) {
                if (search.length < 2) {
                    emit(FavouriteRoomsState.FavouriteRoomsLoaded(userDao.getUserFavouriteRooms(user.id)))
                } else {
                    emit(FavouriteRoomsState.FavouriteRoomsLoaded(userDao.getSearchedFavouriteRooms(search, user.id)))
                }
            } else {
                emit(FavouriteRoomsState.AnonymousUser)
            })
        }.flowOn(Dispatchers.IO)
    }

    override fun getRoomList(): Flow<State<List<Chatroom>>> {
        return flow {
            val page = Jsoup.connect("https://www.xchat.cz/~guest~/index.php").get()
            emit(State.Loaded(page.toRoomList()))
        }.flowOn(Dispatchers.IO)
    }

    override fun enterChatroom(chatroom: Chatroom): Flow<State<Unit>> {
        return flow {
            val user = userDao.getUser()
            val response = createEnterRoomRequest(user!!.token, chatroom.id).execute()
            val code = response.statusCode()
            if (code == 200) {
                emit(State.Loaded(data = Unit))
            } else {
                emit(State.Error(IllegalAccessError("Nejde to")))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun subscribeRoomContent(chatroom: Chatroom): Flow<State<String>> {
        return ticker(10000, 0)
            .consumeAsFlow()
            .mapLatest {
                val user = userDao.getUser()
                try {
                    val response = createGetRoomContentRequest(user!!.token, chatroom.id).execute()
                    if (response != null) {
                        val output = response.getRoomHtmlString()
                        if (output.length < 10) {
                            State.Error(IllegalAccessError("Room content is shit"))
                        } else {
                            State.Loaded(output)
                        }
                    } else {
                        State.Error(IllegalAccessError("Načtení obsahu roomu se nepovedlo"))
                    }
                } catch (e: Exception) {
                    State.Error(UnknownHostException("Unknown host"))
                }
            }
            .flowOn(Dispatchers.IO)
    }


    override fun saveRoomToFavourites(selectedRoom: Chatroom): Flow<Long> {
        return flow {
            val user = userDao.getUser()
            emit(
                userDao.saveRoomToFavourites(
                    UserFavouriteRoom(
                        userId = user!!.id,
                        roomId = selectedRoom.id,
                        roomName = selectedRoom.name
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun getFavouriteRooms(): Flow<FavouriteRoomsState> {
        return flow {
            val user = userDao.getUser()
            if (user != null) {
                emit(FavouriteRoomsState.FavouriteRoomsLoaded(userDao.getUserFavouriteRooms(user.id)))
            } else {
                emit(FavouriteRoomsState.AnonymousUser)
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getSendToken(roomId: Int) {
        val user = userDao.getUser()
        user?.token?.let {
            val response = createGetSendTokenRequest(roomId, it).get().toString()
            sendToken = Regex("wtkn\" value=\"(.*?)[\"]").find(response)?.groupValues?.get(1) ?: ""
        }
    }

    override fun sendMessage(message: String, roomId: Int): Flow<State<Unit>> {
        return flow {
            val user = userDao.getUser()
            getSendToken(roomId)
            if (user != null) {
                val response = createSendMessageRequest(
                    message = message,
                    roomId = roomId,
                    token = user.token,
                    sendToken = sendToken
                ).execute()
                if (response.statusCode() == 200) {
                    emit(State.Loaded(Unit))
                } else {
                    emit(State.Error(IllegalAccessError("Odeslání zprávy selhalo")))
                }
            } else {
                emit(State.Error(AnonymousUserException()))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getRoomInfo(roomId: Int): Flow<State<ChatBottomSheetState.RoomInfo>> {
        return flow {
            val user = userDao.getUser()
            user?.let {
                val userpage = createGetUserListRequest(roomId = roomId, token = user.token).get()
                val roomInfoPage = createGetRoomInfoRequest(roomId = roomId, token = user.token).get()
                val pageString = roomInfoPage.toString()
                val admin = Regex("strong id=\"admin\">(.*?)</strong>").find(pageString)?.groupValues?.get(1)?.trim() ?: ""
                val idle = Regex("strong id=\"idle\">(.*?)</strong>").find(pageString)?.groupValues?.get(1)?.trim() ?: ""
                val roomInfo = ChatBottomSheetState.RoomInfo(users = userpage.getUserList(), admin = admin, idleTime = idle)
                emit(State.Loaded(roomInfo))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun exitRoom(selectedRoom: Chatroom): Flow<State<Unit>> {
        return flow {
            val user = userDao.getUser()
            val exitRequest = createRoomExitRequest(user!!.token, selectedRoom.id)
            val response = exitRequest.execute()
            if (response.statusCode() == 200) {
                emit(State.Loaded(Unit))
            } else {
                emit(State.Error(IllegalAccessError("Opuštění místnosti selhalo")))
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getRoomUsers(roomId: Int): Flow<List<String>> {
        return flow {
            val user = userDao.getUser()
            user?.let {
                val userpage = createGetUserListRequest(roomId = roomId, token = user.token).get().getUserList().map {
                    it.nickname
                }
                emit(userpage)
            }
        }.flowOn(Dispatchers.IO)
    }
}
