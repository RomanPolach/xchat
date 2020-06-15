package com.example.xchat2.chat

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.xchat2.R
import com.example.xchat2.ui.main.repos.Chatroom
import kotlinx.android.synthetic.main.chat_fragment.*
import org.koin.android.viewmodel.ext.android.viewModel

class ChatFragment : Fragment() {

    private lateinit var selectedRoom: Chatroom
    private val viewModel: ChatViewModel by viewModel()

    companion object {
        val ARGS = "ARGS"

        fun newInstance(room: Chatroom): ChatFragment {
            val fragment = ChatFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARGS, room)
            fragment.arguments = bundleOf(ARGS to room)
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.chat_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar: Toolbar = view.findViewById(R.id.chatbar)
        toolbar.inflateMenu(R.menu.chatroommenu)
        (arguments?.getParcelable(ARGS) as? Chatroom)?.let {
            selectedRoom = it
        }
        toolbar.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.action_favourite -> {
                    viewModel.saveRoomToFavourites(selectedRoom)
                    true
                }
                else -> false
            }
        }
        webview.getSettings().setDomStorageEnabled(true)
        webview.getSettings().setLoadsImagesAutomatically(true)
        webview.getSettings().setJavaScriptEnabled(true)
        viewModel.enterRoom(selectedRoom!!)
        viewModel.roomContent.observe(viewLifecycleOwner, Observer { roomContent ->
            webview.loadDataWithBaseURL(null, roomContent.roomHtml, "text/html", "ISO-8859-2", null)
            progressBar.visibility = View.GONE

            if(roomContent.favouriteRoomSaved) {
                Toast.makeText(context, getString(R.string.room_saved), Toast.LENGTH_LONG).show()
                viewModel.roomSavedConsumed()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.chatroommenu, menu)
    }

}