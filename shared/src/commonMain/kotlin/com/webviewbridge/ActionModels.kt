package com.webviewbridge

import kotlinx.serialization.Serializable

// ─── Domain enums ────────────────────────────────────────────

enum class ObjectType(val value: String) {
    POLICY("POLICY"), SUBSCRIPTION("SUBSCRIPTION");
    companion object { fun from(v: String) = entries.firstOrNull { it.value == v } ?: SUBSCRIPTION }
}

enum class ActionType(val value: String) {
    ISSUE("ISSUE"), WITHDRAWAL("WITHDRAWAL"), RESIGNATION("RESIGNATION");
    companion object { fun from(v: String) = entries.firstOrNull { it.value == v } ?: ISSUE }
}

enum class UIMode(val value: String) { DARK("dark"), LIGHT("light") }

// ─── Action context ───────────────────────────────────────────

/**
 * All attributes describing a single native→web action.
 * Constructed by the host app and passed to the SDK view.
 */
@Serializable
data class ActionContext(
    val distributorId: String,
    val customerId:    String,
    val objectId:      String,
    val objectType:    String,   // ObjectType.value
    val action:        String,   // ActionType.value
    val lang:          String = "en",
    val uiMode:        String = UIMode.LIGHT.value,
    val extraParams:   Map<String, String> = emptyMap()
) {
    fun toSafeLog() = "ActionContext(dist=${distributorId.take(4)}****, " +
        "obj=$objectId, type=$objectType, action=$action, lang=$lang)"
}

// ─── Bridge message constants ─────────────────────────────────

object BridgeActions {
    const val INITIALIZE_SESSION = "initializeSession"
    const val EXECUTE_ACTION     = "executeAction"
    const val REQUEST_DATA       = "requestData"
    const val RETURN_RESULT      = "returnResult"
    const val REFRESH_TOKEN      = "refreshToken"
    const val NAVIGATE_BACK      = "navigateBack"
    const val CLOSE              = "close"
    const val ERROR_REPORT       = "errorReport"
}

// ─── Bridge message (web → native) ───────────────────────────

@Serializable
data class BridgeMessage(
    val action: String,
    val token:  String = "",
    val params: Map<String, String> = emptyMap()
) {
    fun toSafeLog() = "BridgeMessage(action=$action, token=****, params.keys=${params.keys})"
}

// ─── Native message (native → web) ───────────────────────────

data class NativeMessage(
    val type:    String,
    val token:   String = "",
    val payload: Map<String, String> = emptyMap()
) {
    /** Serialises to JSON without a library dependency in common. */
    fun toJson(): String = buildString {
        append("""{"type":"$type"""")
        if (token.isNotEmpty()) append(""","token":"$token"""")
        if (payload.isNotEmpty()) {
            append(""","payload":{""")
            append(payload.entries.joinToString(",") { (k, v) ->
                """"${k.escapeJson()}":"${v.escapeJson()}"""" })
            append("}")
        }
        append("}")
    }

    companion object {
        fun sessionInit(token: String, ctx: ActionContext) = NativeMessage(
            type = BridgeActions.INITIALIZE_SESSION,
            token = token,
            payload = buildMap {
                put("distributorId", ctx.distributorId)
                put("objectId",      ctx.objectId)
                put("objectType",    ctx.objectType)
                put("action",        ctx.action)
                put("lang",          ctx.lang)
                put("uiMode",        ctx.uiMode)
                putAll(ctx.extraParams)
            }
        )

        fun executeAction(token: String, name: String, params: Map<String, String>) =
            NativeMessage(
                type = BridgeActions.EXECUTE_ACTION,
                token = token,
                payload = mapOf("actionName" to name) + params
            )

        fun tokenRefreshed(newToken: String) =
            NativeMessage(type = BridgeActions.REFRESH_TOKEN, token = newToken)
    }
}

private fun String.escapeJson() = replace("\\", "\\\\").replace("\"", "\\\"")
