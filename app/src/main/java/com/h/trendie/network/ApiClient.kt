package com.h.trendie.network

import android.content.Context
import android.util.Log
import com.h.trendie.ApiConfig
import com.h.trendie.BuildConfig
import com.h.trendie.auth.TokenProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val DEFAULT_BASE_URL = "https://skyewha-trendie.kr/"
    private const val MAX_LOG_BYTES: Long = 64 * 1024 // 64KB

    private lateinit var appContext: Context
    fun init(context: Context) {
        appContext = context.applicationContext
        Log.d("ApiClient", "init base=${normalizedBaseUrl()}")
    }

    fun setJwt(token: String?) {
        if (token.isNullOrBlank()) {
            TokenProvider.clear(appContext)
        } else {
            TokenProvider.save(appContext, token)
        }
    }
    fun saveJwt(raw: String?) = setJwt(raw)
    fun clearJwt() = TokenProvider.clear(appContext)

    private fun normalizedBaseUrl(): String {
        val raw = ApiConfig.BASE_URL.ifBlank { DEFAULT_BASE_URL }
        return if (raw.endsWith("/")) raw else "$raw/"
    }

    private val schemeUpgrade = Interceptor { chain ->
        var req = chain.request()
        if (!req.url.isHttps) {
            val https = req.url.newBuilder().scheme("https").port(443).build()
            Log.d("CleartextFix", "upgrade ${req.url} -> $https")
            req = req.newBuilder().url(https).build()
        }
        chain.proceed(req)
    }

    private val auth = Interceptor { chain ->
        val r0 = chain.request()
        val bearer = TokenProvider.getBearer(appContext)
        val rb = r0.newBuilder().addHeader("Accept", "application/json")
        if (!bearer.isNullOrBlank()) {
            rb.addHeader("Authorization", bearer)
            // 길이만 로깅
            Log.d("Auth", "attach Authorization (len=${bearer.length - 7}) to ${r0.url.encodedPath}")
        } else {
            Log.w("Auth", "NO Authorization for ${r0.url}")
        }
        chain.proceed(rb.build())
    }

    private val redirectSniffer = Interceptor { chain ->
        val req = chain.request()
        val res = chain.proceed(req)
        if (res.code in 300..399) {
            Log.w("Redirect", "→ ${req.method} ${req.url}\n← ${res.code} Location: ${res.header("Location")}")
        }
        res
    }

    private fun normalizeRedirectTarget(target: HttpUrl, original: HttpUrl): HttpUrl {
        return if (target.scheme == "http" && target.host == original.host) {
            target.newBuilder().scheme("https").port(443).build()
        } else target
    }

    private val keepMethodRedirect = Interceptor { chain ->
        var request = chain.request()
        var response = chain.proceed(request)
        var hops = 0
        while (response.code in listOf(301, 302, 303, 307, 308) && hops < 10) {
            val loc = response.header("Location") ?: break
            response.close()
            val target = request.url.resolve(loc)
                ?: runCatching { loc.toHttpUrl() }.getOrElse {
                    throw IllegalStateException("Bad Location: $loc")
                }
            val normalized = normalizeRedirectTarget(target, request.url)
            Log.w("RedirectFix", "Re-issuing ${request.method} to $normalized (keep method)")
            request = request.newBuilder().url(normalized).build() // 헤더 유지됨
            response = chain.proceed(request)
            hops++
        }
        response
    }

    private fun isKeywordPath(url: HttpUrl) =
        url.encodedPath.startsWith("/api/v1/keyword/")
    private fun isFeedbackPath(url: HttpUrl) =
        url.encodedPath.startsWith("/api/v1/feedback/")
    private fun isProblemPath(url: HttpUrl) =
        isFeedbackPath(url) || isKeywordPath(url)

    private val closeOnProblemPath = Interceptor { chain ->
        val req = chain.request()
        if (isProblemPath(req.url)) {
            chain.proceed(req.newBuilder().header("Connection", "close").build())
        } else {
            chain.proceed(req)
        }
    }

    private val curlLogger = Interceptor { chain ->
        val req = chain.request()
        if (!BuildConfig.DEBUG) return@Interceptor chain.proceed(req)

        val sb = StringBuilder("curl -i -X ${req.method} '${req.url}'")
        for (name in req.headers.names()) {
            val value = when (name.lowercase()) {
                "authorization", "cookie", "set-cookie" -> "REDACTED"
                else -> req.header(name) ?: ""
            }
            sb.append(" \\\n  -H '$name: $value'")
        }
        Log.w("cURL", sb.toString())
        chain.proceed(req)
    }

    private fun isTextLike(ct: MediaType?): Boolean {
        if (ct == null) return false
        val type = ct.type.lowercase()
        val subtype = ct.subtype.lowercase()
        return type == "text" ||
                (type == "application" && (
                        "json" in subtype || "xml" in subtype || "x-www-form-urlencoded" in subtype
                        ))
    }

    private val errorBodySniffer = Interceptor { chain ->
        val req = chain.request()
        val res = chain.proceed(req)
        if (!res.isSuccessful) {
            val ct = res.body?.contentType()
            if (isTextLike(ct)) {
                val peek = res.peekBody(MAX_LOG_BYTES)
                Log.e(
                    "HTTP${res.code}",
                    "❌ ${req.method} ${req.url}\nHeaders=${req.headers}\nErrorBody=${peek.string()}"
                )
            } else {
                Log.e(
                    "HTTP${res.code}",
                    "❌ ${req.method} ${req.url}\nHeaders=${req.headers}\nErrorBody=<non-text ${ct}>"
                )
            }
        }
        res
    }

    private val tokenAccessor = object : TokenProviderAccessor {
        override fun getAccessToken(): String? = TokenProvider.get(appContext)
        override fun getRefreshToken(): String? {
            return appContext.getSharedPreferences("auth_prefs_secure", Context.MODE_PRIVATE)
                .getString("refresh_token", null)
        }
        override fun saveTokens(access: String?, refresh: String?) {
            if (!access.isNullOrBlank()) TokenProvider.save(appContext, access)
            if (!refresh.isNullOrBlank()) {
                appContext.getSharedPreferences("auth_prefs_secure", Context.MODE_PRIVATE)
                    .edit().putString("refresh_token", refresh).apply()
            }
        }
    }

    private fun baseBuilder(): OkHttpClient.Builder {
        val logging = HttpLoggingInterceptor { Log.d("OkHttp", it) }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization"); redactHeader("Cookie"); redactHeader("Set-Cookie")
        }

        return OkHttpClient.Builder()
            .addInterceptor(schemeUpgrade)
            .addInterceptor(auth)
            .addInterceptor(redirectSniffer)
            .addInterceptor(curlLogger)
            .addInterceptor(errorBodySniffer)
            .addNetworkInterceptor(logging)
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .eventListener(object : EventListener() {
                override fun responseHeadersEnd(call: Call, response: Response) {
                    if (response.code == 405) {
                        Log.e("HTTP405", "Allow = ${response.header("Allow")} url=${call.request().url}")
                    }
                }
                override fun callFailed(call: Call, ioe: IOException) {
                    Log.e("HTTP", "callFailed: ${ioe.message}", ioe)
                }
            })
            .authenticator(TokenRefreshAuthenticator(normalizedBaseUrl(), tokenAccessor))
    }

    private val client: OkHttpClient by lazy {
        baseBuilder()
            .addInterceptor(keepMethodRedirect)
            .build()
    }

    private val clientH1: OkHttpClient by lazy {
        baseBuilder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
            .callTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(keepMethodRedirect)
            .addInterceptor(closeOnProblemPath)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl())
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private val retrofitH1: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl())
            .client(clientH1)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }

    val apiServiceH1: ApiService by lazy { retrofitH1.create(ApiService::class.java) }

    @Suppress("unused")
    val apiServiceProd: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl())
            .client(
                baseBuilder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
            )
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
