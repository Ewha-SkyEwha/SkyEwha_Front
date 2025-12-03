package com.h.trendie.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.h.trendie.ApiConfig

object NicknameStore {

    private fun secureSp(ctx: Context) =
        EncryptedSharedPreferences.create(
            ctx,
            ApiConfig.PREFS_USER,
            MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    /** 닉네임 읽기 */
    fun get(ctx: Context): String {
        val sec = secureSp(ctx)
        val cur = sec.getString(ApiConfig.KEY_NICKNAME, null)
        if (!cur.isNullOrBlank()) return cur

        val legacyFiles = listOf("user_prefs", "Userprefs", "UserPrefs", "profile_prefs", "mypage_prefs")
        val legacyKeys  = listOf("nickname", "user_nickname", "nick_name", "name", "userName")
        for (file in legacyFiles) {
            val sp = ctx.getSharedPreferences(file, Context.MODE_PRIVATE)
            for (k in legacyKeys) {
                val v = sp.getString(k, null)
                if (!v.isNullOrBlank()) {
                    sec.edit().putString(ApiConfig.KEY_NICKNAME, v).apply()
                    return v
                }
            }
        }
        return "유저"
    }

    /** 닉네임 저장(항상 보안 prefs) */
    fun set(ctx: Context, nickname: String) {
        if (nickname.isBlank()) return
        secureSp(ctx).edit().putString(ApiConfig.KEY_NICKNAME, nickname).apply()
    }
}
