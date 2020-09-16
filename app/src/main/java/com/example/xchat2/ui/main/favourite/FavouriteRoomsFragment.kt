package com.example.xchat2.ui.main.favourite

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.example.xchat2.MainActivity
import com.example.xchat2.R
import com.example.xchat2.chat.ChatFragment
import com.example.xchat2.chat.roomItem
import com.example.xchat2.ui.main.repos.FavouriteRoomsState
import com.example.xchat2.util.emptyItem
import com.example.xchat2.util.header
import kotlinx.android.synthetic.main.favourite_rooms_fragment.*
import org.koin.android.viewmodel.ext.android.viewModel

class FavouriteRoomsFragment : Fragment() {

    companion object {
        fun newInstance() = FavouriteRoomsFragment()
    }

    private val viewModel: FavouriteRoomsViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.favourite_rooms_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getFavouriteRooms().observe(viewLifecycleOwner, Observer { roomState ->
            showRooms(roomState)
        })
        viewModel.filterRoomsLiveData.observe(viewLifecycleOwner, Observer { roomState ->
            showRooms(roomState)
        })
        edittext_filter.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(filter: Editable?) {
                filter?.let {
                        viewModel.setSearchQuery(it.toString())
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

        })
        toolbar_favourite.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun showRooms(roomsState: FavouriteRoomsState) {
        epoxy_favourite_rooms.withModels {
            when (roomsState) {
                is FavouriteRoomsState.FavouriteRoomsLoaded -> {
                    val rooms = roomsState.rooms.toChatRoomList()

                    if (rooms.isEmpty()) {
                        emptyItem {
                            id("empty")
                            title(getString(R.string.empty_favourite_title))
                        }
                    } else {
                        header {
                            id("header")
                            title("Počet oblíbených místností: ${rooms.size}")
                        }

                        rooms.forEach {
                            roomItem {
                                onClick { selectedRoom ->
                                    (activity as MainActivity).openFragment(ChatFragment.newInstance(selectedRoom))
                                }
                                id(it.id)
                                room(it)
                            }
                        }
                    }
                }
                is FavouriteRoomsState.AnonymousUser -> {
                    emptyItem {
                        id("anonymous")
                        title(getString(R.string.favourite_rooms_anonymous_title))
                    }
                }
            }
        }
    }
}
