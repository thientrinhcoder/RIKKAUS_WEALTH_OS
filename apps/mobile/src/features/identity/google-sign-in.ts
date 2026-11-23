import {
  makeRedirectUri,
  ResponseType,
  useAuthRequest,
  type AuthRequestConfig,
  type AuthSessionResult,
} from 'expo-auth-session';
import { Platform } from 'react-native';

import { openGooglePopup } from './google-popup';
import type { GoogleSignInInput } from './session-store';

/**
 * Google's authorization endpoint. Only the authorization step happens on the client: the code
 * and PKCE verifier go to `POST /api/v1/auth/google`, which redeems them server-side, so no Google
 * token or client secret ever reaches the app.
 */
const GOOGLE_DISCOVERY = { authorizationEndpoint: 'https://accounts.google.com/o/oauth2/v2/auth' };

/** The minimal identity scopes from the accepted design; no other Google API is requested. */
const GOOGLE_SCOPES = ['openid', 'email', 'profile'];

/**
 * One OAuth client per platform, as Google requires. Each `process.env.EXPO_PUBLIC_*` reference
 * stays spelled out so Expo can inline it into the bundle.
 */
export function readGoogleClientId(): string | undefined {
  const value = Platform.select({
    web: process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID,
    ios: process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID,
    android: process.env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID,
  });

  return value?.trim() || undefined;
}

/**
 * On web this is `<origin>/auth/callback` (locally `http://localhost:8081/auth/callback`), which
 * must also be in the API's `GOOGLE_OAUTH_REDIRECT_URIS` allowlist.
 */
export function googleRedirectUri(): string {
  return makeRedirectUri({ scheme: 'rikkaus-wealth-os', path: 'auth/callback' });
}

export type GoogleOutcome =
  | { kind: 'code'; input: GoogleSignInInput }
  /** The user closed the Google window without choosing. */
  | { kind: 'cancelled' }
  /** Google refused to share the identity Rikkaus needs (`access_denied`). */
  | { kind: 'denied' }
  /** Anything else: a mismatched state, a malformed answer, an unknown Google error. */
  | { kind: 'failed' };

export interface GoogleSignIn {
  configured: boolean;
  /** False until the PKCE challenge has been generated. */
  ready: boolean;
  prompt(): Promise<GoogleOutcome>;
}

export function useGoogleSignIn(): GoogleSignIn {
  const clientId = readGoogleClientId();
  const redirectUri = googleRedirectUri();
  const config: AuthRequestConfig = {
    clientId: clientId ?? '',
    redirectUri,
    scopes: GOOGLE_SCOPES,
    responseType: ResponseType.Code,
    usePKCE: true,
    extraParams: { prompt: 'select_account' },
  };
  const [request, , promptAsync] = useAuthRequest(config, GOOGLE_DISCOVERY);

  /**
   * On web the popup is opened here rather than by `promptAsync`, whose handoff goes through
   * localStorage (see google-popup.web.ts). The request still builds the URL and checks `state`.
   */
  async function authorize(): Promise<AuthSessionResult | null> {
    if (!request) return null;

    if (Platform.OS !== 'web') return promptAsync();

    if (!request.url) return null;

    const returned = await openGooglePopup(request.url);

    if (returned === 'blocked') return null;
    if (returned === null) return { type: 'dismiss' };

    return request.parseReturnUrl(returned);
  }

  async function prompt(): Promise<GoogleOutcome> {
    if (!clientId || !request) return { kind: 'failed' };

    const result = await authorize();

    if (!result) return { kind: 'failed' };

    switch (result.type) {
      case 'success': {
        const code = result.params.code;

        return code && request.codeVerifier
          ? {
              kind: 'code',
              input: { authorizationCode: code, codeVerifier: request.codeVerifier, redirectUri },
            }
          : { kind: 'failed' };
      }
      case 'error':
        return result.params.error === 'access_denied' ? { kind: 'denied' } : { kind: 'failed' };
      case 'dismiss':
      case 'cancel':
        return { kind: 'cancelled' };
      default:
        return { kind: 'failed' };
    }
  }

  return { configured: Boolean(clientId), ready: Boolean(clientId && request), prompt };
}
