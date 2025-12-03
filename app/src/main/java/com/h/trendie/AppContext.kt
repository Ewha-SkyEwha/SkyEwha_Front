package com.h.trendie.core

import android.app.Application
import android.content.Context

object AppContext {
    lateinit var app: Application
        private set

    fun init(application: Application) {
        app = application
    }

    val context: Context get() = app.applicationContext
}
