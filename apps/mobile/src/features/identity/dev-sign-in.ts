/**
 * The local development sign-in is offered only when both hold:
 *
 * - `__DEV__`: a development bundle. `expo export` builds with `__DEV__` false, so the minifier
 *   drops the section and the `dev-sign-in` path from every release build.
 * - `EXPO_PUBLIC_DEV_SIGN_IN=local`: the developer states the API runs under its `local`
 *   profile, the only profile where `POST /api/v1/auth/dev-sign-in` exists.
 */
export const DEV_SIGN_IN_ENABLED = __DEV__ && process.env.EXPO_PUBLIC_DEV_SIGN_IN === 'local';

export const DEV_SIGN_IN_DEFAULTS = {
  email: 'an.nguyen@example.com',
  displayName: 'An Nguyễn',
} as const;
