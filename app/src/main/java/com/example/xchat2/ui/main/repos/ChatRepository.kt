package com.example.xchat2.ui.main.repos

import android.view.View
import com.example.xchat2.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import org.jsoup.Jsoup

/**
 * Repository for chat
 */
interface ChatRepository {
    fun login(name: String, password: String): Flow<State<User>>

    fun tryLoginWithSavedInfo(): Flow<State<User>>

    fun getRoomList(): Flow<State<List<Chatroom>>>

    fun enterChatroom(chatroom: Chatroom): Flow<State<Unit>>

    fun subscribeRoomContent(chatroom: Chatroom): Flow<State<ChatRoomContent>>

    fun saveRoomToFavourites(selectedRoom: Chatroom): Flow<Long>

    fun getFavouriteRooms(): Flow<FavouriteRoomsState>
}

class ChatRepositoryImpl(val userDao: UserDao) : ChatRepository {

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
        }
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
        }
    }

    override fun getRoomList(): Flow<State<List<Chatroom>>> {
        return flow {
            val page = Jsoup.connect("https://www.xchat.cz/~guest~/index.php").get()
            emit(State.Loaded(page.toRoomList()))
        }
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
        }
    }

    override fun subscribeRoomContent(chatroom: Chatroom): Flow<State<ChatRoomContent>> {
        val user = userDao.getUser()

        return flow {
            do {
                val response = createGetRoomContentRequest(user!!.token, chatroom.id).execute()
                if (response != null) {
                    val output = response.getRoomHtmlString()
                    if (output.length < 10) emit(State.Error(IllegalAccessError("Room content is shit")))
                    else if (output.length > 10) emit(State.Loaded(ChatRoomContent(output)))
                } else {
                    emit(State.Error(IllegalAccessError("Načtení obsahu roomu se nepovedlo")))
                }
                delay(10000)
            } while (true)
        }
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
        }
    }

    override fun getFavouriteRooms(): Flow<FavouriteRoomsState> {
        return flow {
            val user = userDao.getUser()
            if (user != null) {
                emit(FavouriteRoomsState.FavouriteRoomsLoaded(userDao.getUserFavouriteRooms(user.id)))
            } else {
                emit(FavouriteRoomsState.AnonymousUser)
            }
        }
    }
}