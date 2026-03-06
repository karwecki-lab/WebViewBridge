package com.webviewbridge

/**
 * Immutable SDK configuration. Supplied once at startup; never mutated.
 *
 * @param baseUrl           HTTPS entry-point loaded by the WebView.
 * @param allowedDomains    Explicit allowlist (base URL host added automatically).
 * @param certificatePins   SHA-256 SPKI pins: `"sha256/BASE64=="`.
 * @param extraHeaders      Static headers added to every request.
 * @param enforceHttps      Block non-HTTPS navigation (default true).
 * @param jsHandlerName     JS bridge object name on `window` (default "webviewbridge").
 * @param messageValidation Validate inbound token on every bridge message.
 */
data class SdkConfig(
    val baseUrl:           String,
    val allowedDomains:    Set<String>         = emptySet(),
    val certificatePins:   Set<String>         = emptySet(),
    val extraHeaders:      Map<String, String> = emptyMap(),
    val enforceHttps:      Boolean             = true,
    val jsHandlerName:     String              = "webviewbridge",
    val messageValidation: Boolean             = true
) {
    init {
        require(baseUrl.startsWith("https://")) {
            "SdkConfig.baseUrl must use HTTPS. Got: $baseUrl"
        }
        require(jsHandlerName.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*"))) {
            "jsHandlerName must be a valid JS identifier."
        }
    }

    fun effectiveDomains(): Set<String> =
        allowedDomains + listOfNotNull(hostOf(baseUrl))

    companion object {
        fun hostOf(url: String): String? = runCatching {
            url.removePrefix("https://").removePrefix("http://")
                .split("/", "?", "#").first().split(":").first()
                .takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}
