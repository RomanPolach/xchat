// MainScreen.kt
package com.example.xchat2.ui.main

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.asFlow
import com.example.xchat2.R
import com.example.xchat2.util.State
import com.example.xchat2.ui.main.db.User

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onLoginClick: () -> Unit,
    onRoomListClick: () -> Unit,
    onFavouritesClick: () -> Unit
) {
    // Move the state observation out of recomposable code
    val state by viewModel.loginState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.retryLogin()
    }

    val statusText = when (state) {
        is State.Loaded<User> -> "Přihlášen:\n${(state as State.Loaded<User>).data.name}"
        is State.Error -> "Stav:\nNepřihlášen"
        else -> "Stav:\n Nepřihlášen"
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xD2188EFE), Color(0xD29BD4FF))
                    )
                )
                .padding(30.dp)
        ) {
            Text(
                text = statusText,
                modifier = Modifier
                    .background(color = Color(0xD2E1E5E5), shape = RoundedCornerShape(8.dp))
                    .padding(10.dp),
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(50.dp))
            Button(
                onClick = { onLoginClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Přihlášení")
            }
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = {
                    if (viewModel.isLoggedIn()) {
                        onRoomListClick()
                    } else {
                        Toast.makeText(context, "Pro přístup se musíte přihlásit", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Výpis místností")
            }
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = {
                    if (viewModel.isLoggedIn()) {
                        onFavouritesClick()
                    } else {
                        Toast.makeText(context, "Pro přístup se musíte přihlásit", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text("Oblíbené místnosti")
            }
        }
    }
}
