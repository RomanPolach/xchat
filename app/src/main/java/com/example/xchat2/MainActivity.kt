package com.example.xchat2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.xchat2.ui.main.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }

    fun replaceFragment(fragment: Fragment, name: String = fragment.javaClass.name, addToBackStack: Boolean = true,
                        transition: Int = FragmentTransaction.TRANSIT_FRAGMENT_OPEN) {
        val transaction = supportFragmentManager.beginTransaction().replace(R.id.container, fragment, fragment.javaClass.name)
        if (addToBackStack) {
            transaction.addToBackStack(name)
        }
        transaction.setTransition(transition).commit()
    }
}
