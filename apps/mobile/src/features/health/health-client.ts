import { HealthResponseSchema, type HealthResponse } from './health-schema';

export class HealthCheckError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'HealthCheckError';
  }
}

export function isApiBaseUrlConfigured(baseUrl: string | undefined): boolean {
  return Boolean(baseUrl?.trim());
}

export function normalizeApiBaseUrl(baseUrl: string | undefined): string {
  const value = baseUrl?.trim();

  if (!value) {
    throw new HealthCheckError('The API endpoint is not configured.');
  }

  let url: URL;

  try {
    url = new URL(value);
  } catch {
    throw new HealthCheckError('The API endpoint configuration is invalid.');
  }

  if (!['http:', 'https:'].includes(url.protocol) || url.search || url.hash) {
    throw new HealthCheckError('The API endpoint configuration is invalid.');
  }

  return url.toString().replace(/\/+$/, '');
}

export async function fetchHealth(
  baseUrl: string | undefined,
  signal?: AbortSignal,
): Promise<HealthResponse> {
  const healthUrl = `${normalizeApiBaseUrl(baseUrl)}/actuator/health`;
  let response: Response;

  try {
    response = await fetch(healthUrl, {
      headers: { Accept: 'application/json' },
      signal,
    });
  } catch (error) {
    if (signal?.aborted) {
      throw error;
    }

    throw new HealthCheckError('The API could not be reached.');
  }

  if (!response.ok) {
    throw new HealthCheckError(`The API returned HTTP ${response.status}.`);
  }

  let payload: unknown;

  try {
    payload = await response.json();
  } catch {
    throw new HealthCheckError('The API returned an invalid JSON response.');
  }

  const parsed = HealthResponseSchema.safeParse(payload);

  if (!parsed.success) {
    throw new HealthCheckError('The API reported an unhealthy or invalid status.');
  }

  return parsed.data;
}
