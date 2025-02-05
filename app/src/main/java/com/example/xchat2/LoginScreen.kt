package com.example.xchat2.ui.main.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.xchat2.util.State

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginState by viewModel.loginState.observeAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Přihlášení", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    navigationIconContentColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xD2188EFE), Color(0xD29BD4FF))
            )
        )) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Zadej Přezdívku") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Heslo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                val isLoading = loginState is State.Loading
                Button(
                    onClick = { viewModel.login(username, password) },
                    enabled = !isLoading && username.isNotEmpty() && password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Text("Přihlásit se")
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                if (loginState is State.Loaded) {
                    LaunchedEffect(Unit) {
                        onLoginSuccess()
                    }
                }

                if (loginState is State.Error) {
                    Text(
                        text = "Chyba: ${(loginState as State.Error).error.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
