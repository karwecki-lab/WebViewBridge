package com.webviewbridge

/**
 * Integrator-implemented token interface.
 *
 * The SDK never fetches, stores, or caches tokens.
 * All token operations are delegated to the host application.
 *
 * Android (Kotlin):
 * ```kotlin
 * class MyTokenProvider : TokenProvider {
 *     override fun getToken(): String = authService.currentToken()
 *     override fun refreshToken(): String = authService.renewToken()
 * }
 * ```
 * iOS (Swift) — bridged automatically from the KMP framework:
 * ```swift
 * class MyTokenProvider: TokenProvider {
 *     func getToken() -> String { AuthService.shared.token }
 *     func refreshToken() -> String { AuthService.shared.renew() }
 * }
 * ```
 */
interface TokenProvider {
    /** Returns the current valid session token. Called on every request. */
    fun getToken(): String

    /**
     * Fetches a renewed token. Called when the web app sends
     * [BridgeActions.REFRESH_TOKEN] or the current token is rejected.
     */
    fun refreshToken(): String
}
