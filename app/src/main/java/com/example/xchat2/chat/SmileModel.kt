package com.example.xchat2.chat

import android.widget.ImageView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.example.xchat2.R
import pl.droidsonroids.gif.GifDrawable


@EpoxyModelClass(layout = R.layout.smile_layout)
abstract class SmileModel : EpoxyModelWithHolder<SmileModel.Holder?>() {
    @EpoxyAttribute
    var onSmileClick: (Int) -> Unit = {}

    @EpoxyAttribute
    var smileNumber: Int? = null

    override fun bind(holder: Holder) {
        val gifFromAssets = GifDrawable(holder.imgSmile.context.assets, smileNumber.toString() + ".gif")
        holder.imgSmile.setPadding(20, 20, 20, 20)
        holder.imgSmile.setOnClickListener {
            onSmileClick(smileNumber ?: 0)
        }
        holder.imgSmile.setImageDrawable(gifFromAssets)
    }

    class Holder : KotlinEpoxyHolder() {
        val imgSmile: ImageView by bind(R.id.img_smile)
    }
}