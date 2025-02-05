// MainActivity.kt
package com.example.xchat2

import FavouriteRoomsViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.example.xchat2.ui.main.MainViewModel
import com.example.xchat2.ui.main.login.LoginViewModel
import com.example.xchat2.chat.ChatViewModel
import com.example.xchat2.chat.RoomListScreen
import com.example.xchat2.chat.RoomListViewModel
import com.example.xchat2.ui.main.MainScreen
import com.example.xchat2.ui.main.favourite.FavouriteRoomsScreen
import com.example.xchat2.ui.main.login.LoginScreen
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by inject()
    private val loginViewModel: LoginViewModel by inject()
    private val roomListViewModel: RoomListViewModel by inject()
    private val chatViewModel: ChatViewModel by inject()
    private val favViewModel: FavouriteRoomsViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            AppTheme {

                NavHost(navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            viewModel = mainViewModel,
                            onLoginClick = { navController.navigate("login") },
                            onRoomListClick = { navController.navigate("roomList") },
                            onFavouritesClick = { navController.navigate("favourites") }
                        )
                    }
                    composable("login/{roomId}?roomName={roomName}",
                        arguments = listOf(
                            navArgument("roomId") { defaultValue = -1; type = NavType.IntType },
                            navArgument("roomName") { defaultValue = ""; type = NavType.StringType }
                        )) {
                        val roomId = it.arguments?.getInt("roomId") ?: -1
                        val roomName = it.arguments?.getString("roomName") ?: ""
                        LoginScreen(
                            loginViewModel,
                            onLoginSuccess = {
                                if (roomId > 0) {
                                    navController.navigate("chat/$roomId/$roomName") {
                                        popUpTo("main") { inclusive = false }
                                    }
                                } else {
                                    navController.popBackStack()
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("login") {
                        LoginScreen(
                            loginViewModel,
                            onLoginSuccess = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("roomList") {
                        RoomListScreen(
                            viewModel = roomListViewModel,
                            onBack = { navController.popBackStack() },
                            onRoomSelected = { room, logged ->
                                if (logged) {
                                    navController.navigate("chat/${room.id}/${room.name}") {
                                        // Pop up to roomList inclusive, so pressing back from chat goes to roomList
                                        popUpTo("roomList") { inclusive = false }
                                        // Prevent multiple copies of the same destination
                                        launchSingleTop = true
                                        // Optional: Restore state when navigating back
                                        restoreState = true
                                    }
                                } else {
                                    navController.navigate("login/${room.id}?roomName=${room.name}")
                                }
                            }
                        )
                    }

                    composable(
                        "chat/{roomId}/{roomName}",
                        arguments = listOf(
                            navArgument("roomId") { type = NavType.IntType },
                            navArgument("roomName") { type = NavType.StringType }
                        )
                    ) {
                        val roomId = it.arguments?.getInt("roomId") ?: -1
                        val roomName = it.arguments?.getString("roomName") ?: ""
                        ChatScreen(
                            viewModel = chatViewModel,
                            roomId = roomId,
                            roomName = roomName,
                            onExit = {
                                // Pop back to roomList instead of navigating to main
                                navController.popBackStack(route = "roomList", inclusive = false)
                            }
                        )
                    }
                    composable("favourites") {
                        FavouriteRoomsScreen(
                            viewModel = favViewModel,
                            onRoomClick = { room ->
                                navController.navigate("chat/${room.id}/${room.name}")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

}
