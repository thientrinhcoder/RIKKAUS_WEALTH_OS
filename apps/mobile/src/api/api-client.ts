import type { z } from 'zod';

import { resolveApiBaseUrl } from './api-config';
import { ApiError, type ApiRequestDescription } from './api-error';
import type {
  ApiOperation,
  HttpMethod,
  PathParamsOf,
  ContractResponse,
  PathWithMethod,
  QueryParamsOf,
  RequestBodyOf,
  SuccessBodyOf,
} from './contract-types';
import { CORRELATION_HEADER, createCorrelationId } from './correlation';
import { isProblemContentType, ProblemDetailSchema } from './problem';

/**
 * The seam the identity layer plugs into. This module stores no token and knows nothing about
 * sign-in: it asks for a token before each authenticated request and reports a 401 once.
 */
export interface ApiAuthHooks {
  /** Return the current access token, or nothing to send the request anonymously. */
  getAccessToken: () => string | null | undefined | Promise<string | null | undefined>;
  /**
   * Called once per request when it fails with `urn:rikkaus:problem:unauthorized`. Return
   * `'retry'` after renewing the session to resend the request once with a fresh token; a
   * second 401 is thrown without calling this again. Return `'fail'` to surface the 401.
   */
  onUnauthorized?: (error: ApiError) => UnauthorizedDecision | Promise<UnauthorizedDecision>;
}

export type UnauthorizedDecision = 'retry' | 'fail';

export interface ApiClientOptions {
  /** A value, or a reader called per request (see `readApiBaseUrlFromEnv`). */
  baseUrl: string | undefined | (() => string | undefined);
  auth?: ApiAuthHooks;
  /** Defaults to the global `fetch`; fixtures and the verification harness replace it. */
  fetchImpl?: typeof fetch;
}

export interface ApiResponse<T> {
  data: T;
  status: number;
  correlationId: string | null;
}

interface CommonRequestOptions {
  signal?: AbortSignal;
  /** Send the access token. Defaults to true; public routes such as `/api/v1/meta` pass false. */
  auth?: boolean;
  /** Choose the correlation id instead of generating one. */
  correlationId?: string;
}

type PathOption<O> = [PathParamsOf<O>] extends [never]
  ? { pathParams?: never }
  : { pathParams: PathParamsOf<O> };

type QueryOption<O> = [QueryParamsOf<O>] extends [never]
  ? { query?: never }
  : object extends QueryParamsOf<O>
    ? { query?: QueryParamsOf<O> }
    : { query: QueryParamsOf<O> };

type BodyOption<O> = [RequestBodyOf<O>] extends [never]
  ? { body?: never }
  : { body: RequestBodyOf<O> };

/**
 * A 2xx JSON body is validated by the caller's Zod schema, whose output must be assignable to the
 * contract's response type: a schema that drifts from the generated contract fails typecheck at
 * the call site. A 204-only operation takes no schema.
 */
type SchemaOption<O, T> = [SuccessBodyOf<O>] extends [never]
  ? { schema?: undefined }
  : { schema: z.ZodType<T> };

export type ApiRequestOptions<
  M extends HttpMethod,
  P extends PathWithMethod<M>,
  T,
> = CommonRequestOptions &
  PathOption<ApiOperation<M, P>> &
  QueryOption<ApiOperation<M, P>> &
  BodyOption<ApiOperation<M, P>> &
  SchemaOption<ApiOperation<M, P>, T>;

/** Paths outside the published `/api/v1` contract that the client still needs. */
export type OperationalPath = '/actuator/health';

type QueryValue = string | number | boolean | null | undefined | readonly (string | number)[];

interface PreparedRequest {
  method: string;
  path: string;
  url: string;
  headers: Record<string, string>;
  body?: string;
  signal?: AbortSignal;
  schema?: z.ZodType<unknown>;
}

function interpolatePath(path: string, params: Record<string, unknown> | undefined): string {
  return path.replace(/\{([^}]+)\}/g, (_, name: string) => {
    const value = params?.[name];

    if (value === undefined || value === null || value === '') {
      throw new TypeError(`Missing path parameter "${name}" for ${path}`);
    }

    return encodeURIComponent(String(value));
  });
}

function buildQuery(query: Record<string, QueryValue> | undefined): string {
  if (!query) return '';

  const search = new URLSearchParams();

  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null) continue;

    for (const item of Array.isArray(value) ? value : [value]) {
      search.append(key, String(item));
    }
  }

  const text = search.toString();

  return text ? `?${text}` : '';
}

function readCorrelationId(response: Response, body?: unknown): string | null {
  const header = response.headers?.get?.(CORRELATION_HEADER);

  if (header) return header;

  if (body && typeof body === 'object' && 'correlationId' in body) {
    const value = (body as { correlationId: unknown }).correlationId;

    return typeof value === 'string' && value ? value : null;
  }

  return null;
}

async function readJson(
  response: Response,
  signal: AbortSignal | undefined,
): Promise<{ ok: true; value: unknown } | { ok: false }> {
  try {
    const text = await response.text();

    return { ok: true, value: text ? JSON.parse(text) : undefined };
  } catch (error) {
    /** An abort while the body streams is cancellation, not a malformed body. */
    if (signal?.aborted) throw error;

    return { ok: false };
  }
}

function issuePaths(error: z.ZodError): string[] {
  return error.issues.map((issue) => (issue.path.length ? issue.path.join('.') : '(root)'));
}

