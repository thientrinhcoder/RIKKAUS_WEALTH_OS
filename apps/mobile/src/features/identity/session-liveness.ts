import { useQuery } from '@tanstack/react-query';

import { useApiClient } from '@/api';
import { SessionUserSchema } from './session-schema';

/**
 * `GET /api/v1/auth/session` while the shell is open. It refetches when the window regains focus,
 * so a session revoked elsewhere is noticed on return rather than on the next save, and an
 * expired access token is renewed through the client's single-flight 401 handling.
 */
export function useSessionLiveness() {
  const client = useApiClient();

  return useQuery({
    queryKey: ['identity', 'session'],
    queryFn: async ({ signal }) =>
      (await client.request('get', '/api/v1/auth/session', { schema: SessionUserSchema, signal }))
        .data,
    retry: false,
  });
}
