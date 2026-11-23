import { CORRELATION_HEADER, createCorrelationId } from './correlation';
import { contractExamples } from './generated/contract-examples';

/**
 * Mock responses built from the contract's own examples, for component previews and offline
 * development. Nothing here is wired into the running app: a screen only sees fixtures when a
 * caller passes `createFixtureFetch` as the client's `fetchImpl`.
 */

type Examples = typeof contractExamples;

export type ContractOperation = keyof Examples;

export type ContractStatus<O extends ContractOperation> = keyof Examples[O] & string;

export type ContractExampleName<
  O extends ContractOperation,
  S extends ContractStatus<O>,
> = Examples[O][S] extends { examples: infer E } ? keyof E & string : never;

export interface FixtureResponse {
  status: number;
  contentType: string;
  body: unknown;
  headers?: Record<string, string>;
}

/** One named example from the contract, e.g. `contractFixture('GET /api/v1/meta', '200', 'current build')`. */
export function contractFixture<
  O extends ContractOperation,
  S extends ContractStatus<O>,
  N extends ContractExampleName<O, S>,
>(operation: O, status: S, name: N): FixtureResponse {
  const entry = contractExamples[operation][status] as unknown as {
    contentType: string;
    examples: Record<string, unknown>;
  };

  return { status: Number(status), contentType: entry.contentType, body: entry.examples[name] };
}

export interface FixtureRoute {
  /** `METHOD /concrete/path`, matched without the query string. */
  route: string;
  response: FixtureResponse;
}

/**
 * A `fetch` that answers from fixtures and never touches the network. Each response carries an
 * `X-Correlation-Id`, as the real server's does. An unmatched route rejects like an unreachable
 * host, so a missing fixture is visible rather than silently answered.
 */
export function createFixtureFetch(routes: FixtureRoute[]): typeof fetch {
  return async (input, init) => {
    const url = new URL(
      typeof input === 'string' ? input : input instanceof URL ? input.href : input.url,
    );
    const method = (init?.method ?? 'GET').toUpperCase();
    const match = routes.find((candidate) => candidate.route === `${method} ${url.pathname}`);

    if (!match) {
      throw new TypeError(`No fixture for ${method} ${url.pathname}`);
    }

    const { status, contentType, body, headers } = match.response;
    const sentCorrelationId = new Headers(init?.headers).get(CORRELATION_HEADER);

    return new Response(body === undefined ? null : JSON.stringify(body), {
      status,
      headers: {
        'Content-Type': contentType,
        [CORRELATION_HEADER]: sentCorrelationId ?? createCorrelationId(),
        ...headers,
      },
    });
  };
}
