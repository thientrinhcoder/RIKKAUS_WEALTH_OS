import type { components, paths } from './generated/openapi';

/**
 * Type-level views over the generated contract. Nothing here exists at runtime; it lets the
 * client accept only paths, methods, parameters and bodies the contract declares, and lets a Zod
 * schema be checked against the contract's response type at compile time.
 */

export type HttpMethod = 'get' | 'post' | 'put' | 'patch' | 'delete';

export type ApiPath = keyof paths;

/** `paths[P][M]` is `undefined` (declared `?: never`) for a method the path does not support. */
type OperationOf<P extends ApiPath, M extends HttpMethod> = NonNullable<paths[P][M]>;

/** Every path that supports method `M`. */
export type PathWithMethod<M extends HttpMethod> = {
  [P in ApiPath]: [OperationOf<P, M>] extends [never] ? never : P;
}[ApiPath];

export type ApiOperation<M extends HttpMethod, P extends PathWithMethod<M>> = OperationOf<P, M>;

type ParametersOf<O> = O extends { parameters: infer Parameters } ? Parameters : never;

export type PathParamsOf<O> =
  ParametersOf<O> extends { path?: infer Path } ? ([Path] extends [undefined] ? never : Path) : never;

export type QueryParamsOf<O> =
  ParametersOf<O> extends { query?: infer Query }
    ? [Query] extends [undefined]
      ? never
      : Query
    : never;

/** `requestBody?: never` (no body) would otherwise match the pattern below with `unknown`. */
export type RequestBodyOf<O> = O extends { requestBody?: infer RequestBody }
  ? [NonNullable<RequestBody>] extends [never]
    ? never
    : NonNullable<RequestBody> extends { content: { 'application/json': infer Body } }
      ? Body
      : never
  : never;

type SuccessStatus = 200 | 201;

/** The JSON body of the operation's 200 or 201 response; `never` for a 204-only operation. */
export type SuccessBodyOf<O> = O extends { responses: infer Responses }
  ? {
      [S in keyof Responses & SuccessStatus]: Responses[S] extends {
        content: { 'application/json': infer Body };
      }
        ? Body
        : never;
    }[keyof Responses & SuccessStatus]
  : never;

/** What a request resolves with: the success body, or `undefined` for a 204-only operation. */
export type ContractResponse<M extends HttpMethod, P extends PathWithMethod<M>> = [
  SuccessBodyOf<ApiOperation<M, P>>,
] extends [never]
  ? undefined
  : SuccessBodyOf<ApiOperation<M, P>>;

export type ContractSchemas = components['schemas'];

/** The problem `type` URNs the contract publishes today. */
export type KnownProblemType = NonNullable<ContractSchemas['ProblemDetail']['type']>;
