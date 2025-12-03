package com.h.trendie

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.h.trendie.ApiConfig.KEY_ACCESS
import com.h.trendie.ApiConfig.KEY_EMAIL
import com.h.trendie.ApiConfig.KEY_NICKNAME
import com.h.trendie.ApiConfig.KEY_PROVIDER
import com.h.trendie.ApiConfig.KEY_REFRESH
import com.h.trendie.ApiConfig.PREFS_USER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore


class UserPrefs(private val context: Context) {

    private val secureSp: SharedPreferences by lazy {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_USER,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** access 토큰 존재 여부 */
    fun hasAccess(): Boolean = !getAccess().isNullOrBlank()

    fun getAccess(): String?  = secureSp.getString(KEY_ACCESS,   null)
    fun getRefresh(): String? = secureSp.getString(KEY_REFRESH,  null)
    fun getProvider(): String?= secureSp.getString(KEY_PROVIDER, null)

    fun saveTokens(access: String, refresh: String) {
        secureSp.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .apply()
    }

    /**
     * 프로필 저장
     * - provider는 보안 영역에
     * - nickname/email은 DataStore에
     */
    fun saveProfile(provider: String?, nickname: String?, email: String?) {
        if (!provider.isNullOrBlank()) {
            secureSp.edit().putString(KEY_PROVIDER, provider).apply()
        }
        runBlocking(Dispatchers.IO) {
            if (!nickname.isNullOrBlank()) setNickname(context, nickname)
            if (!email.isNullOrBlank()) setEmail(context, email)
        }
    }

    fun clearSecure() {
        secureSp.edit().clear().apply()
    }

    // -------- DataStore (닉네임/이메일) --------
    companion object {
        private val Context.dataStore by preferencesDataStore(name = "user_prefs")

        private val DS_KEY_NICKNAME = stringPreferencesKey(KEY_NICKNAME)
        private val DS_KEY_EMAIL    = stringPreferencesKey(KEY_EMAIL)

        /** 닉네임 실시간 스트림 (기본 "유저") */
        fun nicknameFlow(ctx: Context): Flow<String> =
            ctx.dataStore.data.map { it[DS_KEY_NICKNAME] ?: "유저" }

        /** suspend 읽기 */
        suspend fun getNickname(ctx: Context): String =
            ctx.dataStore.data.first()[DS_KEY_NICKNAME] ?: "유저"

        /** 즉시 읽기(동기) */
        fun getNicknameSync(ctx: Context): String =
            try { runBlocking(Dispatchers.IO) { getNickname(ctx) } } catch (_: Exception) { "유저" }

        /** 쓰기 */
        suspend fun setNickname(ctx: Context, name: String?) {
            ctx.dataStore.edit { prefs ->
                if (name.isNullOrBlank()) prefs.remove(DS_KEY_NICKNAME)
                else prefs[DS_KEY_NICKNAME] = name
            }
        }

        // ---- 이메일 ----
        fun emailFlow(ctx: Context): Flow<String?> =
            ctx.dataStore.data.map { it[DS_KEY_EMAIL] }

        suspend fun getEmail(ctx: Context): String? =
            ctx.dataStore.data.first()[DS_KEY_EMAIL]

        fun getEmailSync(ctx: Context): String? =
            try { runBlocking(Dispatchers.IO) { getEmail(ctx) } } catch (_: Exception) { null }

        suspend fun setEmail(ctx: Context, email: String?) {
            ctx.dataStore.edit { prefs ->
                if (email.isNullOrBlank()) prefs.remove(DS_KEY_EMAIL)
                else prefs[DS_KEY_EMAIL] = email
            }
        }

        /** 전체 삭제(로그아웃/탈퇴 시) */
        suspend fun clear(ctx: Context) {
            ctx.dataStore.edit { it.clear() }
        }
    }
}