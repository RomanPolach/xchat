package com.example.xchat2.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.db.User
import com.example.xchat2.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MainViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<State<User>>(State.Idle)
    val loginState: StateFlow<State<User>> = _loginState.asStateFlow()

    // Add a flag to track login attempts
    private var isLoggingIn = AtomicBoolean(false)

    init {
        tryLoginWithSavedInfo()
    }

    private fun tryLoginWithSavedInfo() {
        // Cancel any existing login attempts before starting a new one
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.tryLoginWithSavedInfo()
                .distinctUntilChanged()
                .collect { state ->
                    when (state) {
                        is State.Loading -> {
                            if (!isLoggingIn.get()) {
                                isLoggingIn.set(true)
                                _loginState.value = state
                            }
                        }
                        is State.Loaded -> {
                            isLoggingIn.set(false)
                            _loginState.value = state
                        }
                        is State.Error -> {
                            isLoggingIn.set(false)
                            _loginState.value = state
                        }
                        else -> _loginState.value = state
                    }
                }
        }
    }

    // Add a function to manually trigger login attempt
    fun retryLogin() {
        if (!isLoggingIn.get()) {
            _loginState.value = State.Idle // Reset state
            tryLoginWithSavedInfo()
        }
    }

    fun isLoggedIn(): Boolean {
        return _loginState.value is State.Loaded<User>
    }
}
