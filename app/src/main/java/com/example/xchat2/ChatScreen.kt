package com.example.xchat2

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.xchat2.chat.ChatBottomSheetState
import com.example.xchat2.chat.ChatUser
import com.example.xchat2.chat.ChatViewModel
import com.example.xchat2.chat.Sex
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.State

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    roomId: Int,
    roomName: String,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                    return true
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    view?.loadDataWithBaseURL(
                        null,
                        "<html><body><h3>Chyba při načítání obsahu. Zkuste obnovit.</h3></body></html>",
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
            settings.javaScriptEnabled = true
            setBackgroundColor(Color(0xFFE3F2FD).toArgb())
        }
    }


    val uiState by viewModel.uiState.collectAsState()
    val roomContent = uiState.roomContent
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentRoom = remember(roomId, roomName) { Chatroom(roomId, roomName) }

    DisposableEffect(roomId, roomName) {
        viewModel.initializeRoom(roomId, roomName)
        onDispose {
            viewModel.cleanupRoom()
        }
    }

    // Refresh content when screen resumes, especially if in error state
    val isRefreshNeeded = remember {
        derivedStateOf {
            val state = uiState.roomContent.roomHtmlState
            state is State.Error || state is State.Idle
        }
    }

    DisposableEffect(lifecycleOwner, roomId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isRefreshNeeded.value) {
                viewModel.refreshRoomContent()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val sheetState = rememberModalBottomSheetState()
    val showSheet = roomContent.chatBottomSheetState !is ChatBottomSheetState.Closed

    SideEffect {
        uiState.shouldShowToast?.getContentIfNotHandled()?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.onToastShown()
        }
    }

    SideEffect {
        uiState.shouldNavigateExit?.getContentIfNotHandled()?.let {
            onExit()
            viewModel.onExitHandled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    navigationIconContentColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { onExit() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(text = roomName) },
                actions = {
                    IconButton(onClick = { viewModel.refreshRoomContent() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                    IconButton(onClick = {
                        viewModel.saveRoomToFavourites(currentRoom)
                    }) {
                        Icon(Icons.Default.Favorite, contentDescription = "Fav", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.getUserList(roomId) }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.onSmilesClick() }) {
                        Icon(Icons.Default.Face, contentDescription = "Smiles", tint = Color.White)
                    }
                    IconButton(onClick = {
                        viewModel.exitRoom(currentRoom)
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Exit", tint = Color.White)
                    }
                }
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                AndroidView(
                    factory = {
                        webView
                    },
                    update = { webView ->
                        when (val state = roomContent.roomHtmlState) {
                            is State.Loaded -> {
                                val html = state.data
                                webView.loadDataWithBaseURL(
                                    null,
                                    html,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }

                            is State.Loading -> {
                                webView.loadDataWithBaseURL(
                                    null,
                                    "<html><body><h3>Načítám...</h3></body></html>",
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }

                            else -> {
                                // Handle Error or Empty states
                                webView.loadDataWithBaseURL(
                                    null,
                                    "<html><body><h3>Chyba při načítání obsahu. Zkuste obnovit.</h3></body></html>",
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Color.LightGray)
                        .padding(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = uiState.userDropdownExpanded,
                        onExpandedChange = { viewModel.onUserDropdownExpandedChange(it) },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        OutlinedTextField(
                            value = roomContent.selectedUser,
                            onValueChange = { viewModel.onSelectedUserChange(it) },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.userDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = uiState.userDropdownExpanded,
                            onDismissRequest = { viewModel.onUserDropdownExpandedChange(false) }
                        ) {
                            val userList = listOf(com.example.xchat2.chat.ALL_USERS) + roomContent.roomUsers
                            userList.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(user) },
                                    onClick = {
                                        viewModel.onSelectedUserChange(user)
                                        viewModel.onUserDropdownExpandedChange(false)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black),
                        value = uiState.messageTextFieldValue,
                        singleLine = true,
                        onValueChange = { viewModel.onMessageChange(it) },
                        placeholder = { Text("Poslat zprávu") },
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxHeight(),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                viewModel.sendMessage(roomId)
                            }
                        )
                    )
                }
            }
            if (uiState.showSuggestions) {
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 70.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .background(Color.White)
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        uiState.filteredUsers.forEach { user ->
                            Text(
                                textAlign = TextAlign.Center,
                                color = Color.Black,
                                text = user,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .clickable {
                                        viewModel.onSuggestionClick(user)
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onCloseBottomSheetClick() },
            sheetState = sheetState
        ) {
            when (val sheetData = roomContent.chatBottomSheetState) {
                is ChatBottomSheetState.RoomInfo -> {
                    RoomInfoContent(
                        admin = sheetData.admin,
                        idleTime = sheetData.idleTime,
                        users = sheetData.users
                    )
                }

                is ChatBottomSheetState.SmileScreen -> {
                    SmileScreenContent(
                        smileRange = sheetData.smileResources,
                        onSmileClick = { smileId ->
                            viewModel.onSmileClick(smileId)
                            viewModel.onCloseBottomSheetClick()
                        }
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun SmileScreenContent(smileRange: IntRange, onSmileClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp) // Constrain height to improve performance
    ) {
        items(
            count = smileRange.count(),
            key = { idx -> smileRange.first + idx } // Stable keys for better recomposition
        ) { idx ->
            val smileId = smileRange.first + idx
            val imageName = "$smileId.gif"
            AsyncImage(
                model = "file:///android_asset/$imageName",
                contentDescription = "Smile #$smileId",
                modifier = Modifier
                    .size(30.dp)
                    .clickable {
                        onSmileClick(smileId)
                    },
                placeholder = painterResource(R.drawable.ic_loading_placeholder),
                error = painterResource(R.drawable.ic_error),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun RoomInfoContent(admin: String, idleTime: String, users: List<ChatUser>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = "Správce: $admin", fontWeight = FontWeight.Bold)
        Text(text = "Doba nečinnosti: $idleTime", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Uživatelé v místnosti:", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        users.forEach { user ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "User sex icon",
                    modifier = Modifier.size(18.dp),
                    tint = when (user.sex) {
                        Sex.MUZ -> Color.Blue
                        Sex.ZENA -> Color.Red
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = user.nickname)
            }
        }
    }
}