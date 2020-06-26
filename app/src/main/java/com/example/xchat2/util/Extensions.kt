package com.example.xchat2.util

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment


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

fun Fragment.hideKeyboad() {
    val imm = context!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view!!.windowToken, 0)
}