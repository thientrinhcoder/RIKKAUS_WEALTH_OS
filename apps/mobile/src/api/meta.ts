import { z } from 'zod';

import type { ApiClient, ApiResponse } from './api-client';

/**
 * `GET /api/v1/meta`, the one public route in the contract. The contract marks every field
 * optional; the client requires them because the server always sends them, and a stricter schema
 * is still assignable to the contract type. `serverTime` is an ISO-8601 string ending in `Z`.
 */
export const MetaResponseSchema = z.object({
  application: z.string(),
  apiVersion: z.string(),
  buildVersion: z.string(),
  serverTime: z.iso.datetime(),
});

export type MetaResponse = z.infer<typeof MetaResponseSchema>;

export function fetchMeta(
  client: ApiClient,
  signal?: AbortSignal,
): Promise<ApiResponse<MetaResponse>> {
  return client.request('get', '/api/v1/meta', {
    schema: MetaResponseSchema,
    auth: false,
    signal,
  });
}
