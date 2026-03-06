/**
 * WebViewBridge — Web Application Bridge
 * ===================================
 * Include this script in the web application that is loaded by WebViewBridge.
 * It provides a unified API for communicating with the native host on both
 * Android and iOS, and exposes session parameters injected by the SDK.
 *
 * The SDK automatically injects a minimal `window.webviewbridge` stub on every
 * page load. This file augments that stub with additional helpers and
 * documents the full contract.
 *
 * Usage:
 *   import './sdk-bridge.js';
 *
 *   // Wait for the SDK to be ready
 *   window.addEventListener('webviewbridge:ready', (e) => {
 *     console.log('Distributor:', e.detail.distributorId);
 *     console.log('UI mode:', e.detail.uiMode); // "dark" | "light"
 *     console.log('Language:', e.detail.language);
 *   });
 *
 *   // Send a message to the native app
 *   WebViewBridge.send('OPEN_POLICY_PDF', { policyId: 'POL-42' });
 *
 *   // Request token refresh
 *   WebViewBridge.send('TOKEN_REFRESH_REQUIRED');
 */

'use strict';

const WebViewBridge = (() => {
  // ---------------------------------------------------------------------------
  // Internal state
  // ---------------------------------------------------------------------------

  let _sessionParams = null;
  let _ready = false;
  const _pendingMessages = [];

  // ---------------------------------------------------------------------------
  // Core send function — routes to Android or iOS bridge
  // ---------------------------------------------------------------------------

  function send(action, params) {
    if (typeof action !== 'string' || action.length === 0) {
      console.error('[WebViewBridge] send() requires a non-empty action string.');
      return;
    }

    const payload = JSON.stringify({ action, params: params || {} });

    // iOS WKWebView
    if (window.webkit?.messageHandlers?.webviewbridge) {
      window.webkit.messageHandlers.webviewbridge.postMessage(payload);
      return;
    }

    // Android JavascriptInterface
    if (window.webviewbridgeNative?.postMessage) {
      window.webviewbridgeNative.postMessage(payload);
      return;
    }

    // Fallback: queue until bridge is available (handles race conditions)
    console.warn('[WebViewBridge] Bridge not yet available, queuing:', action);
    _pendingMessages.push({ action, params });
  }

  // Flush queued messages once the bridge is confirmed ready
  function flushPending() {
    while (_pendingMessages.length > 0) {
      const { action, params } = _pendingMessages.shift();
      send(action, params);
    }
  }

  // ---------------------------------------------------------------------------
  // Well-known action helpers
  // ---------------------------------------------------------------------------

  const actions = {
    /** Tell the native app to navigate back. */
    navigateBack: () => send('NAVIGATE_BACK'),

    /** Tell the native app to close / dismiss the SDK view. */
    close: () => send('CLOSE'),

    /** Signal that a new session token is needed. */
    requestTokenRefresh: () => send('TOKEN_REFRESH_REQUIRED'),

    /**
     * Open the native share sheet.
     * @param {string} text  Text or URL to share.
     */
    share: (text) => send('SHARE', { text }),

    /**
     * Report an internal web app error to the native layer.
     * @param {string} code     Machine-readable error code.
     * @param {string} message  Human-readable description.
     */
    reportError: (code, message) => send('ERROR_REPORT', { code, message }),

    /**
     * Request biometric re-authentication.
     */
    requestBiometricAuth: () => send('BIOMETRIC_AUTH'),
  };

  // ---------------------------------------------------------------------------
  // Session params accessor
  // ---------------------------------------------------------------------------

  function getSessionParams() {
    return _sessionParams
      ? Object.freeze({ ..._sessionParams })
      : null;
  }

  // ---------------------------------------------------------------------------
  // Internal event listeners
  // ---------------------------------------------------------------------------

  // The SDK dispatches 'webviewbridge:ready' with session params when the bridge is
  // initialised (on every page load / navigation).
  window.addEventListener('webviewbridge:ready', (e) => {
    _sessionParams = e.detail || {};
    _ready = true;
    flushPending();
  });

  // The SDK dispatches 'webviewbridge:message' when the native app sends data to the
  // web app (e.g. after a token refresh).
  window.addEventListener('webviewbridge:message', (e) => {
    const detail = e.detail || {};
    if (detail.type === 'SESSION_UPDATE') {
      _sessionParams = { ..._sessionParams, ...detail.payload };
    }
    if (detail.type === 'TOKEN_REFRESHED') {
      // Notify any listeners registered by the web app
      window.dispatchEvent(new CustomEvent('webviewbridge:tokenRefreshed'));
    }
  });

  // If the stub was already injected before this script loaded
  if (window.__webviewbridge_initialized && window.webviewbridge?.sessionParams) {
    _sessionParams = window.webviewbridge.sessionParams;
    _ready = true;
  }

  // ---------------------------------------------------------------------------
  // Public API
  // ---------------------------------------------------------------------------

  return {
    /** Send a message to the native host application. */
    send,

    /** Convenience helpers for common actions. */
    actions,

    /** Returns the current session parameters (read-only copy). */
    getSessionParams,

    /** True once the SDK bridge has been initialised. */
    get isReady() { return _ready; },
  };
})();

// Export for module environments
if (typeof module !== 'undefined') {
  module.exports = WebViewBridge;
}
