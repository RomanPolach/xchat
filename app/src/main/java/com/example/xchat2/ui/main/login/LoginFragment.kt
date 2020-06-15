package com.example.xchat2.ui.main.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.xchat2.R
import com.example.xchat2.util.State
import com.example.xchat2.util.setVisible
import kotlinx.android.synthetic.main.chat_fragment.*
import kotlinx.android.synthetic.main.login_fragment.*
import kotlinx.android.synthetic.main.main_fragment.*
import org.jsoup.Jsoup
import org.koin.android.viewmodel.ext.android.viewModel

class LoginFragment : Fragment() {

    companion object {
        fun newInstance() = LoginFragment()
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
                        Toast.makeText(context, "Šlo to", Toast.LENGTH_LONG).show()
                        activity?.onBackPressed()
                    }
                    is State.Error -> Toast.makeText(context, "Nejde", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}
