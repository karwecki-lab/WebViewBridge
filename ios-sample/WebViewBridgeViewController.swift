// WebViewBridgeViewController.swift
// iOS sample — three action buttons + WebView
//
// Requires: WebViewBridge.xcframework (built via ./gradlew :shared:assembleXCFramework)
// Add framework to target → General → Frameworks, Libraries, and Embedded Content

import UIKit
import WebKit
import WebViewBridge   // KMP xcframework

// MARK: - View Controller

class WebViewBridgeViewController: UIViewController {

    private var bridge: WebViewBridgeController!

    // MARK: Preset action contexts

    private var subscriptionIssue: ActionContext { .init(
        distributorId: "DIST-001", customerId: "CUST-12345",
        objectId: "OBJ-001", objectType: ObjectType.subscription.value,
        action: ActionType_.issue.value,       lang: "en",
        uiMode: UIMode.light.value, extraParams: [:]
    )}
    private var policyWithdrawal: ActionContext { .init(
        distributorId: "DIST-001", customerId: "CUST-12345",
        objectId: "POL-999", objectType: ObjectType.policy.value,
        action: ActionType_.withdrawal.value,  lang: "en",
        uiMode: UIMode.light.value, extraParams: [:]
    )}
    private var policyResignation: ActionContext { .init(
        distributorId: "DIST-001", customerId: "CUST-12345",
        objectId: "POL-999", objectType: ObjectType.policy.value,
        action: ActionType_.resignation.value, lang: "en",
        uiMode: UIMode.light.value, extraParams: [:]
    )}

    // MARK: Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground

        let sdkConfig = SdkConfig(
            baseUrl:           "https://nnw-frontend.int.npr.dbyc.cardif.io",
            allowedDomains:    ["nnw-frontend.int.npr.dbyc.cardif.io", "cardif.io"],
            certificatePins:   [],
            extraHeaders:      [:],
            enforceHttps:      true,
            jsHandlerName:     "webviewbridge",
            messageValidation: true
        )
        bridge = WebViewBridgeController(
            config:        sdkConfig,
            tokenProvider: StubTokenProvider(),
            eventListener: self
        )
        buildLayout()
        bridge.setActionContext(ctx: subscriptionIssue)   // default
    }

    // MARK: Layout — [Btn1][Btn2][Btn3] / [WebView]

    private func buildLayout() {
        let btn1 = actionButton("Subscribe\n+ Issue")     { [weak self] in
            guard let s = self else { return }
            s.bridge.setActionContext(ctx: s.subscriptionIssue)
        }
        let btn2 = actionButton("Policy\n+ Withdrawal")   { [weak self] in
            guard let s = self else { return }
            s.bridge.setActionContext(ctx: s.policyWithdrawal)
        }
        let btn3 = actionButton("Policy\n+ Resignation")  { [weak self] in
            guard let s = self else { return }
            s.bridge.setActionContext(ctx: s.policyResignation)
        }

        let stack = UIStackView(arrangedSubviews: [btn1, btn2, btn3])
        stack.axis         = .horizontal
        stack.distribution = .fillEqually
        stack.spacing      = 8
        stack.translatesAutoresizingMaskIntoConstraints = false

        let wv = bridge.webView
        wv.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(stack)
        view.addSubview(wv)

        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 8),
            stack.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 8),
            stack.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -8),
            stack.heightAnchor.constraint(equalToConstant: 56),

            wv.topAnchor.constraint(equalTo: stack.bottomAnchor, constant: 8),
            wv.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            wv.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            wv.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }

    private func actionButton(_ title: String, handler: @escaping () -> Void) -> UIButton {
        let b = UIButton(type: .system)
        b.setTitle(title, for: .normal)
        b.titleLabel?.numberOfLines = 2
        b.titleLabel?.textAlignment = .center
        b.titleLabel?.font = .systemFont(ofSize: 12, weight: .semibold)
        b.backgroundColor = .systemBlue
        b.setTitleColor(.white, for: .normal)
        b.layer.cornerRadius = 8
        b.addAction(UIAction { _ in handler() }, for: .touchUpInside)
        return b
    }
}

// MARK: - SdkEventListener

extension WebViewBridgeViewController: SdkEventListener {
    func onBridgeMessage(message: BridgeMessage) {
        print("[WVB] Bridge: \(message.toSafeLog())")
        switch message.action {
        case BridgeActions.companion.NAVIGATE_BACK:
            navigationController?.popViewController(animated: true)
        case BridgeActions.companion.CLOSE:
            dismiss(animated: true)
        case BridgeActions.companion.REFRESH_TOKEN:
            bridge.refreshToken()
        case BridgeActions.companion.RETURN_RESULT:
            let result = message.params["result"] ?? "—"
            let alert = UIAlertController(title: "Result", message: result, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default))
            present(alert, animated: true)
        default:
            print("[WVB] Unhandled action: \(message.action)")
        }
    }

    func onSecurityAuditEvent(event: AuditEvent) {
        print("[WVB AUDIT] \(event.toLog())")
        // TODO: forward to SIEM
    }

    func onError(error: SdkError) {
        print("[WVB ERROR] \(error)")
    }

    func onPageLoaded(url: String)  { print("[WVB] Loaded:  \(url)") }
    func onPageStarted(url: String) { print("[WVB] Started: \(url)") }
}

// MARK: - Stub TokenProvider

/// Replace with your real auth service.
private class StubTokenProvider: TokenProvider {
    private var token = "eyJhbGciOiJIUzI1NiJ9.REPLACE_WITH_REAL_TOKEN"
    func getToken() -> String { token }
    func refreshToken() -> String {
        token = "eyJhbGciOiJIUzI1NiJ9.refreshed_\(Date().timeIntervalSince1970)"
        return token
    }
}
