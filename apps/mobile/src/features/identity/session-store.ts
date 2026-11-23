import {
  ApiError,
  createApiClient,
  isApiError,
  resolveApiBaseUrl,
  type ApiClient,
  type UnauthorizedContext,
  type UnauthorizedDecision,
} from '@/api';
import { SessionResponseSchema, type SessionResponse, type SessionUser } from './session-schema';
import type { SessionTransport } from './session-transport-types';

/**
 * - `restoring`: the app just started and is asking the server whether a session survives.
 * - `signed-out`: there is no session; protected routes redirect to sign-in.
 * - `signed-in`: an access token is held in memory.
 * - `ended`: a session existed and the server refused to renew it. Protected content is replaced,
 *   never overlaid, by the expired or revoked screen.
 * - `unreachable`: the restore could not reach the server, so whether a session exists is unknown.
 */
export type SessionStatus = 'restoring' | 'signed-out' | 'signed-in' | 'ended' | 'unreachable';

/**
 * The server answers a refused renewal with the same 401 whatever the cause, so the reason is
 * inferred from when it happened. Refused while the app was closed (on restore): the session ran
 * out, `expired`. Refused while the app was in use: it was ended under the user, `revoked`, which
 * includes reuse of a retired refresh token and a sign-out from elsewhere.
 */
export type SessionEndReason = 'expired' | 'revoked';

export interface SessionSnapshot {
  status: SessionStatus;
  user: SessionUser | null;
  endReason: SessionEndReason | null;
  /** Why the restore could not complete, for the `unreachable` screen. */
  restoreError: ApiError | null;
}

export interface GoogleSignInInput {
  authorizationCode: string;
  codeVerifier: string;
  redirectUri: string;
}

export interface DevSignInInput {
  email: string;
  displayName?: string;
}

type RenewalOutcome =
  | { kind: 'renewed' }
  /** The server refused the refresh token: 401, or 400 when none was sent. */
  | { kind: 'refused'; status: number }
  | { kind: 'failed'; error: unknown };

export interface SessionStore {
  getSnapshot(): SessionSnapshot;
  subscribe(listener: () => void): () => void;
  /** The client every screen uses: it attaches the access token and renews once on a 401. */
  apiClient: ApiClient;
  restore(): Promise<void>;
  signInWithGoogle(input: GoogleSignInInput): Promise<void>;
  devSignIn(input: DevSignInInput): Promise<void>;
  /** Throws when the server could not be told, so the caller can keep the session and say so. */
  signOut(): Promise<void>;
  /** Leaves the expired/revoked screen for the sign-in screen. */
  acknowledgeEnded(): void;
}

interface SessionStoreOptions {
  baseUrl: () => string | undefined;
  transport: SessionTransport;
  fetchImpl?: typeof fetch;
}

const UNAUTHORIZED = 'urn:rikkaus:problem:unauthorized';

function problemStatus(error: unknown): number | null {
  if (!isApiError(error)) return null;

  return error.failure.kind === 'problem' || error.failure.kind === 'http'
    ? error.failure.status
    : null;
}

function isRefusal(error: unknown): boolean {
  if (!isApiError(error) || error.failure.kind !== 'problem') return false;

  const { status, problem } = error.failure;

  /** 400 means no refresh token was presented at all: there is simply no session. */
  return problem.type === UNAUTHORIZED || status === 400 || status === 401;
}

