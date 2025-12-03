package com.h.trendie

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private var navigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            delay(700)

            navigateNext()
        }
    }

    private fun navigateNext() {
        if (navigated) return
        navigated = true

        val hasAccess = runCatching { UserPrefs(this).hasAccess() }.getOrElse { false }
        val next = if (hasAccess) MainActivity::class.java else LoginActivity::class.java

        startActivity(
            Intent(this, next).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}