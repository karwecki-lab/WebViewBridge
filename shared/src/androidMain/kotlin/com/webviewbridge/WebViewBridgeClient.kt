package com.webviewbridge

import android.graphics.Bitmap
import android.webkit.*
import java.io.ByteArrayInputStream

internal class WebViewBridgeClient(
    private val ctrl: SdkController,
    private val listener: SdkEventListener
) : WebViewClient() {

    // Block navigation to disallowed domains
    override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
        return when (val r = ctrl.validateUrl(req.url.toString())) {
            is ValidationResult.Allowed -> false
            is ValidationResult.Blocked -> { listener.onSecurityAuditEvent(r.event); true }
        }
    }

    // Block sub-resource requests to disallowed domains
    override fun shouldInterceptRequest(view: WebView, req: WebResourceRequest): WebResourceResponse? {
        return when (val r = ctrl.validateUrl(req.url.toString())) {
            is ValidationResult.Allowed -> null
            is ValidationResult.Blocked -> { listener.onSecurityAuditEvent(r.event); blockedResponse() }
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        listener.onPageStarted(ctrl.sanitiseUrl(url))
        view.evaluateJavascript(ctrl.buildInjectionScript(), null)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        view.evaluateJavascript(ctrl.buildInjectionScript(), null)
        ctrl.actionContext.value?.let { ctx ->
            val msg = NativeMessage.sessionInit(ctrl.currentToken(), ctx)
            view.evaluateJavascript(ctrl.buildPostMessageScript(msg), null)
        }
        listener.onPageLoaded(ctrl.sanitiseUrl(url))
    }

    private fun blockedResponse() = WebResourceResponse(
        "text/plain", "UTF-8", 403, "Forbidden",
        mapOf("X-WVB-Blocked" to "true"),
        ByteArrayInputStream("Blocked by WebViewBridge security policy.".toByteArray())
    )
}
