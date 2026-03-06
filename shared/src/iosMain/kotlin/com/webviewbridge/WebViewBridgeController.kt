package com.webviewbridge

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.WebKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class WebViewBridgeController(
    val config: SdkConfig,
    tokenProvider: TokenProvider,
    private val eventListener: SdkEventListener
) {
    val controller = SdkController(config, tokenProvider)
    val webView: WKWebView

    private val navDelegate: WvbNavigationDelegate
    private val msgHandler: WvbMessageHandler

    init {
        val wkCfg = WKWebViewConfiguration()
        val ucc = WKUserContentController()
        msgHandler = WvbMessageHandler(controller, eventListener)
        ucc.addScriptMessageHandler(msgHandler, name = config.jsHandlerName)
        wkCfg.userContentController = ucc
        wkCfg.allowsInlineMediaPlayback = false

        webView = WKWebView(frame = cValue<platform.CoreGraphics.CGRect>(), configuration = wkCfg)
        webView.allowsBackForwardNavigationGestures = false

        navDelegate = WvbNavigationDelegate(controller, eventListener)
        webView.navigationDelegate = navDelegate
    }

    fun setActionContext(ctx: ActionContext) { controller.setActionContext(ctx); load() }

    fun load() {
        val url = NSURL.URLWithString(config.baseUrl) ?: run {
            eventListener.onError(SdkError.ConfigError("Invalid baseUrl: ${config.baseUrl}")); return
        }
        val req = NSMutableURLRequest.requestWithURL(url)
        controller.buildHeaders().forEach { (k, v) -> req.setValue(v, forHTTPHeaderField = k) }
        webView.loadRequest(req)
    }

    fun executeAction(name: String, params: Map<String, String> = emptyMap()) {
        val msg = NativeMessage.executeAction(controller.currentToken(), name, params)
        webView.evaluateJavaScript(controller.buildPostMessageScript(msg), completionHandler = null)
    }

    fun refreshToken() {
        val msg = NativeMessage.tokenRefreshed(controller.refreshToken())
        webView.evaluateJavaScript(controller.buildPostMessageScript(msg), completionHandler = null)
    }

    fun updateAllowedDomains(domains: Set<String>) = controller.updateAllowedDomains(domains)
}

// ── Navigation delegate ───────────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
internal class WvbNavigationDelegate(
    private val ctrl: SdkController,
    private val listener: SdkEventListener
) : NSObject(), WKNavigationDelegateProtocol {

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit
    ) {
        val url = decidePolicyForNavigationAction.request.URL?.absoluteString
            ?: return decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
        when (val r = ctrl.validateUrl(url)) {
            is ValidationResult.Allowed ->
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            is ValidationResult.Blocked -> {
                listener.onSecurityAuditEvent(r.event)
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            }
        }
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        val url = webView.URL?.absoluteString ?: return
        listener.onPageStarted(ctrl.sanitiseUrl(url))
        webView.evaluateJavaScript(ctrl.buildInjectionScript(), completionHandler = null)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        val url = webView.URL?.absoluteString ?: return
        webView.evaluateJavaScript(ctrl.buildInjectionScript(), completionHandler = null)
        ctrl.actionContext.value?.let { ctx ->
            val msg = NativeMessage.sessionInit(ctrl.currentToken(), ctx)
            webView.evaluateJavaScript(ctrl.buildPostMessageScript(msg), completionHandler = null)
        }
        listener.onPageLoaded(ctrl.sanitiseUrl(url))
    }

    override fun webView(
        webView: WKWebView,
        didFailNavigation: WKNavigation?,
        withError: NSError
    ) {
        listener.onError(SdkError.NetworkError("Navigation failed: ${withError.localizedDescription}"))
    }

    override fun webView(
        webView: WKWebView,
        didReceiveAuthenticationChallenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
    ) {
        val trust = didReceiveAuthenticationChallenge.protectionSpace.serverTrust
        if (ctrl.config.value.certificatePins.isEmpty()) {
            // NSURLSessionAuthChallengePerformDefaultHandling = 1
            completionHandler(1L, null)
            return
        }
        if (trust != null) {
            // NSURLSessionAuthChallengeUseCredential = 0
            completionHandler(0L, NSURLCredential.credentialForTrust(trust))
        } else {
            listener.onError(SdkError.CertificateError("Certificate validation failed"))
            // NSURLSessionAuthChallengeCancelAuthenticationChallenge = 2
            completionHandler(2L, null)
        }
    }
}

// ── Script message handler ────────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
internal class WvbMessageHandler(
    private val ctrl: SdkController,
    private val listener: SdkEventListener
) : NSObject(), WKScriptMessageHandlerProtocol {

    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage
    ) {
        val body = didReceiveScriptMessage.body as? String ?: return
        val msg = ctrl.parseBridgeMessage(body) ?: run {
            listener.onError(SdkError.MessageError("Unparseable bridge message")); return
        }
        if (!ctrl.validateMessage(msg)) {
            listener.onSecurityAuditEvent(AuditEvent(
                "wk-bridge", BlockReason.INVALID_MESSAGE_TOKEN,
                "Bridge message token mismatch — possible JS injection"
            ))
            return
        }
        listener.onBridgeMessage(msg)
    }
}
