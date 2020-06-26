package com.example.xchat2.util

open class Event<out T>(private val executed: T) {

    companion object {
        fun createDefaultState() = Event(null)
        fun createEvent(success: Boolean) = Event(success)
    }

    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            executed
        }
    }

    fun peekContent(): T = executed
}