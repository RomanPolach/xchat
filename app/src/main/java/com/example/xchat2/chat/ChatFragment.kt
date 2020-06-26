package com.example.xchat2.chat

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.xchat2.R
import com.example.xchat2.ui.main.repos.AnonymousUserException
import com.example.xchat2.ui.main.repos.ChatRoomContent
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.State
import com.example.xchat2.util.hideKeyboad
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.chat_fragment.*
import kotlinx.android.synthetic.main.layout_bottom_sheet.*
import org.koin.android.viewmodel.ext.android.viewModel

class ChatFragment : Fragment() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var selectedRoom: Chatroom
    private lateinit var bottomController: ChatBottomsheetController
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
        chatbar.inflateMenu(R.menu.chatroommenu)

        setBottomSheet()
        setListeners()
        setWebView()
        loadRoom()
    }

    private fun loadRoom() {
        (arguments?.getParcelable(ARGS) as? Chatroom)?.let {
            selectedRoom = it
        }
        viewModel.enterRoom(selectedRoom)
        chatbar.title = selectedRoom.name
        viewModel.roomContent.observe(viewLifecycleOwner, Observer { roomContent ->
            handleRoomState(roomContent)
        })
    }

    private fun setBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(layoutBottomSheet)
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    viewModel.onCloseBottomSheetClick()
                }
            }

        })
        bottomController = ChatBottomsheetController(context!!)
        bottomController.onSmileClick = {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            textinput.setText("${textinput.text} *$it*")
        }
        epoxy_chatroom_bottomsheet.setController(bottomController)
    }

    private fun setWebView() {
        webview.getSettings().setDomStorageEnabled(true)
        webview.getSettings().setLoadsImagesAutomatically(true)
        webview.getSettings().setJavaScriptEnabled(true)
    }

    private fun setListeners() {
        chatbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_favourite -> {
                    viewModel.saveRoomToFavourites(selectedRoom)
                    true
                }
                R.id.action_info -> {
                    if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                        viewModel.getUserList(selectedRoom.id)
                    } else {
                        viewModel.onCloseBottomSheetClick()
                    }
                    true
                }
                R.id.action_smile -> {
                    if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                        viewModel.onSmilesClick()
                    } else {
                        viewModel.onCloseBottomSheetClick()
                    }
                    true
                }
                R.id.action_exit -> {
                    viewModel.exitRoom(selectedRoom)
                    true
                }
                else -> false
            }
        }

        textinput.setOnEditorActionListener(OnEditorActionListener { textView, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_SEND) {
                viewModel.sendMessage(textinput.text.toString(), selectedRoom.id)
                return@OnEditorActionListener true
            }
            false
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.chatroommenu, menu)
    }

    private fun handleRoomState(roomContent: ChatRoomContent) {
        webview.loadDataWithBaseURL(null, roomContent.roomHtml, "text/html", "ISO-8859-2", null)
        progressBar.visibility = View.GONE

        roomContent.favouriteRoomSaved.getContentIfNotHandled()?.let {
            showToast(getString(R.string.room_saved))
        }
        roomContent.retryingTimeout.getContentIfNotHandled()?.let {
            showToast("Retrying timeout")
        }

        roomContent.roomExitState.getContentIfNotHandled()?.let { success ->
            if (success) {
                activity?.onBackPressed()
            } else {
                showToast(getString(R.string.room_exit_failed))
            }
        }

        bottomController.chatBottomSheetState = roomContent.chatBottomSheetState

        when (roomContent.chatBottomSheetState) {
            is ChatBottomSheetState.SmileScreen -> {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                }
                epoxy_chatroom_bottomsheet.setLayoutManager(GridLayoutManager(context, 8))
                bottomController.requestModelBuild()
            }
            is ChatBottomSheetState.RoomInfo -> {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                }
                epoxy_chatroom_bottomsheet.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                bottomController.requestModelBuild()
            }
            is ChatBottomSheetState.Closed -> {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }

        when (roomContent.sendingMessageState.getContentIfNotHandled()) {
            true -> {
                textinput.setText("")
                hideKeyboad()
            }
            false -> showToast("Sending message failed")
        }
    }

    private fun handleError(error: Throwable) {
        when (error) {
            is IllegalAccessError -> showToast(error.message ?: "Illegal Access Error")
            is AnonymousUserException -> showToast(getString(R.string.anonymous_user_error))
        }
    }

    fun showToast(message: kotlin.String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}