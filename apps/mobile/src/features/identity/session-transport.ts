import * as SecureStore from 'expo-secure-store';

import type { SessionTransport } from './session-transport-types';

/**
 * Native (iOS/Android) transport. The refresh token travels in the request and response bodies
 * and is kept in the platform keystore through `expo-secure-store`; the access token stays in
 * memory. No `X-Rikkaus-Client` header is sent, which is what makes a request native.
 */
const REFRESH_TOKEN_KEY = 'rikkaus.refresh-token';

export const sessionTransport: SessionTransport = {
  mode: 'native',
  requestInit: {},
  async readRefreshToken() {
    return SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
  },
  async saveRefreshToken(token) {
    if (token) await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, token);
  },
  async clearRefreshToken() {
    await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
  },
  withRefreshLock(task) {
    return task();
  },
};