export interface ApiClient {
  request<
    M extends HttpMethod,
    P extends PathWithMethod<M>,
    T extends ContractResponse<M, P> = ContractResponse<M, P>,
  >(
    method: M,
    path: P,
    options: ApiRequestOptions<M, P, T>,
  ): Promise<ApiResponse<T>>;
  /**
   * `GET` on a path outside the contract. Sends no token and no custom header: the
   * `/actuator/health` CORS mapping allows neither, so either would fail the browser preflight.
   */
  getOperational<T>(
    path: OperationalPath,
    options: { schema: z.ZodType<T>; signal?: AbortSignal },
  ): Promise<ApiResponse<T>>;
}

export function createApiClient({ baseUrl, auth, fetchImpl }: ApiClientOptions): ApiClient {
  const readBaseUrl = typeof baseUrl === 'function' ? baseUrl : () => baseUrl;

  /** Throws a `configuration` ApiError, naming the request, before anything is sent. */
  function resolveBase(description: ApiRequestDescription): string {
    try {
      return resolveApiBaseUrl(readBaseUrl());
    } catch (error) {
      if (error instanceof ApiError) throw new ApiError(error.failure, null, description);
      throw error;
    }
  }

  async function execute<T>(prepared: PreparedRequest): Promise<ApiResponse<T>> {
    const description: ApiRequestDescription = { method: prepared.method, path: prepared.path };
    const doFetch = fetchImpl ?? globalThis.fetch;
    let response: Response;

    try {
      response = await doFetch(prepared.url, {
        method: prepared.method,
        headers: prepared.headers,
        body: prepared.body,
        signal: prepared.signal,
      });
    } catch (error) {
      /** Cancellation is not a failure; TanStack Query relies on seeing the AbortError. */
      if (prepared.signal?.aborted) throw error;

      throw new ApiError({ kind: 'network' }, null, description, { cause: error });
    }

    const parsed = await readJson(response, prepared.signal);
    const body = parsed.ok ? parsed.value : undefined;
    const correlationId = readCorrelationId(response, body);

    if (response.ok) {
      if (!prepared.schema) {
        return { data: undefined as T, status: response.status, correlationId };
      }

      const result = parsed.ok ? prepared.schema.safeParse(body) : null;

      if (!result?.success) {
        throw new ApiError(
          {
            kind: 'schema-mismatch',
            status: response.status,
            issuePaths: result ? issuePaths(result.error) : ['(invalid JSON)'],
          },
          correlationId,
          description,
        );
      }

      return { data: result.data as T, status: response.status, correlationId };
    }

    const problem =
      parsed.ok && isProblemContentType(response.headers?.get?.('Content-Type') ?? null)
        ? ProblemDetailSchema.safeParse(body)
        : null;

    if (problem?.success) {
      throw new ApiError(
        { kind: 'problem', status: response.status, problem: problem.data },
        correlationId,
        description,
      );
    }

    throw new ApiError({ kind: 'http', status: response.status }, correlationId, description);
  }

  async function request<M extends HttpMethod, P extends PathWithMethod<M>, T>(
    method: M,
    path: P,
    options: ApiRequestOptions<M, P, T>,
  ): Promise<ApiResponse<T>> {
    const {
      pathParams,
      query,
      body,
      schema,
      signal,
      auth: useAuth = true,
      correlationId,
    } = options as CommonRequestOptions & {
      pathParams?: Record<string, unknown>;
      query?: Record<string, QueryValue>;
      body?: unknown;
      schema?: z.ZodType<unknown>;
    };
    const httpMethod = method.toUpperCase();
    const base = resolveBase({ method: httpMethod, path });
    const url = `${base}${interpolatePath(path, pathParams)}${buildQuery(query)}`;

    async function attempt(isRetry: boolean): Promise<ApiResponse<T>> {
      const headers: Record<string, string> = {
        Accept: 'application/json',
        [CORRELATION_HEADER]: correlationId ?? createCorrelationId(),
      };

      if (body !== undefined) headers['Content-Type'] = 'application/json';

      if (useAuth && auth) {
        const token = await auth.getAccessToken();

        if (token) headers.Authorization = `Bearer ${token}`;
      }

      try {
        return await execute<T>({
          method: httpMethod,
          path,
          url,
          headers,
          body: body === undefined ? undefined : JSON.stringify(body),
          signal,
          schema,
        });
      } catch (error) {
        const unauthorized =
          error instanceof ApiError &&
          error.failure.kind === 'problem' &&
          error.failure.problem.type === 'urn:rikkaus:problem:unauthorized';

        if (unauthorized && useAuth && !isRetry && auth?.onUnauthorized) {
          if ((await auth.onUnauthorized(error)) === 'retry') return attempt(true);
        }

        throw error;
      }
    }

    return attempt(false);
  }

  async function getOperational<T>(
    path: OperationalPath,
    { schema, signal }: { schema: z.ZodType<T>; signal?: AbortSignal },
  ): Promise<ApiResponse<T>> {
    const base = resolveBase({ method: 'GET', path });

    return execute<T>({
      method: 'GET',
      path,
      url: `${base}${path}`,
      headers: { Accept: 'application/json' },
      signal,
      schema: schema as z.ZodType<unknown>,
    });
  }

  return { request, getOperational } as ApiClient;
}
