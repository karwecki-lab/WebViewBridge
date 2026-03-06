# WebViewBridge SDK

Financial-grade Kotlin Multiplatform SDK providing a secure, reusable WebView wrapper for Android and iOS.

---

## Architecture

```
WebViewBridge/
├── shared/                          # KMP shared module (compiles to AAR + xcframework)
│   └── src/
│       ├── commonMain/kotlin/com/webviewbridge/
│       │   ├── TokenProvider.kt     # Integrator-implemented interface
│       │   ├── ActionModels.kt      # ActionContext, BridgeMessage, NativeMessage, enums
│       │   ├── SdkConfig.kt         # Immutable configuration
│       │   ├── SdkController.kt     # Platform-agnostic state machine
│       │   ├── SdkEvents.kt         # SdkEventListener, AuditEvent, SdkError
│       │   └── UrlSecurityValidator.kt
│       ├── androidMain/kotlin/com/webviewbridge/
│       │   ├── WebViewBridgeView.kt # Drop-in Android WebView subclass
│       │   ├── WebViewBridgeClient.kt
│       │   ├── AndroidJsBridge.kt
│       │   └── OkHttpSupport.kt
│       └── iosMain/kotlin/com/webviewbridge/
│           └── WebViewBridgeController.kt # iOS WKWebView manager
├── android-sample/                  # Android host app sample
├── ios-sample/                      # iOS Swift host app sample
└── react-app/                       # React web app (runs inside the WebView)
    └── src/
        ├── types/       ActionContext, BridgeMessage, route helpers
        ├── services/    TokenProvider, SdkBridge
        ├── hooks/       useActionContext
        ├── components/  ActionSimulator, ContextCard
        └── pages/       SubscriptionIssuePage, PolicyWithdrawalPage, PolicyResignationPage
```

---

## 1. TokenProvider Interface

The SDK **never** fetches, stores, or persists tokens. The host app supplies them.

### Android (Kotlin)
```kotlin
class MyTokenProvider : TokenProvider {
    override fun getToken(): String = authService.currentToken()
    override fun refreshToken(): String = authService.renewToken()
}
```

### iOS (Swift)
```swift
class MyTokenProvider: TokenProvider {
    func getToken() -> String    { AuthService.shared.token }
    func refreshToken() -> String{ AuthService.shared.renew() }
}
```

### Web / React
```ts
class MyTokenProvider extends TokenProvider {
    async getToken()      { return authService.currentToken(); }
    async refreshToken()  { return authService.renew(); }
}
```

---

## 2. Action Context

Every native→web trigger carries an `ActionContext`:

| Field | Type | Description |
|-------|------|-------------|
| `distributorId` | String | Unique ID of the distributing financial institution |
| `customerId` | String | End-user ID (never exposed to JS) |
| `objectId` | String | Policy or subscription identifier |
| `objectType` | `POLICY` / `SUBSCRIPTION` | Object being acted upon |
| `action` | `ISSUE` / `WITHDRAWAL` / `RESIGNATION` | Action to perform |
| `lang` | String | Language code, e.g. `"en"`, `"pl"` |
| `uiMode` | `light` / `dark` | Passed to web app for theming |
| `extraParams` | Map | Arbitrary additional parameters |

---

## 3. Communication Model

All bridge messages are JSON. Every message includes the session token.

### Web → Native
```json
{ "action": "returnResult", "token": "eyJ...", "params": { "result": "ISSUED" } }
```

### Native → Web (via JS event)
```json
{ "type": "initializeSession", "token": "eyJ...", "payload": { "objectType": "SUBSCRIPTION", "action": "ISSUE", ... } }
```

**Supported actions:** `initializeSession`, `executeAction`, `requestData`, `returnResult`, `refreshToken`, `navigateBack`, `close`

### Web-side usage
```ts
// Listen for SDK ready
window.addEventListener('wvb:ready', (e) => {
    console.log('Context:', e.detail);
});

// Listen for messages from native
window.addEventListener('wvb:message', (e) => {
    const { type, payload } = e.detail;
    if (type === 'initializeSession') initApp(payload);
});

// Send message to native
window.webviewbridge.send('returnResult', { result: 'ISSUED' });
```

---

## 4. Android Integration

### Build AAR
```bash
./gradlew :shared:assembleRelease
# Output: shared/build/outputs/aar/shared-release.aar
```

### Add to your app
```kotlin
// settings.gradle.kts
includeBuild("path/to/webviewbridge/shared")

// or add the AAR directly:
dependencies { implementation(files("libs/shared-release.aar")) }
```

