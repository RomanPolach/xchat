package com.example.xchat2.chat

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.example.xchat2.MainActivity
import com.example.xchat2.R
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.State
import kotlinx.android.synthetic.main.room_list_fragment.*
import org.koin.android.viewmodel.ext.android.viewModel

class RoomListFragment : Fragment() {

    companion object {
        fun newInstance() = RoomListFragment()
    }

    private val viewModel: RoomListViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.room_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        epoxyRecyclerView.itemAnimator = null
        viewModel.getRoomList().observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Loaded -> showRooms(it.data)
            }
        })
        toolbar_roomlist.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    fun showRooms(rooms: List<Chatroom>) {
        epoxyRecyclerView.withModels {
            rooms.forEach {
                roomItem {
                    id(it.id)
                    onClick { selectedRoom ->
                        (activity as MainActivity).replaceFragment(
                            ChatFragment.newInstance(selectedRoom)
                        )
                    }
                    room(it)
                }
            }
        }
    }
}