export {
  createApiClient,
  type ApiAuthHooks,
  type ApiClient,
  type ApiClientOptions,
  type ApiRequestOptions,
  type ApiResponse,
  type OperationalPath,
  type UnauthorizedDecision,
} from './api-client';
export { isApiBaseUrlConfigured, readApiBaseUrlFromEnv, resolveApiBaseUrl } from './api-config';
export { ApiError, isApiError, type ApiFailure } from './api-error';
export { ApiClientProvider, shouldRetryApiError, useApiClient } from './api-query';
export {
  classifyApiError,
  formatReferenceLabel,
  type ApiErrorPresentation,
} from './classify-api-error';
export type {
  ApiPath,
  ContractSchemas,
  HttpMethod,
  KnownProblemType,
  PathWithMethod,
} from './contract-types';
export { CORRELATION_HEADER, CORRELATION_ID_PATTERN, createCorrelationId } from './correlation';
export { contractFixture, createFixtureFetch, type FixtureResponse, type FixtureRoute } from './fixtures';
export { fetchMeta, MetaResponseSchema, type MetaResponse } from './meta';
export {
  isKnownProblemType,
  KNOWN_PROBLEM_TYPES,
  ProblemDetailSchema,
  type ProblemDetail,
} from './problem';
