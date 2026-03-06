package com.webviewbridge

import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Optional OkHttp client with cert pinning + auth headers.
 * Use when the host app needs to make API calls alongside the WebView.
 */
internal class HeaderInterceptor(private val ctrl: SdkController) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()
        ctrl.buildHeaders().forEach { (k, v) -> req.header(k, v) }
        return chain.proceed(req.build())
    }
}

object OkHttpClientFactory {
    fun create(config: SdkConfig, ctrl: SdkController): OkHttpClient {
        val b = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor(ctrl))
        if (config.certificatePins.isNotEmpty()) {
            val pinner = CertificatePinner.Builder()
            config.effectiveDomains().forEach { domain ->
                config.certificatePins.forEach { pin -> pinner.add(domain, pin) }
            }
            b.certificatePinner(pinner.build())
        }
        return b.build()
    }
}
