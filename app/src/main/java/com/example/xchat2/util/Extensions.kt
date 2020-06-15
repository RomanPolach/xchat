package com.example.xchat2.util

import android.view.View
import android.widget.TextView

fun View.setVisible(visible: Boolean) {
    if (visible) {
        visibility = View.VISIBLE
    } else {
        visibility = View.GONE
    }
}

fun TextView.setTextOrHide(text: String?) {
    if (text == null) {
        visibility = View.GONE
    } else {
        visibility = View.VISIBLE
        this.text = text
    }
}