import { createContext, useContext, type PropsWithChildren } from 'react';

import { createApiClient, type ApiClient } from './api-client';
import { readApiBaseUrlFromEnv } from './api-config';
import { classifyApiError } from './classify-api-error';

/**
 * The client a screen gets when no provider supplies one: reads the base URL from the bundle's
 * environment on each request and has no auth hooks. The identity layer replaces it with a client
 * built with `auth` hooks through `ApiClientProvider`.
 */
const defaultApiClient = createApiClient({ baseUrl: readApiBaseUrlFromEnv });

const ApiClientContext = createContext<ApiClient>(defaultApiClient);

export function ApiClientProvider({ client, children }: PropsWithChildren<{ client: ApiClient }>) {
  return <ApiClientContext.Provider value={client}>{children}</ApiClientContext.Provider>;
}

export function useApiClient(): ApiClient {
  return useContext(ApiClientContext);
}

/** At most one automatic retry, and only for failures a retry can fix. */
export const MAX_AUTOMATIC_RETRIES = 1;

/**
 * TanStack Query `retry` callback. Network failures, `internal-error` and 5xx answers are retried
 * once; a 4xx problem, a schema mismatch or a configuration error never is, because resending the
 * same request cannot change the answer.
 */
export function shouldRetryApiError(failureCount: number, error: unknown): boolean {
  return failureCount < MAX_AUTOMATIC_RETRIES && classifyApiError(error).retryable;
}