export function createSessionStore({
  baseUrl,
  transport,
  fetchImpl,
}: SessionStoreOptions): SessionStore {
  const listeners = new Set<() => void>();
  let snapshot: SessionSnapshot = {
    status: 'restoring',
    user: null,
    endReason: null,
    restoreError: null,
  };
  /** Memory only. Never written to any storage, on any platform. */
  let accessToken: string | null = null;
  let renewal: Promise<RenewalOutcome> | null = null;
  let restoring: Promise<void> | null = null;

  /** Auth routes are called without a bearer token and with the platform's transport. */
  const authClient = createApiClient({ baseUrl, fetchImpl });

  function setSnapshot(next: Partial<SessionSnapshot>) {
    snapshot = { ...snapshot, ...next };
    listeners.forEach((listener) => listener());
  }

  async function accept(session: SessionResponse) {
    accessToken = session.accessToken;
    await transport.saveRefreshToken(session.refreshToken);
    setSnapshot({ status: 'signed-in', user: session.user, endReason: null, restoreError: null });
  }

  async function clearLocal() {
    accessToken = null;
    await transport.clearRefreshToken();
  }

  async function end(reason: SessionEndReason) {
    await clearLocal();
    setSnapshot({ status: 'ended', user: null, endReason: reason, restoreError: null });
  }

  /** One `POST /auth/refresh`, serialised across tabs by the transport's lock. */
  function renewOnce(): Promise<RenewalOutcome> {
    return transport.withRefreshLock(async () => {
      try {
        const stored = await transport.readRefreshToken();

        if (transport.mode === 'native' && !stored) return { kind: 'refused', status: 400 };

        const { data } = await authClient.request('post', '/api/v1/auth/refresh', {
          auth: false,
          body: stored ? { refreshToken: stored } : {},
          schema: SessionResponseSchema,
          ...transport.requestInit,
        });

        await accept(data);

        return { kind: 'renewed' };
      } catch (error) {
        if (isRefusal(error)) return { kind: 'refused', status: problemStatus(error) ?? 401 };

        return { kind: 'failed', error };
      }
    });
  }

  /**
   * Single flight: every caller that needs a renewal while one is running awaits the same
   * promise, so concurrent 401s produce exactly one `POST /auth/refresh`.
   */
  function renew(): Promise<RenewalOutcome> {
    if (!renewal) {
      renewal = renewOnce().finally(() => {
        renewal = null;
      });
    }

    return renewal;
  }

  async function handleUnauthorized(
    _error: ApiError,
    { accessToken: rejected }: UnauthorizedContext,
  ): Promise<UnauthorizedDecision> {
    if (snapshot.status !== 'signed-in') return 'fail';

    /** Another request already renewed after this one was sent: retry with the new token. */
    if (rejected && accessToken && rejected !== accessToken) return 'retry';

    const outcome = await renew();

    if (outcome.kind === 'renewed') return 'retry';

    if (outcome.kind === 'refused') {
      await end('revoked');

      return 'fail';
    }

    /**
     * The renewal itself could not reach the server. The session may still be fine, so it is
     * kept, and the caller sees the network failure rather than a misleading "expired".
     */
    throw outcome.error;
  }

  const apiClient = createApiClient({
    baseUrl,
    fetchImpl,
    auth: { getAccessToken: () => accessToken, onUnauthorized: handleUnauthorized },
  });

  async function restore() {
    if (restoring) return restoring;

    restoring = (async () => {
      setSnapshot({ status: 'restoring', restoreError: null });

      const outcome = await renew();

      if (outcome.kind === 'renewed') return;

      if (outcome.kind === 'refused') {
        /** No token at all is a plain signed-out state; a refused one is an expired session. */
        if (outcome.status === 400) {
          await clearLocal();
          setSnapshot({ status: 'signed-out', user: null, endReason: null });
        } else {
          await end('expired');
        }

        return;
      }

      setSnapshot({
        status: 'unreachable',
        restoreError: isApiError(outcome.error)
          ? outcome.error
          : new ApiError({ kind: 'network' }, null),
      });
    })().finally(() => {
      restoring = null;
    });

    return restoring;
  }

  async function signInWithGoogle(input: GoogleSignInInput) {
    const { data } = await authClient.request('post', '/api/v1/auth/google', {
      auth: false,
      body: input,
      schema: SessionResponseSchema,
      ...transport.requestInit,
    });

    await accept(data);
  }

  /**
   * `POST /api/v1/auth/dev-sign-in` exists only under the API's `local` profile and is outside
   * the published contract, so the typed client cannot name it. Failures are still raised as
   * `ApiError` so the screen classifies them like any other request.
   */
  async function devSignIn(input: DevSignInInput) {
    const description = { method: 'POST', path: '/api/v1/auth/dev-sign-in' };
    const url = `${resolveApiBaseUrl(baseUrl())}${description.path}`;
    let response: Response;

    try {
      response = await (fetchImpl ?? globalThis.fetch)(url, {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          ...transport.requestInit.headers,
        },
        ...(transport.requestInit.credentials
          ? { credentials: transport.requestInit.credentials }
          : {}),
        body: JSON.stringify(input),
      });
    } catch (error) {
      throw new ApiError({ kind: 'network' }, null, description, { cause: error });
    }

    if (!response.ok) {
      throw new ApiError({ kind: 'http', status: response.status }, null, description);
    }

    const parsed = SessionResponseSchema.safeParse(await response.json().catch(() => null));

    if (!parsed.success) {
      throw new ApiError(
        { kind: 'schema-mismatch', status: response.status, issuePaths: ['(root)'] },
        null,
        description,
      );
    }

    await accept(parsed.data);
  }

  async function signOut() {
    const stored = await transport.readRefreshToken();

    try {
      await authClient.request('post', '/api/v1/auth/logout', {
        auth: false,
        body: stored ? { refreshToken: stored } : {},
        ...transport.requestInit,
      });
    } catch (error) {
      /**
       * A problem answer (such as 400 when the cookie is already gone) means the server holds no
       * session for this device either, so signing out locally is correct. Only a failure to
       * reach the server is surfaced: on web the cookie would otherwise survive and silently
       * restore the session on the next load.
       */
      if (!isApiError(error) || error.failure.kind !== 'problem') throw error;
    }

    await clearLocal();
    setSnapshot({ status: 'signed-out', user: null, endReason: null });
  }

  function acknowledgeEnded() {
    if (snapshot.status === 'ended') {
      setSnapshot({ status: 'signed-out', endReason: null });
    }
  }

  return {
    getSnapshot: () => snapshot,
    subscribe(listener) {
      listeners.add(listener);

      return () => listeners.delete(listener);
    },
    apiClient,
    restore,
    signInWithGoogle,
    devSignIn,
    signOut,
    acknowledgeEnded,
  };
}
