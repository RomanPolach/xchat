package com.example.xchat2.ui.main.repos

import android.util.Log
import com.example.xchat2.chat.ChatBottomSheetState
import com.example.xchat2.ui.main.db.User
import com.example.xchat2.ui.main.db.UserDao
import com.example.xchat2.ui.main.db.UserFavouriteRoom
import com.example.xchat2.util.State
import com.example.xchat2.util.createEnterRoomRequest
import com.example.xchat2.util.createGetRoomContentRequest
import com.example.xchat2.util.createGetRoomInfoRequest
import com.example.xchat2.util.createGetSendTokenRequest
import com.example.xchat2.util.createGetUserListRequest
import com.example.xchat2.util.createLoginRequest
import com.example.xchat2.util.createRoomExitRequest
import com.example.xchat2.util.createSendMessageRequest
import com.example.xchat2.util.getGuestIndexDocument
import com.example.xchat2.util.getRoomHtmlString
import com.example.xchat2.util.getUserHashtag
import com.example.xchat2.util.getUserList
import com.example.xchat2.util.isSuccessful
import com.example.xchat2.util.parseDocument
import com.example.xchat2.util.toRoomList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Repository for chat
 */
interface ChatRepository {
    suspend fun login(name: String, password: String): State<User>
    suspend fun tryLoginWithSavedInfo(): State<User>

    suspend fun getRoomList(): State<List<Chatroom>>

    suspend fun enterChatroom(chatroom: Chatroom): State<Unit>

    fun subscribeRoomContent(chatroom: Chatroom): Flow<State<String>>

    suspend fun fetchRoomContentOnce(chatroom: Chatroom): State<String>

    suspend fun saveRoomToFavourites(selectedRoom: Chatroom): Long

    suspend fun getFavouriteRooms(): FavouriteRoomsState

    suspend fun getSendToken(roomId: Int)

    suspend fun sendMessage(message: String, roomId: Int): State<Unit>

    suspend fun getRoomInfo(roomId: Int): State<ChatBottomSheetState.RoomInfo>

    suspend fun exitRoom(selectedRoom: Chatroom): State<Unit>

    suspend fun getRoomUsers(roomId: Int): List<String>

    suspend fun isUserLogged(): Boolean

    suspend fun searchRooms(search: String): FavouriteRoomsState
}

class ChatRepositoryImpl(val userDao: UserDao) : ChatRepository {

    private var sendToken: String = ""

    override suspend fun login(name: String, password: String): State<User> {
        val response = createLoginRequest(name, password)
        if (response.isSuccessful()) {
            val user = User(name, password, response.getUserHashtag())
            userDao.insertUser(user)
            return State.Loaded(user)
        } else {
            return State.Error(IllegalAccessError("Přihlášeni selhalo"))
        }
    }

