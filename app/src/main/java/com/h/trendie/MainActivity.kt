package com.h.trendie

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val container = findViewById<View>(R.id.fragment_container)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = bottom)
            insets
        }

        if (savedInstanceState == null) {
            val home = HomeFragment()
            val keyword = KeywordFragment()
            val report = FeedbackUploadFragment()
            val mypage = MypageFragment()

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.fragment_container, home, TAG_HOME)
                add(R.id.fragment_container, keyword, TAG_KEYWORD); hide(keyword)
                add(R.id.fragment_container, report, TAG_REPORT); hide(report)
                add(R.id.fragment_container, mypage, TAG_MYPAGE); hide(mypage)
            }
        }

        fun show(tag: String) {
            getSystemService<InputMethodManager>()
                ?.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                supportFragmentManager.fragments.forEach { f ->
                    if (f.tag == tag) show(f) else hide(f)
                }
            }
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> show(TAG_HOME)
                R.id.nav_keyword  -> show(TAG_KEYWORD)
                R.id.nav_feedback -> show(TAG_REPORT)
                R.id.nav_mypage   -> show(TAG_MYPAGE)
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    private fun find(tag: String): Fragment? =
        supportFragmentManager.findFragmentByTag(tag)

    companion object {
        private const val TAG_HOME = "home"
        private const val TAG_KEYWORD = "keyword"
        private const val TAG_REPORT = "report"
        private const val TAG_MYPAGE = "mypage"
    }
}