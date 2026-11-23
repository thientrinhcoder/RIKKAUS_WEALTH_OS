import { useQueryClient } from '@tanstack/react-query';
import {
  createContext,
  useContext,
  useEffect,
  useState,
  useSyncExternalStore,
  type PropsWithChildren,
} from 'react';
import { Platform } from 'react-native';

import { ApiClientProvider, readApiBaseUrlFromEnv } from '@/api';
import { createSessionStore, type SessionSnapshot, type SessionStore } from './session-store';
import { sessionTransport } from './session-transport';

const SessionContext = createContext<SessionStore | null>(null);

/**
 * The Google window lands on `/auth/callback`, a second copy of the app that only hands the
 * result back to the window that opened it. Restoring there would spend the refresh cookie a
 * second time for nothing, so that page skips the restore.
 */
function isOAuthCallbackPage(): boolean {
  return Platform.OS === 'web' && typeof window !== 'undefined'
    ? window.location.pathname.startsWith('/auth/callback')
    : false;
}

export function SessionProvider({ children }: PropsWithChildren) {
  const queryClient = useQueryClient();
  const [store] = useState(() =>
    createSessionStore({ baseUrl: readApiBaseUrlFromEnv, transport: sessionTransport }),
  );

  useEffect(() => {
    if (!isOAuthCallbackPage()) void store.restore();
  }, [store]);

  /**
   * Whenever the session stops being live, cached server data goes with it, so nothing a
   * previous session read can reappear on screen or stay in memory for the next user.
   */
  useEffect(() => {
    let wasSignedIn = store.getSnapshot().status === 'signed-in';

    return store.subscribe(() => {
      const signedIn = store.getSnapshot().status === 'signed-in';

      if (wasSignedIn && !signedIn) queryClient.clear();
      wasSignedIn = signedIn;
    });
  }, [store, queryClient]);

  return (
    <SessionContext.Provider value={store}>
      <ApiClientProvider client={store.apiClient}>{children}</ApiClientProvider>
    </SessionContext.Provider>
  );
}

export function useSessionStore(): SessionStore {
  const store = useContext(SessionContext);

  if (!store) throw new Error('useSessionStore must be used inside SessionProvider');

  return store;
}

export function useSession(): SessionSnapshot {
  const store = useSessionStore();

  return useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
}
