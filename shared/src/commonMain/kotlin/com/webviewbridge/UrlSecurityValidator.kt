package com.webviewbridge

class UrlSecurityValidator(private val config: SdkConfig) {

    fun validate(url: String): ValidationResult {
        // 1. Block internal JS execution
        if (url.startsWith("javascript:") || url.startsWith("data:text/html")) {
            return ValidationResult.Blocked(AuditEvent(
                sanitise(url), BlockReason.SECURITY_POLICY,
                "Dangerous scheme blocked: ${url.take(30)}"
            ))
        }

        // 2. HTTPS enforcement (http→https navigation only)
        if (config.enforceHttps && url.startsWith("http://")) {
            return ValidationResult.Blocked(AuditEvent(
                sanitise(url), BlockReason.INSECURE_SCHEME,
                "HTTP not allowed"
            ))
        }

        // 3. Sensitive query-param heuristic
        if (hasSensitiveParams(url)) {
            return ValidationResult.Blocked(AuditEvent(
                sanitise(url), BlockReason.SENSITIVE_PARAM_IN_URL,
                "URL contains sensitive parameters"
            ))
        }

        // 4. Domain allowlist (http/https only)
        if (url.startsWith("http://") || url.startsWith("https://")) {
            val host = hostOf(url)
            if (host != null && !isAllowed(host)) {
                return ValidationResult.Blocked(AuditEvent(
                    sanitise(url), BlockReason.DOMAIN_NOT_ALLOWED,
                    "Host '$host' not in allowlist"
                ))
            }
        }

        return ValidationResult.Allowed
    }

    fun sanitise(url: String): String = url.substringBefore("?").take(200)

    private fun isAllowed(host: String) =
        config.effectiveDomains().any { host == it || host.endsWith(".$it") }

    private fun hostOf(url: String) = SdkConfig.hostOf(url)

    private fun hasSensitiveParams(url: String): Boolean {
        val q = url.substringAfter("?", "").lowercase()
        return SENSITIVE_PARAMS.any { q.contains(it) }
    }

    companion object {
        private val SENSITIVE_PARAMS = listOf(
            "token=", "access_token=", "id_token=", "jwt=", "bearer=",
            "customerid=", "customer_id=", "distributorid=", "secret=",
            "password=", "passwd=", "api_key=", "apikey="
        )
    }
}

sealed class ValidationResult {
    object Allowed : ValidationResult()
    data class Blocked(val event: AuditEvent) : ValidationResult()
}
