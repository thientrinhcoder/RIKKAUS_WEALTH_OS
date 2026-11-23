import type { ApiClient } from '@/api';

import { HealthResponseSchema, type HealthResponse } from './health-schema';

/**
 * `/actuator/health` sits outside the published `/api/v1` contract, so it goes through the
 * client's operational route: same base URL, error classification and schema validation, but no
 * token and no custom header, which that endpoint's CORS mapping would reject.
 */
export async function fetchHealth(
  client: ApiClient,
  signal?: AbortSignal,
): Promise<HealthResponse> {
  const { data } = await client.getOperational('/actuator/health', {
    schema: HealthResponseSchema,
    signal,
  });

  return data;
}
