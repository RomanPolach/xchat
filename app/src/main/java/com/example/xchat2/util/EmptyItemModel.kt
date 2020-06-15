package com.example.xchat2.util

import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.example.xchat2.R
import com.example.xchat2.chat.KotlinEpoxyHolder
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.setVisible

@EpoxyModelClass(layout = R.layout.empty_layout)
abstract class EmptyItemModel : EpoxyModelWithHolder<EmptyItemModel.Holder?>() {
    @EpoxyAttribute
    lateinit var title: String

    override fun bind(holder: Holder) {
        holder.txtTitle.text = title
    }

    class Holder : KotlinEpoxyHolder() {
        val txtTitle: TextView by bind(R.id.txtEmptyTitle)
    }
}