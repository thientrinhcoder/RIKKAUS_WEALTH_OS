/**
 * How a platform carries the refresh token. Web and native differ only here; everything else in
 * the session lifecycle is shared.
 */
export interface SessionTransport {
  mode: 'web' | 'native';
  /** Added to every auth-route request: the web header and cookie credentials, or nothing. */
  requestInit: { headers?: Record<string, string>; credentials?: RequestCredentials };
  /**
   * The stored refresh token, or null. The web client always answers `undefined`: the token is in
   * an HttpOnly cookie the page cannot read, and the browser attaches it itself.
   */
  readRefreshToken(): Promise<string | null | undefined>;
  saveRefreshToken(token: string | undefined): Promise<void>;
  clearRefreshToken(): Promise<void>;
  /**
   * Runs a renewal so that no two renewals overlap, even across browser tabs. The refresh token
   * is single-use, so two tabs renewing with the same cookie at once would make the server see a
   * reused token and revoke the whole session.
   */
  withRefreshLock<T>(task: () => Promise<T>): Promise<T>;
}
