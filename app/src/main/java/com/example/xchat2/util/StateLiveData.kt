package com.example.xchat2.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

// Observers of [State]

/**
 * Special [MutableLiveData] emitting [State] through dedicated methods.
 */
open class StateLiveData<T>(default: State<T> = State.Idle) : MutableLiveData<State<T>>() {

    init {
        value = default
    }

    fun loaded(data: T) {
        value = State.Loaded(data)
    }

    fun loading() {
        value = State.Loading
    }

    fun error(err: Throwable) {
        value = State.Error(err)
    }

    fun hide(): LiveData<State<T>> = this
}

/**
 * [StateLiveData] that has no value type and therefore is emulated by [Unit] type
 */
class NoValueStateLiveData : StateLiveData<Unit>() {

    fun loaded() {
        loaded(Unit)
    }
}

/**
 * Base state that can be maintained by any view model
 */
sealed class State<out T> {

    object Loading : State<Nothing>()
    object Idle : State<Nothing>()
    data class Error(val error: Throwable) : State<Nothing>()
    data class Loaded<out T>(val data: T) : State<T>()
}