package com.example.xchat2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xchat2.ui.main.favourite.FavouriteRoomsViewModel
import com.example.xchat2.ui.main.favourite.toChatRoomList
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.ui.main.repos.FavouriteRoomsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouriteRoomsScreen(
    viewModel: FavouriteRoomsViewModel,
    onRoomClick: (Chatroom) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Oblíbené místnosti") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xD2188EFE), Color(0xD29BD4FF))
                )
            )
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Hledej") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            val roomsState = if (uiState.searchQuery.isEmpty()) {
                uiState.favouriteRoomsState
            } else {
                uiState.filterRoomsState
            }

            when (roomsState) {
                is FavouriteRoomsState.FavouriteRoomsLoaded -> {
                    val rooms = roomsState.rooms.toChatRoomList()
                    if (rooms.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Nenalezeny žádné oblíbené místnosti")
                        }
                    } else {
                        LazyColumn {
                            item {
                                Text(
                                    "Počet oblíbených místností: ${rooms.size}",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            items(rooms) { room ->
                                RoomItem(room = room, onRoomClick = onRoomClick)
                            }
                        }
                    }
                }
                is FavouriteRoomsState.AnonymousUser -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Pro zobrazení oblíbených místností se musíš přihlásit")
                    }
                }
            }
        }
    }
}

@Composable
fun RoomItem(room: Chatroom, onRoomClick: (Chatroom) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onRoomClick(room) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text(
                text = room.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
