package com.webviewbridge

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Platform-agnostic state machine shared by Android and iOS adapters.
 *
 * Holds: current [SdkConfig], active [ActionContext], and the [TokenProvider].
 * Platform-specific views delegate all routing and security decisions here.
 */
class SdkController(
    initialConfig: SdkConfig,
    private val tokenProvider: TokenProvider
) {
    private val _config = MutableStateFlow(initialConfig)
    val config: StateFlow<SdkConfig> = _config.asStateFlow()

    private val _context = MutableStateFlow<ActionContext?>(null)
    val actionContext: StateFlow<ActionContext?> = _context.asStateFlow()

    private var validator = UrlSecurityValidator(initialConfig)

    // ── Token ────────────────────────────────────────────────

    fun currentToken(): String = tokenProvider.getToken()
    fun refreshToken(): String = tokenProvider.refreshToken()

    // ── Context ──────────────────────────────────────────────

    fun setActionContext(ctx: ActionContext) { _context.value = ctx }

    // ── Config updates ────────────────────────────────────────

    fun updateAllowedDomains(domains: Set<String>) {
        val updated = _config.value.copy(allowedDomains = domains)
        _config.value = updated
        validator = UrlSecurityValidator(updated)
    }

    // ── Security ──────────────────────────────────────────────

    fun validateUrl(url: String): ValidationResult = validator.validate(url)
    fun sanitiseUrl(url: String): String = validator.sanitise(url)

    /** Returns true when message token matches current token (or validation disabled). */
    fun validateMessage(msg: BridgeMessage): Boolean {
        if (!_config.value.messageValidation) return true
        if (msg.token.isBlank()) return false
        return msg.token == tokenProvider.getToken()
    }

    // ── Headers ───────────────────────────────────────────────

    fun buildHeaders(): Map<String, String> = buildMap {
        putAll(_config.value.extraHeaders)
        put("Authorization", "Bearer ${tokenProvider.getToken()}")
        _context.value?.let { ctx ->
            put("X-WVB-Distributor-Id", ctx.distributorId)
            put("X-WVB-Object-Type",    ctx.objectType)
            put("X-WVB-Action",         ctx.action)
            put("X-WVB-Lang",           ctx.lang)
            put("X-WVB-UI-Mode",        ctx.uiMode)
            ctx.extraParams.forEach { (k, v) -> put("X-WVB-$k", v) }
        }
    }

    // ── Message parsing ───────────────────────────────────────

    fun parseBridgeMessage(raw: String): BridgeMessage? = runCatching {
        json.decodeFromString(BridgeMessage.serializer(), raw)
    }.getOrNull()

    // ── JS scripts ───────────────────────────────────────────

    /**
     * Injected on every page load.
     * Exposes `window.<handlerName>.send(action, params)`.
     * Raw token is NOT exposed to JS — only non-sensitive context fields.
     */
    fun buildInjectionScript(): String {
        val ctx = _context.value
        val name = _config.value.jsHandlerName
        val ctxJson = ctx?.let {
            """{"distributorId":"${it.distributorId}","objectType":"${it.objectType}","action":"${it.action}","lang":"${it.lang}","uiMode":"${it.uiMode}"}"""
        } ?: "{}"

        return """
(function(){
  if(window.__wvb_init)return;
  window.__wvb_init=true;
  var _tok='';
  window.$name={
    context:$ctxJson,
    send:function(action,params){
      var m=JSON.stringify({action:action,token:_tok,params:params||{}});
      if(window.webkit&&window.webkit.messageHandlers&&window.webkit.messageHandlers.$name)
        window.webkit.messageHandlers.$name.postMessage(m);
      else if(window.${name}Native)
        window.${name}Native.postMessage(m);
    },
    _setToken:function(t){_tok=t;}
  };
  window.dispatchEvent(new CustomEvent('wvb:ready',{detail:window.$name.context}));
})();
""".trimIndent()
    }

    /** JS that pushes a [NativeMessage] event into the web app. */
    fun buildPostMessageScript(msg: NativeMessage): String {
        val name = _config.value.jsHandlerName
        val json = msg.toJson()
        return """
(function(){
  window.dispatchEvent(new CustomEvent('wvb:message',{detail:$json}));
  if(window.$name&&'${msg.token}'.length>0)window.$name._setToken('${msg.token}');
})();
""".trimIndent()
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }
}
