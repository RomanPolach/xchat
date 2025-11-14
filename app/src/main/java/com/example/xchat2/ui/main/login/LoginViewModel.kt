package com.example.xchat2.ui.main.login

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

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loginState: State<User?> = State.Idle
)

class LoginViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun login() {
        val currentState = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = currentState.copy(loginState = State.Loading)
            val state = chatRepository.login(currentState.username, currentState.password)
            _uiState.value = currentState.copy(loginState = state)
        }
    }
}
