package com.h.trendie

import android.app.Application
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.h.trendie.network.ApiClient
import com.kakao.sdk.common.KakaoSdk
import com.h.trendie.core.AppContext

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        KakaoSdk.init(this, getString(R.string.kakao_native_app_key))
        AppContext.init(this)

        runCatching {
            val key = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val esp = EncryptedSharedPreferences.create(
                this,
                ApiConfig.PREFS_USER,
                key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val legacy = esp.getString(ApiConfig.KEY_ACCESS, null)
            if (!legacy.isNullOrBlank()) {
                com.h.trendie.auth.TokenProvider.save(this, legacy)
            }
        }
    }
}