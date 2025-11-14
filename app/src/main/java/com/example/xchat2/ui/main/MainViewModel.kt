package com.example.xchat2.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xchat2.ui.main.db.User
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.util.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<State<User>>(State.Idle)
    val loginState: StateFlow<State<User>> = _loginState.asStateFlow()

    private var isLoggingIn = false

    init {
        tryLoginWithSavedInfo()
    }

    private fun tryLoginWithSavedInfo() {
        viewModelScope.launch {
            if (isLoggingIn) return@launch
            isLoggingIn = true
            _loginState.value = State.Loading

            val state = chatRepository.tryLoginWithSavedInfo()
            isLoggingIn = false
            _loginState.value = state
        }
    }
}
