package com.h.trendie.util

import android.content.Context
import android.content.Context.MODE_PRIVATE

object UserPrefs {
    private const val SP = "user_prefs"
    const val KEY_NICKNAME = "nickname"
    const val KEY_EMAIL = "email"

    fun getNickname(ctx: Context): String =
        ctx.getSharedPreferences(SP, MODE_PRIVATE).getString(KEY_NICKNAME, "유저") ?: "유저"

    fun setNickname(ctx: Context, value: String) {
        ctx.getSharedPreferences(SP, MODE_PRIVATE).edit().putString(KEY_NICKNAME, value).apply()
    }

    fun getEmail(ctx: Context): String? =
        ctx.getSharedPreferences(SP, MODE_PRIVATE).getString(KEY_EMAIL, null)

    fun setEmail(ctx: Context, value: String?) {
        ctx.getSharedPreferences(SP, MODE_PRIVATE).edit().putString(KEY_EMAIL, value).apply()
    }
}