### Integrate
```kotlin
val config = SdkConfig(
    baseUrl        = "https://my-webapp.example.com",
    allowedDomains = setOf("my-webapp.example.com"),
    enforceHttps   = true
)

val sdk = WebViewBridgeView(this, config, MyTokenProvider(), object : SdkEventListener {
    override fun onBridgeMessage(message: BridgeMessage) {
        when (message.action) {
            BridgeActions.REFRESH_TOKEN -> sdk.refreshToken()
            BridgeActions.RETURN_RESULT -> handleResult(message.params)
        }
    }
    override fun onSecurityAuditEvent(event: AuditEvent) { siem.log(event) }
    override fun onError(error: SdkError) { /* handle */ }
})

// Trigger an action
sdk.setActionContext(ActionContext(
    distributorId = "DIST-001", customerId = "CUST-123",
    objectId = "OBJ-001", objectType = ObjectType.SUBSCRIPTION.value,
    action = ActionType.ISSUE.value, lang = "en", uiMode = UIMode.LIGHT.value
))

// Dynamic action execution
sdk.executeAction("openProductDetails", mapOf("productId" to "12345"))
```

---

## 5. iOS Integration

### Build xcframework
```bash
# Requires JDK 17 and Xcode
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew :shared:assembleXCFramework
# Output: shared/build/XCFrameworks/release/WebViewBridge.xcframework
```

### Add to Xcode project
1. Drag `WebViewBridge.xcframework` into your Xcode project
2. Target → General → **Frameworks, Libraries, and Embedded Content** → **Embed & Sign**

### Integrate
```swift
import WebViewBridge

let bridge = WebViewBridgeController(
    config:        SdkConfig(baseUrl: "https://my-webapp.example.com",
                             allowedDomains: ["my-webapp.example.com"]),
    tokenProvider: MyTokenProvider(),
    eventListener: self
)
view.addSubview(bridge.webView)

bridge.setActionContext(ctx: ActionContext(
    distributorId: "DIST-001", customerId: "CUST-123",
    objectId: "OBJ-001", objectType: ObjectType.subscription.value,
    action: ActionType_.issue.value, lang: "en", uiMode: UIMode.light.value,
    extraParams: [:]
))
```

---

## 6. React App

```bash
cd react-app
npm install
npm run dev      # http://localhost:5173
npm run build    # production build → dist/
```

### Screen routing

| objectType + action | Screen |
|---|---|
| `SUBSCRIPTION` + `ISSUE` | Product catalogue iframe |
| `POLICY` + `WITHDRAWAL` | Informational + confirm page |
| `POLICY` + `RESIGNATION` | Two-step confirmation with typed confirmation |

---

## 7. Security

### URL validation (every navigation + sub-resource)
1. **Dangerous scheme block** — `javascript:` and `data:text/html` always blocked
2. **HTTPS enforcement** — `http://` navigation blocked when `enforceHttps = true`
3. **Sensitive param heuristic** — blocks URLs containing `token=`, `customerId=`, `jwt=`, etc.
4. **Domain allowlist** — all navigations validated against `allowedDomains`

### JS bridge protection
- Every inbound message token is compared to `TokenProvider.getToken()`
- Mismatches trigger `onSecurityAuditEvent` with `INVALID_MESSAGE_TOKEN`
- Raw token is never exposed to JS — only non-sensitive context fields

### Certificate pinning (Android)
```kotlin
val config = SdkConfig(
    baseUrl = "https://my-webapp.example.com",
    certificatePins = setOf("sha256/AAAA...BASE64==")
)
// OkHttpClientFactory.create(config, controller) for API calls
```

### Audit events
All blocked requests fire `SdkEventListener.onSecurityAuditEvent(event: AuditEvent)`.
Forward to your SIEM:
```kotlin
override fun onSecurityAuditEvent(event: AuditEvent) {
    siemClient.send(event.reason, event.blockedUrl, event.detail, event.timestampMs)
}
```

---

## 8. Build Requirements

| Tool | Version |
|------|---------|
| JDK | 17 (set via `jvmToolchain(17)`) |
| Kotlin | 2.1.21 |
| AGP | 8.7.3 |
| Gradle | 9.4 |
| Xcode | 15+ |
| Android minSdk | 24 |
| Android compileSdk | 34 |

### Resolve Java version issue
```bash
# If you have Java 25 as system JDK:
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew :shared:assembleXCFramework
```
