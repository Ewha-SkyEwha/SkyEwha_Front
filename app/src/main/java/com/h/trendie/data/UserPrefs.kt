package com.h.trendie.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

private val Context.dataStore by preferencesDataStore("user_prefs")

object UserPrefs {
    private val KEY_NICKNAME = stringPreferencesKey("nickname")

    fun nicknameFlow(ctx: Context): Flow<String> =
        ctx.dataStore.data.map { it[KEY_NICKNAME] ?: "유저" }

    suspend fun setNickname(ctx: Context, name: String) {
        ctx.dataStore.edit { it[KEY_NICKNAME] = name.ifBlank { "유저" } }
    }
}