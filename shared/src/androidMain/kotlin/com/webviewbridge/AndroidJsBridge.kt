package com.webviewbridge

import android.webkit.JavascriptInterface

internal class AndroidJsBridge(
    private val ctrl: SdkController,
    private val listener: SdkEventListener,
    private val post: (Runnable) -> Unit   // main-thread dispatcher
) {
    @JavascriptInterface
    fun postMessage(raw: String) {
        val msg = ctrl.parseBridgeMessage(raw) ?: run {
            post(Runnable { listener.onError(SdkError.MessageError("Unparseable bridge message.")) })
            return
        }
        if (!ctrl.validateMessage(msg)) {
            post(Runnable {
                listener.onSecurityAuditEvent(AuditEvent(
                    "js-bridge", BlockReason.INVALID_MESSAGE_TOKEN,
                    "Bridge message token mismatch — possible JS injection"
                ))
            })
            return
        }
        post(Runnable { listener.onBridgeMessage(msg) })
    }
}
