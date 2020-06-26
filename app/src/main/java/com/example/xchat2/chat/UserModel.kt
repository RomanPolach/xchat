package com.example.xchat2.chat

import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.example.xchat2.R
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.ui.main.repos.User
import com.example.xchat2.util.setVisible

@EpoxyModelClass(layout = R.layout.user_layout)
abstract class UserModel : EpoxyModelWithHolder<UserModel.Holder?>() {
    @EpoxyAttribute
    lateinit var user: ChatUser

    override fun bind(holder: Holder) {
        holder.txtUserName.text = user.nickname
        val icon = if(user.sex == Sex.MUZ) holder.txtUserName.context.getDrawable(R.drawable.ic_man) else holder.txtUserName.context.getDrawable(R.drawable.ic_woman)
        holder.txtUserName.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
    }

    class Holder : KotlinEpoxyHolder() {
        val txtUserName: TextView by bind(R.id.txtUserName)
    }
}