import { useQuery, type UseQueryResult } from '@tanstack/react-query';

import { isApiBaseUrlConfigured, useApiClient } from '@/api';

import { fetchHealth } from './health-client';
import type { HealthResponse } from './health-schema';

export const healthQueryKey = ['health'] as const;

export function useHealthQuery(baseUrl: string | undefined): UseQueryResult<HealthResponse, Error> {
  const client = useApiClient();

  return useQuery({
    queryKey: [...healthQueryKey, baseUrl ?? 'unconfigured'],
    queryFn: ({ signal }) => fetchHealth(client, signal),
    enabled: isApiBaseUrlConfigured(baseUrl),
  });
}
