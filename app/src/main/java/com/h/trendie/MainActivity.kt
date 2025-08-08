package com.h.trendie

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private val homeFragment = HomeFragment()
    private val keywordFragment = KeywordFragment()
    private val reportFragment = FeedbackUploadFragment()
    private val mypageFragment = MypageFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment? = when (item.itemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_keyword -> keywordFragment
                R.id.nav_feedback -> reportFragment
                R.id.nav_mypage -> mypageFragment
                else -> null
            }

            selectedFragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, it)
                    .commit()
                true
            } ?: false
        }

        bottomNavigation.selectedItemId = R.id.nav_home
    }
}