package com.example.xchat2.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.db.User
import com.example.xchat2.util.State
import kotlinx.coroutines.Dispatchers

class MainViewModel(val chatRepository: ChatRepository) : ViewModel() {

    fun tryLoginWithSavedInfo(): LiveData<State<User>> {
        return chatRepository.tryLoginWithSavedInfo().asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)
    }
}
