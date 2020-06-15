package com.example.xchat2.chat

import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.example.xchat2.R
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.setVisible

@EpoxyModelClass(layout = R.layout.room_item_layout)
abstract class RoomItemModel : EpoxyModelWithHolder<RoomItemModel.Holder?>() {
    @EpoxyAttribute
    lateinit var room: Chatroom

    @EpoxyAttribute
    lateinit var onClick: (room: Chatroom) -> Unit

    override fun bind(holder: Holder) {
        holder.txtRoomName.text = room.name
        holder.layoutRoom.setOnClickListener {
            onClick(room)
        }

        holder.txtRoomCount.setVisible(room.roomUsers != null)

        room.roomUsers?.let {
            holder.txtRoomCount.text =
                "${it} ${holder.txtRoomCount.context.getString(R.string.people_in_room)}"
        }
    }

    class Holder : KotlinEpoxyHolder() {
        val txtRoomName: TextView by bind(R.id.txt_room)
        val txtRoomCount: TextView by bind(R.id.txt_room_count)
        val layoutRoom: ConstraintLayout by bind(R.id.layout_room)
    }
}