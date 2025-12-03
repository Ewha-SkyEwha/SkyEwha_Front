import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl

/**
 * 서버가 307 Location을 http로 내려줘도
 * 요청 항상 https로 강제 업그레이드
 */
class HttpsEnforcer(
    private val host: String = "skyewha-trendie.kr"
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var req = chain.request()
        val url = req.url

        // 대상 호스트 & http인 경우만 https로 교체
        if (!url.isHttps && url.host == host) {
            val httpsUrl: HttpUrl = url.newBuilder()
                .scheme("https")
                .build()

            req = req.newBuilder()
                .url(httpsUrl)
                .build()
        }
        return chain.proceed(req)
    }
}