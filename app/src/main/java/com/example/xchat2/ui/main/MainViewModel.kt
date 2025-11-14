package com.example.xchat2.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xchat2.ui.main.db.User
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MainViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<State<User>>(State.Idle)
    val loginState: StateFlow<State<User>> = _loginState.asStateFlow()

    private var isLoggingIn = AtomicBoolean(false)

    init {
        tryLoginWithSavedInfo()
    }

    private fun tryLoginWithSavedInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isLoggingIn.get()) return@launch
            isLoggingIn.set(true)
            _loginState.value = State.Loading

            val state = chatRepository.tryLoginWithSavedInfo()
            isLoggingIn.set(false)
            _loginState.value = state
        }
    }

    fun retryLogin() {
        if (!isLoggingIn.get()) {
            _loginState.value = State.Idle
            tryLoginWithSavedInfo()
        }
    }

    fun isLoggedIn(): Boolean {
        return _loginState.value is State.Loaded<User>
    }
}
