// MainScreen.kt
package com.example.xchat2.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xchat2.ui.main.db.User
import com.example.xchat2.util.State

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onLoginClick: () -> Unit,
    onRoomListClick: () -> Unit,
    onFavouritesClick: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(viewModel)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel)
        }
    }
    
    // Move the state observation out of recomposable code
    val state by viewModel.loginState.collectAsState()
    val context = LocalContext.current

    val statusText = when (state) {
        is State.Loaded -> "Přihlášen:\n${(state as State.Loaded<User>).data.name}"
        is State.Error -> "Stav:\nNepřihlášen"
        else -> "Stav:\n Nepřihlášen"
    }

    val isLoggedIn = state is State.Loaded

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
                    if (isLoggedIn) {
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
                    if (isLoggedIn) {
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
