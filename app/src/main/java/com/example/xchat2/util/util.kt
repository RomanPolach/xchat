// A small extension function for flows in Compose
// collectWithLifecycle just a pseudocode, you can write your actual collector with LaunchedEffect
package com.example.xchat2.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> Flow<T>.collectWithLifecycle(collector: suspend (T) -> Unit) {
    LaunchedEffect(this) {
        this@collectWithLifecycle.collect { collector(it) }
    }
}
