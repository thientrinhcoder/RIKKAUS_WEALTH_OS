/**
 * The browser half of the Google handoff, written so that nothing touches web storage.
 *
 * `expo-web-browser` hands the result from the popup back to the app through localStorage, which
 * the project rule forbids for anything auth-related. Instead the sign-in screen opens the popup
 * itself and the `/auth/callback` page posts the redirect URL on a same-origin `BroadcastChannel`:
 * only documents of this origin can listen, nothing persists, and it works even when the Google
 * pages' opener policy severs `window.opener`. The callback page waits for an acknowledgement, so
 * a callback opened directly (no sign-in waiting) can say so instead of hanging.
 */
export const GOOGLE_CALLBACK_CHANNEL = 'rikkaus-google-callback';

interface CallbackMessage {
  kind: 'callback';
  url: string;
}

interface AckMessage {
  kind: 'ack';
}

const POPUP_NAME = 'rikkaus-google-sign-in';
const CLOSED_POLL_MS = 500;
/** A closed popup may have posted just before closing; wait this long before calling it cancelled. */
const CLOSED_GRACE_MS = 800;
const ACK_TIMEOUT_MS = 1500;

function popupFeatures(): string {
  const width = 480;
  const height = 640;
  const left = Math.max(0, Math.round(window.screenX + (window.outerWidth - width) / 2));
  const top = Math.max(0, Math.round(window.screenY + (window.outerHeight - height) / 2));

  return `popup=yes,width=${width},height=${height},left=${left},top=${top}`;
}

/**
 * Opens Google in a popup. Resolves with the callback URL, with `null` when the user closed the
 * window, or with `'blocked'` when the browser refused to open it. Must be called synchronously
 * from the click handler, or the popup blocker stops it.
 */
export function openGooglePopup(authUrl: string): Promise<string | null | 'blocked'> {
  if (typeof BroadcastChannel === 'undefined') return Promise.resolve('blocked');

  const popup = window.open(authUrl, POPUP_NAME, popupFeatures());

  if (!popup) return Promise.resolve('blocked');

  return new Promise((resolve) => {
    const channel = new BroadcastChannel(GOOGLE_CALLBACK_CHANNEL);
    let settled = false;
    let closedAt: number | null = null;

    function settle(value: string | null) {
      if (settled) return;
      settled = true;
      clearInterval(timer);
      channel.close();
      resolve(value);
    }

    channel.onmessage = (event: MessageEvent<CallbackMessage>) => {
      const data = event.data;

      if (data?.kind !== 'callback' || typeof data.url !== 'string') return;
      if (!data.url.startsWith(`${window.location.origin}/auth/callback`)) return;

      channel.postMessage({ kind: 'ack' } satisfies AckMessage);
      settle(data.url);
    };

    const timer = setInterval(() => {
      if (!popup.closed) return;

      closedAt ??= Date.now();
      if (Date.now() - closedAt >= CLOSED_GRACE_MS) settle(null);
    }, CLOSED_POLL_MS);
  });
}

/**
 * Run by `/auth/callback`. Resolves true when a waiting sign-in screen took the result (and then
 * closes the popup), false when nothing was waiting.
 */
export function handOffGoogleCallback(): Promise<boolean> {
  if (typeof window === 'undefined' || typeof BroadcastChannel === 'undefined') {
    return Promise.resolve(false);
  }

  return new Promise((resolve) => {
    const channel = new BroadcastChannel(GOOGLE_CALLBACK_CHANNEL);
    const timeout = setTimeout(() => {
      channel.close();
      resolve(false);
    }, ACK_TIMEOUT_MS);

    channel.onmessage = (event: MessageEvent<AckMessage>) => {
      if (event.data?.kind !== 'ack') return;

      clearTimeout(timeout);
      channel.close();
      resolve(true);
      window.close();
    };

    channel.postMessage({ kind: 'callback', url: window.location.href } satisfies CallbackMessage);
  });
}
