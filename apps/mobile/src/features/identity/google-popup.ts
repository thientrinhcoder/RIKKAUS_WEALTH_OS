import * as WebBrowser from 'expo-web-browser';

/**
 * Native has no popup: `expo-auth-session` drives the system browser session and receives the
 * redirect itself, so these exist only to give the web module's callers one import on every
 * platform.
 */
export function openGooglePopup(_authUrl: string): Promise<string | null | 'blocked'> {
  return Promise.resolve('blocked');
}

export async function handOffGoogleCallback(): Promise<boolean> {
  return WebBrowser.maybeCompleteAuthSession().type !== 'failed';
}
