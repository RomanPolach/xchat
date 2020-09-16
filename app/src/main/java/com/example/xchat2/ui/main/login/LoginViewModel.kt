package com.example.xchat2.ui.main.login

import androidx.lifecycle.*
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.db.User
import com.example.xchat2.util.State
import kotlinx.coroutines.Dispatchers

class LoginViewModel(val chatRepository: ChatRepository) : ViewModel() {

    fun login(name: String, password: String): LiveData<State<User>> {
        return chatRepository.login(name, password).asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)
    }
}
