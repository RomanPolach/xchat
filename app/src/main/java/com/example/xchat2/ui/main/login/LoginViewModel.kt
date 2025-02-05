package com.example.xchat2.ui.main.login

import androidx.lifecycle.*
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.db.User
import com.example.xchat2.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val _loginState = MutableLiveData<State<User?>>()
    val loginState: LiveData<State<User?>> = _loginState

    fun login(name: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = chatRepository.login(name, password)
            _loginState.postValue(state)
        }
    }
}
