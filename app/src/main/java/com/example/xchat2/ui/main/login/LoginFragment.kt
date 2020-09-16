package com.example.xchat2.ui.main.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.xchat2.MainActivity
import com.example.xchat2.R
import com.example.xchat2.chat.ChatFragment
import com.example.xchat2.ui.main.repos.Chatroom
import com.example.xchat2.util.State
import com.example.xchat2.util.setVisible
import kotlinx.android.synthetic.main.login_fragment.*
import org.koin.android.viewmodel.ext.android.viewModel

class LoginFragment : Fragment() {

    companion object {
        val ARGS = "ARGS"

        fun newInstance(selectedRoom: Chatroom? = null): LoginFragment {
            val fragment = LoginFragment()

            if (selectedRoom != null) {
                val bundle = Bundle()
                bundle.putParcelable(ARGS, selectedRoom)
                fragment.arguments = bundleOf(ARGS to selectedRoom)
            }

            return fragment
        }
    }

    private val viewModel: LoginViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.login_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolblogin.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        btnSignin.setOnClickListener {
            viewModel.login(
                input_name.editText!!.text.toString(),
                input_password.editText!!.text.toString()
            ).observe(viewLifecycleOwner, Observer {
                progress_layout.setVisible(it is State.Loading)
                when (it) {
                    is State.Loaded -> {
                        val selectedRoom = getSelectedRoom()
                        if (selectedRoom == null) {
                            activity?.onBackPressed()
                        } else {
                            (activity as MainActivity).openFragment(ChatFragment.newInstance(selectedRoom))
                        }
                    }
                    is State.Error -> Toast.makeText(context, getString(R.string.login_failed), Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    fun getSelectedRoom(): Chatroom? {
        return arguments?.getParcelable(ARGS)
    }
}