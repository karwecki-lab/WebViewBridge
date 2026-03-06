package com.webviewbridge

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Drop-in Android [WebView] subclass — the single integration point for Android.
 *
 * ## Quick Start
 * ```kotlin
 * val sdk = WebViewBridgeView(
 *     context = this,
 *     config  = SdkConfig(
 *         baseUrl        = "https://my-webapp.example.com",
 *         allowedDomains = setOf("my-webapp.example.com")
 *     ),
 *     tokenProvider = MyTokenProvider(),
 *     eventListener = myListener
 * )
 * setContentView(sdk)
 * sdk.setActionContext(ctx)   // triggers load automatically
 * ```
 */
@SuppressLint("SetJavaScriptEnabled")
class WebViewBridgeView constructor(
    context: Context,
    private val sdkConfig: SdkConfig,
    tokenProvider: TokenProvider,
    private val eventListener: SdkEventListener
) : WebView(context) {

    val controller = SdkController(sdkConfig, tokenProvider)
    private val main = Handler(Looper.getMainLooper())

    init {
        applySecureSettings()
        webViewClient = WebViewBridgeClient(controller, eventListener)
        addJavascriptInterface(
            AndroidJsBridge(controller, eventListener) { main.post(it) },
            "${sdkConfig.jsHandlerName}Native"
        )
    }

    // ── Public API ────────────────────────────────────────────

    /**
     * Set a new [ActionContext] and reload the WebView.
     * Call this every time the user taps a button in the host app.
     */
    fun setActionContext(ctx: ActionContext) {
        controller.setActionContext(ctx)
        load()
    }

    /** Load [SdkConfig.baseUrl] with current auth headers. */
    fun load() = loadUrl(sdkConfig.baseUrl, controller.buildHeaders())

    /**
     * Send a named action with dynamic parameters to the web app.
     * ```kotlin
     * sdk.executeAction("openProductDetails", mapOf("productId" to "12345"))
     * ```
     */
    fun executeAction(name: String, params: Map<String, String> = emptyMap()) {
        val msg = NativeMessage.executeAction(controller.currentToken(), name, params)
        evaluateJavascript(controller.buildPostMessageScript(msg), null)
    }

    /** Refresh the token via [TokenProvider] and push the new token to the web app. */
    fun refreshToken() {
        val newToken = controller.refreshToken()
        evaluateJavascript(controller.buildPostMessageScript(NativeMessage.tokenRefreshed(newToken)), null)
    }

    /** Update the domain allowlist at runtime (no reload needed). */
    fun updateAllowedDomains(domains: Set<String>) = controller.updateAllowedDomains(domains)

    // ── Hardened WebView settings ─────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun applySecureSettings() {
        settings.apply {
            javaScriptEnabled          = true
            domStorageEnabled          = true
            mixedContentMode           = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess            = false
            allowContentAccess         = false
            cacheMode                  = WebSettings.LOAD_NO_CACHE
            setSupportZoom(false)
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
        }
    }
}
