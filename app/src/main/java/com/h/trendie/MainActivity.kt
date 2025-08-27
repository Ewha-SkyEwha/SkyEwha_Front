package com.h.trendie

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private val homeFragment = HomeFragment()
    private val keywordFragment = KeywordFragment()
    private val reportFragment = FeedbackUploadFragment()
    private val mypageFragment = MypageFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val container = findViewById<View>(R.id.fragment_container)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            val top = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            v.updatePadding(top = top)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, homeFragment, "home")
                .add(R.id.fragment_container, keywordFragment, "keyword").hide(keywordFragment)
                .add(R.id.fragment_container, reportFragment, "report").hide(reportFragment)
                .add(R.id.fragment_container, mypageFragment, "mypage").hide(mypageFragment)
                .commitNow()
        }

        fun show(tag: String) {
            val tx = supportFragmentManager.beginTransaction()
            supportFragmentManager.fragments.forEach { tx.hide(it) }
            (supportFragmentManager.findFragmentByTag(tag))?.let { tx.show(it) }
            tx.commit()
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> show("home")
                R.id.nav_keyword  -> show("keyword")
                R.id.nav_feedback -> show("report")
                R.id.nav_mypage   -> show("mypage")
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        // 초기 탭
        bottomNavigation.selectedItemId = R.id.nav_home
    }
}