    override suspend fun tryLoginWithSavedInfo(): State<User> {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUser()
            if (user != null) {
                try {
                    login(user.name, user.password)
                } catch (e: Exception) {
                    State.Error(e)
                }
            } else {
                State.Error(IllegalAccessError("Not logged in"))
            }
        }
    }

    override suspend fun isUserLogged(): Boolean {
        return withContext(Dispatchers.IO) {
            userDao.getUser() != null
        }
    }

    override suspend fun searchRooms(search: String): FavouriteRoomsState {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUser()
            if (user != null) {
                if (search.length < 2) {
                    FavouriteRoomsState.FavouriteRoomsLoaded(userDao.getUserFavouriteRooms(user.id))
                } else {
                    FavouriteRoomsState.FavouriteRoomsLoaded(userDao.getSearchedFavouriteRooms(search, user.id))
                }
            } else {
                FavouriteRoomsState.AnonymousUser
            }
        }
    }

    override suspend fun getRoomList(): State<List<Chatroom>> {
        return withContext(Dispatchers.IO) {
            var lastException: IOException? = null
            repeat(3) { attempt ->
                try {
                    val doc = withTimeoutOrNull(10000) { getGuestIndexDocument() }
                        ?: throw SocketTimeoutException("Timeout fetching room list")
                    return@withContext State.Loaded(doc.toRoomList())
                } catch (e: IOException) {
                    lastException = e
                    if (attempt < 2) {
                        delay(2000)
                    }
                } catch (e: Exception) {
                    return@withContext State.Error(e)
                }
            }
            State.Error(lastException ?: IOException("Failed to fetch room list after 3 attempts"))
        }
    }

    override suspend fun enterChatroom(chatroom: Chatroom): State<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = userDao.getUser()
                val response = createEnterRoomRequest(user!!.token, chatroom.id)
                if (response.statusCode() == 200) {
                    State.Loaded(Unit)
                } else {
                    State.Error(IllegalAccessError("Nejde to"))
                }
            } catch (e: Exception) {
                State.Error(e)
            }
        }
    }

    override fun subscribeRoomContent(chatroom: Chatroom): Flow<State<String>> {
        return flow {
            emit(Unit)
            while (true) {
                delay(3000)
                emit(Unit)
            }
        }
            .mapLatest {
                val user = userDao.getUser()
                if (user == null) {
                    return@mapLatest State.Error(AnonymousUserException())
                }
                try {
                    // Consider adding timeouts to the OkHttpClient instance used here
                    val response = createGetRoomContentRequest(user.token, chatroom.id)
                    val output = response.getRoomHtmlString()
                    // Basic validation - maybe needs improvement
                    if (output.length < 10) {
                        State.Error(IllegalStateException("Invalid room content received"))
                    } else {
                        State.Loaded(output)
                    }
                } catch (e: SocketTimeoutException) {
                    Log.w("ChatRepository", "Timeout fetching room content for ${chatroom.id}", e)
                    State.Error(e)
                } catch (e: UnknownHostException) {
                    Log.w("ChatRepository", "Unknown host fetching room content for ${chatroom.id}", e)
                    State.Error(e)
                } catch (e: IOException) {
                    Log.w("ChatRepository", "IOException fetching room content for ${chatroom.id}", e)
                    State.Error(e)
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Unexpected error fetching room content for ${chatroom.id}", e)
                    State.Error(e)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun fetchRoomContentOnce(chatroom: Chatroom): State<String> {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUser()
            if (user == null) {
                return@withContext State.Error(AnonymousUserException())
            }
            try {
                val response = createGetRoomContentRequest(user.token, chatroom.id)
                val output = response.getRoomHtmlString()
                if (output.length < 10) {
                    State.Error(IllegalStateException("Invalid room content received"))
                } else {
                    State.Loaded(output)
                }
            } catch (e: SocketTimeoutException) {
                Log.w("ChatRepository", "Timeout fetching room content (once) for ${chatroom.id}", e)
                State.Error(e)
            } catch (e: UnknownHostException) {
                Log.w("ChatRepository", "Unknown host fetching room content (once) for ${chatroom.id}", e)
                State.Error(e)
            } catch (e: IOException) {
                Log.w("ChatRepository", "IOException fetching room content (once) for ${chatroom.id}", e)
                State.Error(e)
            } catch (e: Exception) {
                Log.e("ChatRepository", "Unexpected error fetching room content (once) for ${chatroom.id}", e)
                State.Error(RuntimeException("Error during fetch: ${e.message}", e))
            }
        }
    }

    override suspend fun saveRoomToFavourites(selectedRoom: Chatroom): Long {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUser()
            userDao.saveRoomToFavourites(
                UserFavouriteRoom(
                    userId = user!!.id,
                    roomId = selectedRoom.id,
                    roomName = selectedRoom.name
                )
            )
        }
    }

    override suspend fun getFavouriteRooms(): FavouriteRoomsState {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUser()
            if (user != null) {
                FavouriteRoomsState.FavouriteRoomsLoaded(userDao.getUserFavouriteRooms(user.id))
            } else {
                FavouriteRoomsState.AnonymousUser
            }
        }
    }

    override suspend fun getSendToken(roomId: Int) {
        val user = userDao.getUser()
        user?.token?.let { token ->
            val response = createGetSendTokenRequest(roomId, token)
            val pageString = response.parseDocument().toString()
            sendToken = Regex("wtkn\" value=\"(.*?)[\"]").find(pageString)?.groupValues?.get(1) ?: ""
        }
    }

    override suspend fun sendMessage(message: String, roomId: Int): State<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = userDao.getUser()
                if (user == null) {
                    return@withContext State.Error(AnonymousUserException())
                }
                getSendToken(roomId)
                val response = createSendMessageRequest(
                    message = message,
                    roomId = roomId,
                    token = user.token,
                    sendToken = sendToken
                )
                if (response.statusCode() == 200) {
                    State.Loaded(Unit)
                } else {
                    State.Error(IllegalAccessError("Odeslání zprávy selhalo"))
                }
            } catch (e: Exception) {
                State.Error(e)
            }
        }
    }

    override suspend fun getRoomInfo(roomId: Int): State<ChatBottomSheetState.RoomInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val user = userDao.getUser()
                if (user == null) {
                    return@withContext State.Error(AnonymousUserException())
                }
                val userResponse = createGetUserListRequest(roomId = roomId, token = user.token)
                val userpage: Document = userResponse.parse()
                val infoResponse = createGetRoomInfoRequest(roomId = roomId, token = user.token)
                val roomInfoPage: Document = infoResponse.parse()
                val pageString = roomInfoPage.html()
                val admin = Regex("strong id=\"admin\">(.*?)</strong>").find(pageString)?.groupValues?.get(1)?.trim() ?: ""
                val idle = Regex("strong id=\"idle\">(.*?)</strong>").find(pageString)?.groupValues?.get(1)?.trim() ?: ""
                val users = userpage.getUserList()
                val roomInfo = ChatBottomSheetState.RoomInfo(users = users, admin = admin, idleTime = idle)
                State.Loaded(roomInfo)
            } catch (e: Exception) {
                State.Error(e)
            }
        }
    }

    override suspend fun exitRoom(selectedRoom: Chatroom): State<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = userDao.getUser()
                val response = createRoomExitRequest(user!!.token, selectedRoom.id)
                if (response.statusCode() == 200) {
                    State.Loaded(Unit)
                } else {
                    State.Error(IllegalAccessError("Opuštění místnosti selhalo"))
                }
            } catch (e: Exception) {
                State.Error(e)
            }
        }
    }

    override suspend fun getRoomUsers(roomId: Int): List<String> {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUser() // Early return

            try {
                val response = createGetUserListRequest(roomId = roomId, token = user!!.token)
                val doc = response.parseDocument()
                doc.getUserList().map { it.nickname }
            } catch (e: Exception) {
                Log.w("ChatRepository", "Failed to get room users for $roomId", e)
                emptyList()
            }
        }
    }
}
