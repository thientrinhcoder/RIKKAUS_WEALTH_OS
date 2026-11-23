import type { ProblemDetail } from './problem';

/**
 * Every way a request can fail, as one discriminated union. Screens never inspect this directly;
 * they call `classifyApiError`, which turns it into Vietnamese copy and a recovery decision.
 *
 * - `configuration`: the base URL is missing or invalid; no request was sent.
 * - `network`: no HTTP response arrived (offline, DNS, refused, CORS rejection).
 * - `problem`: the server answered with an RFC 9457 body.
 * - `http`: a non-2xx answer without a readable problem body (e.g. Actuator's 503).
 * - `schema-mismatch`: a 2xx body that does not match the client's schema for it.
 */
export type ApiFailure =
  | { kind: 'configuration'; reason: 'missing' | 'invalid' }
  | { kind: 'network' }
  | { kind: 'problem'; status: number; problem: ProblemDetail }
  | { kind: 'http'; status: number }
  | { kind: 'schema-mismatch'; status: number; issuePaths: string[] };

export interface ApiRequestDescription {
  method: string;
  path: string;
}

function describe(failure: ApiFailure, request: ApiRequestDescription | null): string {
  const target = request ? `${request.method} ${request.path}` : 'API request';

  switch (failure.kind) {
    case 'configuration':
      return `${target}: API base URL is ${failure.reason}`;
    case 'network':
      return `${target}: no response`;
    case 'problem':
      return `${target}: ${failure.problem.type} (${failure.status})`;
    case 'http':
      return `${target}: HTTP ${failure.status} without a problem body`;
    case 'schema-mismatch':
      return `${target}: response does not match its schema at ${failure.issuePaths.join(', ')}`;
  }
}

/**
 * The message is for logs and developer tools only, and deliberately never includes the
 * server's `detail`: user copy comes from the classifier.
 */
export class ApiError extends Error {
  readonly failure: ApiFailure;
  /** The `X-Correlation-Id` the server answered with, or the body's `correlationId`. */
  readonly correlationId: string | null;
  readonly request: ApiRequestDescription | null;

  constructor(
    failure: ApiFailure,
    correlationId: string | null,
    request: ApiRequestDescription | null = null,
    options?: { cause?: unknown },
  ) {
    super(describe(failure, request), options);
    this.name = 'ApiError';
    this.failure = failure;
    this.correlationId = correlationId;
    this.request = request;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}
