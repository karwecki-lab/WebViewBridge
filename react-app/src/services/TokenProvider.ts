/**
 * Web-side TokenProvider — mirrors the native SDK interface.
 *
 * Replace with your real auth integration:
 * ```ts
 * class MyTokenProvider extends TokenProvider {
 *   async getToken()     { return authService.currentToken(); }
 *   async refreshToken() { return authService.renew(); }
 * }
 * export const tokenProvider = new MyTokenProvider();
 * ```
 */
export class TokenProvider {
  private _token = 'eyJhbGciOiJIUzI1NiJ9.web_initial_token';

  async getToken(): Promise<string>     { return this._token; }
  async refreshToken(): Promise<string> {
    this._token = `eyJhbGciOiJIUzI1NiJ9.web_refreshed_${Date.now()}`;
    console.log('[TokenProvider] Token refreshed');
    return this._token;
  }
  /** Synchronous accessor for bridge message construction. */
  current(): string { return this._token; }

  /** Called by SdkBridge when native layer pushes a new token. */
  _set(t: string) { this._token = t; }
}

export const tokenProvider = new TokenProvider();
