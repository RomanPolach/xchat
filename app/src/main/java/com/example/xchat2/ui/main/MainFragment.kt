package com.example.xchat2.ui.main

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.example.xchat2.MainActivity
import com.example.xchat2.R
import com.example.xchat2.chat.RoomListFragment
import com.example.xchat2.ui.main.favourite.FavouriteRoomsFragment
import com.example.xchat2.ui.main.login.LoginFragment
import com.example.xchat2.util.State
import kotlinx.android.synthetic.main.main_fragment.*
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    val viewModel: MainViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnLogin.setOnClickListener {
            (activity as MainActivity).replaceFragment(LoginFragment.newInstance())
        }

        btnRoomList.setOnClickListener {
            (activity as MainActivity).replaceFragment(RoomListFragment.newInstance())
        }

        btnFavourite.setOnClickListener {
            (activity as MainActivity).replaceFragment(FavouriteRoomsFragment.newInstance())
        }

        viewModel.tryLoginWithSavedInfo().observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Loaded -> txt_status.text = "Přihlášen:\n${it.data.name}"
                is State.Error -> txt_status.text = "Stav:\n Nepřihlášen"
            }
        })
    }
}
