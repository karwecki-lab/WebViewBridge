package com.webviewbridge

// ─── Security audit ───────────────────────────────────────────

enum class BlockReason {
    DOMAIN_NOT_ALLOWED,
    INSECURE_SCHEME,
    SENSITIVE_PARAM_IN_URL,
    INVALID_MESSAGE_TOKEN,
    SECURITY_POLICY
}

data class AuditEvent(
    val blockedUrl:  String,
    val reason:      BlockReason,
    val detail:      String,
    val timestampMs: Long = currentTimeMillis()
) {
    fun toLog() = "[WVB AUDIT] reason=$reason url=$blockedUrl detail=$detail"
}

// ─── SDK errors ───────────────────────────────────────────────

sealed class SdkError(open val message: String, open val cause: Throwable? = null) {
    data class NetworkError(override val message: String, override val cause: Throwable? = null) : SdkError(message, cause)
    data class CertificateError(override val message: String, override val cause: Throwable? = null) : SdkError(message, cause)
    data class ConfigError(override val message: String) : SdkError(message)
    data class TokenError(override val message: String) : SdkError(message)
    data class MessageError(override val message: String) : SdkError(message)
}

// ─── Event listener ───────────────────────────────────────────

/**
 * Implement in the host application to receive SDK callbacks.
 * All callbacks arrive on the main thread.
 */
interface SdkEventListener {
    /** Web app sent a message via the JS bridge. */
    fun onBridgeMessage(message: BridgeMessage)

    /** A navigation or resource request was blocked. Route to SIEM. */
    fun onSecurityAuditEvent(event: AuditEvent)

    /** Unrecoverable SDK error. */
    fun onError(error: SdkError)

    fun onPageLoaded(url: String)  {}
    fun onPageStarted(url: String) {}
}
