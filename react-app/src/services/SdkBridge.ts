import type { ActionContext, BridgeMessage } from '../types';
import { tokenProvider } from './TokenProvider';

type Listener = (ctx: ActionContext) => void;

/**
 * Web-side SDK bridge.
 *
 * - Listens for messages from the native layer (wvb:ready, wvb:message)
 * - Sends messages to native via the injected window.webviewbridge or fallback postMessage
 * - Notifies React components when context changes via subscribe()
 */
class SdkBridge {
  private _ctx: ActionContext | null = null;
  private _listeners: Listener[] = [];

  constructor() {
    window.addEventListener('wvb:ready', (e: Event) => {
      const d = (e as CustomEvent<ActionContext>).detail;
      if (d) this._updateContext(d);
    });

    window.addEventListener('wvb:message', (e: Event) => {
      const msg = (e as CustomEvent).detail as { type: string; token?: string; payload?: ActionContext };
      if (msg.type === 'initializeSession' && msg.payload) {
        this._updateContext(msg.payload as unknown as ActionContext);
      }
      if (msg.type === 'refreshToken' && msg.token) {
        tokenProvider._set(msg.token);
      }
    });
  }

  // ── Subscribe to context changes ───────────────────────────

  subscribe(fn: Listener): () => void {
    this._listeners.push(fn);
    if (this._ctx) fn(this._ctx);
    return () => { this._listeners = this._listeners.filter(l => l !== fn); };
  }

  getContext(): ActionContext | null { return this._ctx; }

  // ── Send a message to native ───────────────────────────────

  send(action: string, params: Record<string, string> = {}) {
    const msg: BridgeMessage = { action, token: tokenProvider.current(), params };
    const raw = JSON.stringify(msg);
    const w = window as Record<string, any>;

    if (w?.webkit?.messageHandlers?.webviewbridge) {
      w.webkit.messageHandlers.webviewbridge.postMessage(raw);
    } else if (typeof w?.webviewbridgeNative?.postMessage === 'function') {
      w.webviewbridgeNative.postMessage(raw);
    } else {
      // Dev mode: log to console instead of crashing
      console.log('[SdkBridge DEV]', msg);
    }
  }

  // ── Convenience actions ────────────────────────────────────

  navigateBack()  { this.send('navigateBack'); }
  close()         { this.send('close'); }
  requestRefresh(){ this.send('refreshToken'); }
  returnResult(result: string, extra: Record<string, string> = {}) {
    this.send('returnResult', { result, ...extra });
  }

  executeAction(name: string, params: Record<string, string> = {}) {
    this.send('executeAction', { actionName: name, ...params });
  }

  // ── Internal ───────────────────────────────────────────────

  /** Allows dev/simulator mode to inject a context directly. */
  _simulateContext(ctx: ActionContext) { this._updateContext(ctx); }

  private _updateContext(ctx: ActionContext) {
    this._ctx = ctx;
    this._listeners.forEach(fn => fn(ctx));
  }
}

export const sdkBridge = new SdkBridge();
