package com.example.xchat2.ui.main.di

import com.example.xchat2.chat.ChatViewModel
import com.example.xchat2.chat.RoomListViewModel
import com.example.xchat2.ui.main.MainViewModel
import com.example.xchat2.ui.main.favourite.FavouriteRoomsViewModel
import com.example.xchat2.ui.main.login.LoginViewModel
import com.example.xchat2.ui.main.repos.ChatRepository
import com.example.xchat2.ui.main.repos.ChatRepositoryImpl
import com.example.xchat2.ui.main.db.MyDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * App Module for Koin
 */
val appModule = module {

    single<ChatRepository> { ChatRepositoryImpl(get()) }

    viewModel { MainViewModel(get()) }

    viewModel { LoginViewModel(get()) }

    viewModel { ChatViewModel(get())}

    viewModel {RoomListViewModel(get()) }

    viewModel { FavouriteRoomsViewModel(get()) }

    single { MyDatabase.buildDatabase(androidContext()) }

    factory { get<MyDatabase>().userDao() }
}