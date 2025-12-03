package com.h.trendie.auth

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenProvider {
    private const val TAG = "TokenProvider"

    // 표준 저장소/키
    private const val FILE_SECURE = "auth_prefs_secure"
    private const val KEY_ACCESS  = "access_token"

    // 과거/다른 모듈에서 혼재되던 저장소/키
    private val legacyFiles = listOf(
        "auth_prefs",
        "user_prefs",
        "Userprefs",
        "prefs",
        "auth",
        "user_secure_prefs",
        FILE_SECURE
    )
    private val legacyKeys  = listOf(
        "access_token",
        "accessToken",
        "token",
        "jwt",
        "Authorization"
    )

    // 인메모리 캐시(앱 생존 동안만 유지)
    @Volatile private var cachedToken: String? = null

    // --- Public API ---
    fun save(ctx: Context, token: String) {
        val clean = stripBearer(token)
        cachedToken = clean

        val (sp, isEncrypted) = securePrefs(ctx)
        sp.edit().putString(KEY_ACCESS, clean).apply()
        Log.d(TAG, "save -> ${if (isEncrypted) "Encrypted" else "Plain"}:$FILE_SECURE:$KEY_ACCESS preview=${previewText(clean)}")
    }

    fun saveBearer(ctx: Context, bearerValue: String) = save(ctx, stripBearer(bearerValue))

    fun get(ctx: Context): String? {
        // 0) 캐시
        cachedToken?.let { return it }

        // 1) secure
        val (sp, _) = securePrefs(ctx)
        sp.getString(KEY_ACCESS, null)?.let {
            cachedToken = it
            return it
        }

        // 2) legacy -> secure migration
        migrateFromLegacyIfExists(ctx)?.let { migrated ->
            cachedToken = migrated
            Log.d(TAG, "migrated from legacy store: preview=${previewText(migrated)}")
            return migrated
        }

        Log.w(TAG, "No token found in standard/legacy stores")
        return null
    }

    fun getBearer(ctx: Context): String? = get(ctx)?.let { "Bearer $it" }

    fun clear(ctx: Context) {
        cachedToken = null
        val (sp, _) = securePrefs(ctx)
        sp.edit().remove(KEY_ACCESS).apply()
    }

    fun preview(ctx: Context): String {
        val t = get(ctx) ?: return ""
        return previewText(t)
    }

    // --- Internal ---
    private fun securePrefs(ctx: Context) = runCatching {
        val key = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val esp = EncryptedSharedPreferences.create(
            ctx,
            FILE_SECURE,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        esp to true
    }.getOrElse {
        // (구형 기기/테스트 환경) 보안 모듈 미탑재 시 평문 폴백
        ctx.getSharedPreferences(FILE_SECURE, Context.MODE_PRIVATE) to false
    }

    private fun migrateFromLegacyIfExists(ctx: Context): String? {
        for (f in legacyFiles) {
            val sp = ctx.getSharedPreferences(f, Context.MODE_PRIVATE)
            for (k in legacyKeys) {
                val raw = sp.getString(k, null)
                if (!raw.isNullOrBlank()) {
                    val cleaned = stripBearer(raw)
                    // move & wipe
                    save(ctx, cleaned)
                    sp.edit().remove(k).apply()
                    Log.d(TAG, "migrate: $f:$k -> $FILE_SECURE:$KEY_ACCESS preview=${previewText(cleaned)}")
                    return cleaned
                }
            }
        }
        return null
    }

    private fun stripBearer(v: String): String {
        val trimmed = v.trim()
        return if (trimmed.startsWith("Bearer ", ignoreCase = true)) {
            trimmed.substringAfter(' ').trim()
        } else trimmed
    }

    private fun previewText(token: String): String {
        val head = token.take(6)
        return "$head… (len=${token.length})"
    }
}
