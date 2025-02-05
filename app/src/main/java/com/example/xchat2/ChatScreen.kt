package com.example.xchat2

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.asFlow
import com.example.xchat2.chat.ChatBottomSheetState
import com.example.xchat2.chat.ChatUser
import com.example.xchat2.chat.ChatViewModel
import com.example.xchat2.chat.Sex
import com.example.xchat2.ui.main.repos.ChatRoomContent
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.Event
import com.example.xchat2.util.State
import kotlinx.coroutines.launch

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
            }
            settings.javaScriptEnabled = true
            setBackgroundColor(Color(0xFFE3F2FD).toArgb())
        }
    }

    val roomContent by viewModel.roomContent.collectAsState(
        ChatRoomContent(
            roomHtmlState = State.Idle,
            roomUsers = emptyList(),
            favouriteRoomSaved = Event.createDefaultState(),
            retryingTimeout = Event.createDefaultState(),
            roomExitState = Event.createDefaultState(),
            chatBottomSheetState = ChatBottomSheetState.Closed
        )
    )

    val messageState = viewModel.message.collectAsState()

    DisposableEffect(roomId) {
        onDispose {
            viewModel.cleanupRoom()
        }
    }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = messageState.value)) }
    var lastHtmlState = ""

    LaunchedEffect(messageState.value) {
        if (textFieldValue.text != messageState.value) {
            textFieldValue = textFieldValue.copy(
                text = messageState.value,
                selection = TextRange(messageState.value.length) // Set cursor to the end
            )
        }
    }

    if (roomContent.favouriteRoomSaved.getContentIfNotHandled() == true) {
        Toast.makeText(LocalContext.current, "Room saved to favourites", Toast.LENGTH_LONG).show()
    }

    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    LaunchedEffect(roomId, roomName) {
        viewModel.cleanupRoom() // Clean up any existing room
        viewModel.enterRoom(Chatroom(roomId, roomName))
    }

    // Show or hide bottom sheet based on chatBottomSheetState
    LaunchedEffect(roomContent.chatBottomSheetState) {
        if (roomContent.chatBottomSheetState == ChatBottomSheetState.Closed) {
            bottomSheetState.hide()
        } else {
            bottomSheetState.expand()
        }
    }


    var showSuggestions by remember { mutableStateOf(false) }
    var filteredUsers by remember { mutableStateOf(emptyList<String>()) }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
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
                    IconButton(onClick = {
                        viewModel.saveRoomToFavourites(Chatroom(roomId, roomName))
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
                        viewModel.exitRoom(Chatroom(roomId, roomName))
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Exit", tint = Color.White)
                    }
                }
            )
        },
        content = { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()

                ) {

                    AndroidView(
                        factory = {
                            webView
                        },
                        update = { webView ->
                            when (roomContent.roomHtmlState) {
                                is State.Loaded -> {
                                    val loadedHtml = (roomContent.roomHtmlState as State.Loaded).data
                                    if (loadedHtml != lastHtmlState) {
                                        webView.loadDataWithBaseURL(
                                            null,
                                            loadedHtml,
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                        lastHtmlState = loadedHtml
                                    }
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
                                    // Handle other states (e.g., State.Error) if needed
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
                        // 1) ExposedDropdownMenuBox for selecting a user or "all users"
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            val textFieldColors = OutlinedTextFieldDefaults.colors(
                                // customize colors if needed
                            )

                            BasicTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                value = roomContent.selectedUser,
                                onValueChange = { viewModel.onSelectedUserChange(it) },
                                singleLine = true,
                                readOnly = true,
                                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center),
                                decorationBox = { innerTextField ->
                                    OutlinedTextFieldDefaults.DecorationBox(
                                        value = roomContent.selectedUser,
                                        innerTextField = innerTextField,
                                        enabled = true,
                                        singleLine = true,
                                        visualTransformation = VisualTransformation.None,
                                        interactionSource = remember { MutableInteractionSource() },
                                        trailingIcon = {
                                            IconButton(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable), // Exact size of the icon,
                                                onClick = { expanded = !expanded },
                                            )
                                            {
                                                Icon(
                                                    imageVector = Icons.Filled.ArrowDropDown,
                                                    contentDescription = "Dropdown",
                                                    modifier = Modifier.fillMaxSize() // Let the icon fill the Box
                                                )
                                            }
                                        },
                                        leadingIcon = null,
                                        supportingText = null,
                                        isError = false,
                                        colors = textFieldColors,
                                        // ↓ Here, you directly control the content padding
                                        contentPadding = PaddingValues(start = 4.dp, end = 0.dp, top = 4.dp, bottom = 4.dp)
                                    )
                                },

                                )

                            // 2) The dropdown list: "all users" + each user in roomContent.roomUsers
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                val userList = listOf("Všem") + roomContent.roomUsers
                                userList.forEach { user ->
                                    DropdownMenuItem(
                                        text = { Text(user) },
                                        onClick = {
                                            viewModel.onSelectedUserChange(user)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            colors = TextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black),
                            value = textFieldValue,
                            singleLine = true,
                            onValueChange = { value ->
                                textFieldValue = value // Update the local state
                                val newText = value.text
                                if (newText.isNotEmpty() && newText.length in 3..8) {
                                    filteredUsers = roomContent.roomUsers.filter { it.startsWith(newText, ignoreCase = true) }
                                    showSuggestions = filteredUsers.isNotEmpty()
                                } else {
                                    showSuggestions = false
                                }
                                viewModel.onMessageChange(newText) // Update the ViewModel
                            },
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
                if (showSuggestions) {
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
                                .background(
                                    Color.White
                                )
                                .padding(vertical = 10.dp), horizontalAlignment = Alignment.Start
                        ) {
                            filteredUsers.forEach { user ->
                                Text(
                                    textAlign = TextAlign.Center,
                                    color = Color.Black,
                                    text = user,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .clickable {
                                            viewModel.onMessageChange("$user: ")
                                            showSuggestions = false
                                        }
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        sheetPeekHeight = 0.dp,
        sheetContent = {
            when (val sheetData = roomContent.chatBottomSheetState) {
                is ChatBottomSheetState.RoomInfo -> {
                    RoomInfoContent(
                        admin = sheetData.admin,
                        idleTime = sheetData.idleTime,
                        users = sheetData.users
                    )
                }

                is ChatBottomSheetState.SmileScreen -> {
                    SmileScreenContent(sheetData.smileResources, onSmileClick = { smileId ->
                        viewModel.onSmileClick(smileId)
                        viewModel.onCloseBottomSheetClick()
                    })
                }

                else -> {}
            }
        }
    )


    // Navigate away if exit event triggered
    val exitEvent = roomContent.roomExitState.getContentIfNotHandled()
    if (exitEvent == true) {
        onExit()
    }
}

@Composable
fun SmileScreenContent(smileRange: IntRange, onSmileClick: (Int) -> Unit) {
    val context = LocalContext.current
    LazyVerticalGrid(columns = GridCells.Fixed(8)) {
        items(smileRange.count()) { idx ->
            val imageName = "${smileRange.first + idx}.gif"
            val imageBitmap = remember(imageName) {
                try {
                    val inputStream = context.assets.open(imageName) // Open the image from assets
                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                } catch (e: Exception) {
                    null // Handle missing or invalid images
                }
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Smile #${smileRange.first + idx}",
                    modifier = Modifier
                        .size(30.dp)
                        .padding(5.dp)
                        .clickable {
                            onSmileClick(idx + 1)
                        } // Adjust size as needed
                )
            } else {
                // Show a placeholder or error if the image is missing
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = "X",
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun RoomInfoContent(admin: String, idleTime: String, users: List<ChatUser>) {
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
