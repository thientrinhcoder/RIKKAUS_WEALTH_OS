import { useQuery, type UseQueryResult } from '@tanstack/react-query';

import { fetchHealth, isApiBaseUrlConfigured } from './health-client';
import type { HealthResponse } from './health-schema';

export const healthQueryKey = ['health'] as const;

export function useHealthQuery(
  baseUrl: string | undefined,
): UseQueryResult<HealthResponse, Error> {
  return useQuery({
    queryKey: [...healthQueryKey, baseUrl ?? 'unconfigured'],
    queryFn: ({ signal }) => fetchHealth(baseUrl, signal),
    enabled: isApiBaseUrlConfigured(baseUrl),
  });
}
