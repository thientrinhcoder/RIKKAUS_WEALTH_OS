import type { SessionTransport } from './session-transport-types';

/**
 * Browser transport. The server sets the refresh token as the HttpOnly cookie `rikkaus_refresh`
 * when it sees `X-Rikkaus-Client: web`, so the page never reads, writes or stores any token:
 * nothing goes to localStorage, sessionStorage or IndexedDB. `credentials: 'include'` lets the
 * browser send and accept that cookie on the cross-port auth routes.
 */
const REFRESH_LOCK = 'rikkaus-session-refresh';

interface LockManagerLike {
  request<T>(name: string, callback: () => Promise<T>): Promise<T>;
}

function lockManager(): LockManagerLike | undefined {
  if (typeof navigator === 'undefined') return undefined;

  return (navigator as Navigator & { locks?: LockManagerLike }).locks;
}

export const sessionTransport: SessionTransport = {
  mode: 'web',
  requestInit: { headers: { 'X-Rikkaus-Client': 'web' }, credentials: 'include' },
  async readRefreshToken() {
    return undefined;
  },
  async saveRefreshToken() {},
  async clearRefreshToken() {},
  withRefreshLock(task) {
    /**
     * Web Locks queue renewals from every tab of this origin. The second tab's request then
     * leaves after the first tab's response replaced the cookie, so it presents the new value.
     */
    const locks = lockManager();

    return locks ? locks.request(REFRESH_LOCK, task) : task();
  },
};
