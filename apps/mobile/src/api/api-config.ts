import { ApiError } from './api-error';

/**
 * Read at call time rather than captured once, so a client created at module load still sees
 * the value Expo inlined for this bundle. The literal `process.env.EXPO_PUBLIC_API_BASE_URL`
 * must stay spelled out: Expo only inlines references it can see statically.
 */
export function readApiBaseUrlFromEnv(): string | undefined {
  return process.env.EXPO_PUBLIC_API_BASE_URL;
}

export function isApiBaseUrlConfigured(baseUrl: string | undefined): boolean {
  return Boolean(baseUrl?.trim());
}

/**
 * Returns the base URL without a trailing slash. The contract's `servers` entry is relative
 * (`/`), so every path is resolved against this value.
 */
export function resolveApiBaseUrl(baseUrl: string | undefined): string {
  const value = baseUrl?.trim();

  if (!value) {
    throw new ApiError({ kind: 'configuration', reason: 'missing' }, null);
  }

  let url: URL;

  try {
    url = new URL(value);
  } catch {
    throw new ApiError({ kind: 'configuration', reason: 'invalid' }, null);
  }

  if (!['http:', 'https:'].includes(url.protocol) || url.search || url.hash) {
    throw new ApiError({ kind: 'configuration', reason: 'invalid' }, null);
  }

  return url.toString().replace(/\/+$/, '');
}
