package com.example.xchat2.chat

import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView.OnEditorActionListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.xchat2.MainActivity
import com.example.xchat2.R
import com.example.xchat2.ui.main.MainFragment
import com.example.xchat2.ui.main.repos.ChatRoomContent
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.State
import com.example.xchat2.util.hideKeyboad
import com.example.xchat2.util.showToast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.chat_fragment.*
import kotlinx.android.synthetic.main.layout_bottom_sheet.*
import org.koin.android.viewmodel.ext.android.viewModel

class ChatFragment : Fragment() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var selectedRoom: Chatroom
    private lateinit var bottomController: ChatBottomsheetController
    private val viewModel: ChatViewModel by viewModel()
    private val gridLayoutManager = GridLayoutManager(context, 8)
    private val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    private var userList: List<String> = emptyList()

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
        webview.settings.domStorageEnabled = true
        webview.settings.loadsImagesAutomatically = true
        webview.settings.javaScriptEnabled = true
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
                if (spinner.selectedItemPosition == 0) {
                    viewModel.sendMessage(textinput.text.toString(), selectedRoom.id)
                } else {
                    val message = "/m ${spinner.selectedItem as String} ${textinput.text}"
                    viewModel.sendMessage(message, selectedRoom.id)
                }
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
        if (roomContent.roomHtmlState is State.Loaded) {
            webview.loadDataWithBaseURL(null, roomContent.roomHtmlState.data, "text/html", "ISO-8859-2", null)
            progressBar.visibility = View.GONE
        } else if (roomContent.roomHtmlState is State.Error) {
            showToast(roomContent.roomHtmlState.error.message ?: getString(R.string.unknown_error))
        }

        setUsersAutocomplete(roomContent.roomUsers)

        roomContent.favouriteRoomSaved.getContentIfNotHandled()?.let {
            showToast(getString(R.string.room_saved))
        }
        roomContent.retryingTimeout.getContentIfNotHandled()?.let {
            showToast("Retrying timeout")
        }

        roomContent.roomExitState.getContentIfNotHandled()?.let { success ->
            if (success) {
                (activity as MainActivity).openFragment(MainFragment.newInstance())
            } else {
                showToast(getString(R.string.room_exit_failed))
            }
        }

        bottomController.chatBottomSheetState = roomContent.chatBottomSheetState
        when (roomContent.chatBottomSheetState) {
            is ChatBottomSheetState.SmileScreen -> {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    epoxy_chatroom_bottomsheet.layoutManager = gridLayoutManager
                    bottomController.requestModelBuild()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
            is ChatBottomSheetState.RoomInfo -> {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    epoxy_chatroom_bottomsheet.layoutManager = linearLayoutManager
                    bottomController.requestModelBuild()
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
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

    private fun setUsersAutocomplete(users: List<String>) {
        val adapter = ArrayAdapter(context!!, android.R.layout.simple_dropdown_item_1line, users)
        textinput.threshold = 2
        textinput.setAdapter(adapter)
        textinput.dismissDropDown()

        if(!userList.equals(users)) {
            val spinnerAdapter = ArrayAdapter(context!!, android.R.layout.simple_dropdown_item_1line, listOf("Všem") + users.sorted())
            spinner.adapter = spinnerAdapter
            userList = users
        }
    }
}