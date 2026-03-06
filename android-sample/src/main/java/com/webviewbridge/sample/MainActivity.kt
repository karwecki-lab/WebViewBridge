package com.webviewbridge.sample

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.webviewbridge.*

/**
 * Android sample — three action buttons + WebView.
 *
 * Demonstrates full WebViewBridge SDK integration:
 *   • TokenProvider (stub — replace with real auth)
 *   • SdkEventListener
 *   • Three preset ActionContexts mapped to buttons
 */
class MainActivity : AppCompatActivity() {

    private lateinit var sdk: WebViewBridgeView
    private val TAG = "WVB-Sample"

    // ── TokenProvider (stub) ──────────────────────────────────
    private val tokenProvider = object : TokenProvider {
        private var token = "eyJhbGciOiJIUzI1NiJ9.REPLACE_WITH_REAL_TOKEN"
        override fun getToken(): String = token
        override fun refreshToken(): String {
            // TODO: call your auth endpoint
            token = "eyJhbGciOiJIUzI1NiJ9.refreshed_${System.currentTimeMillis()}"
            Log.i(TAG, "Token refreshed")
            return token
        }
    }

    // ── SdkEventListener ──────────────────────────────────────
    private val eventListener = object : SdkEventListener {
        override fun onBridgeMessage(message: BridgeMessage) {
            Log.d(TAG, message.toSafeLog())
            when (message.action) {
                BridgeActions.NAVIGATE_BACK -> onBackPressedDispatcher.onBackPressed()
                BridgeActions.CLOSE         -> finish()
                BridgeActions.REFRESH_TOKEN -> sdk.refreshToken()
                BridgeActions.RETURN_RESULT -> {
                    val result = message.params["result"] ?: "—"
                    Toast.makeText(this@MainActivity, "Result: $result", Toast.LENGTH_SHORT).show()
                }
                BridgeActions.REQUEST_DATA -> {
                    // Example: return data the web app requested
                    sdk.executeAction(BridgeActions.RETURN_RESULT,
                        mapOf("result" to "ok", "requestedAction" to (message.params["dataKey"] ?: "")))
                }
            }
        }

        override fun onSecurityAuditEvent(event: AuditEvent) {
            Log.w(TAG, event.toLog())
            // TODO: forward to SIEM / analytics
        }

        override fun onError(error: SdkError) {
            Log.e(TAG, "SDK error: $error")
            if (error is SdkError.TokenError) sdk.refreshToken()
        }

        override fun onPageLoaded(url: String)  { Log.i(TAG, "Loaded:  $url") }
        override fun onPageStarted(url: String) { Log.i(TAG, "Started: $url") }
    }

    // ── Lifecycle ─────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = SdkConfig(
            baseUrl        = "https://nnw-frontend.int.npr.dbyc.cardif.io",
            allowedDomains = setOf(
                "nnw-frontend.int.npr.dbyc.cardif.io",
                "cardif.io"
            ),
            enforceHttps      = true,
            messageValidation = true
        )

        sdk = WebViewBridgeView(this, config, tokenProvider, eventListener)

        // ── Layout: [Btn1][Btn2][Btn3]
        //             [   WebView    ]  ──
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER
            setPadding(12, 12, 12, 4)
        }
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            .apply { setMargins(6, 0, 6, 0) }

        btnRow.addView(makeButton("Subscribe\n+ Issue")    { trigger(SUBSCRIPTION_ISSUE) },   lp)
        btnRow.addView(makeButton("Policy\n+ Withdrawal")  { trigger(POLICY_WITHDRAWAL) },    lp)
        btnRow.addView(makeButton("Policy\n+ Resignation") { trigger(POLICY_RESIGNATION) },   lp)

        root.addView(btnRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(sdk, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)
        trigger(SUBSCRIPTION_ISSUE)   // default context on launch
    }

    private fun makeButton(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun trigger(ctx: ActionContext) = sdk.setActionContext(ctx)

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (sdk.canGoBack()) sdk.goBack() else super.onBackPressed()
    }

    // ── Preset action contexts ────────────────────────────────

    companion object {
        private val SUBSCRIPTION_ISSUE = ActionContext(
            distributorId = "DIST-001",
            customerId    = "CUST-12345",
            objectId      = "OBJ-001",
            objectType    = ObjectType.SUBSCRIPTION.value,
            action        = ActionType.ISSUE.value,
            lang          = "en",
            uiMode        = UIMode.LIGHT.value
        )
        private val POLICY_WITHDRAWAL = ActionContext(
            distributorId = "DIST-001",
            customerId    = "CUST-12345",
            objectId      = "POL-999",
            objectType    = ObjectType.POLICY.value,
            action        = ActionType.WITHDRAWAL.value,
            lang          = "en",
            uiMode        = UIMode.LIGHT.value
        )
        private val POLICY_RESIGNATION = ActionContext(
            distributorId = "DIST-001",
            customerId    = "CUST-12345",
            objectId      = "POL-999",
            objectType    = ObjectType.POLICY.value,
            action        = ActionType.RESIGNATION.value,
            lang          = "en",
            uiMode        = UIMode.LIGHT.value
        )
    }
}
