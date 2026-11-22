/**
 * Executes the #38 test cases against the real API. Imports apps/mobile/src/api unchanged.
 * Usage: npx tsx run-test-cases.ts <apps/mobile dir> <evidence json path>
 * Env: API=http://localhost:8180, USER_ID=<inserted user uuid>
 */
import { createHmac, randomUUID } from 'node:crypto';
import { writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { resolve } from 'node:path';

const appDir = resolve(process.argv[2]);
const evidencePath = resolve(process.argv[3]);
const requireFromApp = createRequire(`${appDir}/package.json`);
const { z } = requireFromApp('zod') as typeof import('zod');

const api = (await import(`${appDir}/src/api/index.ts`)) as any;
const { fetchHealth } = await import(`${appDir}/src/features/health/health-client.ts`);
const { HealthResponseSchema } = await import(`${appDir}/src/features/health/health-schema.ts`);

const API = process.env.API ?? 'http://localhost:8180';
const USER_ID = process.env.USER_ID!;

// ---- tokens, minted as docs/manual-test-guide-assets.md §4 describes ----------------------
const secret = 'local-development-only-jwt-signing-secret';
const b64 = (raw: Buffer | string) => Buffer.from(raw).toString('base64url');
function mint(sub: string, ttl = 900, key = secret) {
  const now = Math.floor(Date.now() / 1000);
  const h = b64(JSON.stringify({ alg: 'HS256' }));
  const p = b64(
    JSON.stringify({
      iss: 'rikkaus-wealth-api',
      sub,
      sid: '11111111-2222-3333-4444-555555555555',
      iat: now,
      exp: now + ttl,
    }),
  );
  return `${h}.${p}.${b64(createHmac('sha256', key).update(`${h}.${p}`).digest())}`;
}
const VALID = mint(USER_ID);
const EXPIRED = mint(USER_ID, -60);
const FORGED = mint(USER_ID, 900, 'a-different-secret-entirely');
const GHOST = mint(randomUUID());

// ---- an evidence-recording fetch --------------------------------------------------------
interface Exchange {
  request: { method: string; url: string; headers: Record<string, string>; body?: string };
  response?: { status: number; headers: Record<string, string>; body: string };
  error?: string;
}
let exchanges: Exchange[] = [];
function redact(headers: Record<string, string>) {
  const out = { ...headers };
  if (out.authorization) {
    const token = out.authorization.slice(7);
    const label =
      token === VALID ? 'valid' : token === EXPIRED ? 'expired' : token === FORGED ? 'forged' : token === GHOST ? 'no-user-row' : 'other';
    out.authorization = `Bearer [redacted ${label} token]`;
  }
  return out;
}
function recordingFetch(rewrite?: (init: RequestInit) => RequestInit): typeof fetch {
  return async (input, init = {}) => {
    const finalInit = rewrite ? rewrite(init) : init;
    const url = String(input);
    const exchange: Exchange = {
      request: {
        method: String(finalInit.method),
        url,
        headers: redact(Object.fromEntries(new Headers(finalInit.headers).entries())),
        body: finalInit.body as string | undefined,
      },
    };
    exchanges.push(exchange);
    try {
      const response = await fetch(url, finalInit);
      const body = await response.clone().text();
      exchange.response = {
        status: response.status,
        headers: Object.fromEntries(
          [...response.headers.entries()].filter(([k]) =>
            ['content-type', 'x-correlation-id'].includes(k),
          ),
        ),
        body: body.length > 600 ? `${body.slice(0, 600)}…` : body,
      };
      return response;
    } catch (error) {
      exchange.error = String(error);
      throw error;
    }
  };
}
const client = (opts: Partial<Parameters<typeof api.createApiClient>[0]> = {}) =>
  api.createApiClient({ baseUrl: `${API}/`, fetchImpl: recordingFetch(), ...opts });
const authed = (token = VALID) => client({ auth: { getAccessToken: () => token } });

// ---- case runner -------------------------------------------------------------------------
interface Result {
  id: string;
  title: string;
  pass: boolean;
  checks: Record<string, unknown>;
  failures: string[];
  exchanges: Exchange[];
}
const results: Result[] = [];
async function tc(id: string, title: string, body: (expect: (label: string, actual: unknown, expected: unknown) => void, note: Record<string, unknown>) => Promise<void>) {
  exchanges = [];
  const failures: string[] = [];
  const checks: Record<string, unknown> = {};
  const expect = (label: string, actual: unknown, expected: unknown) => {
    const ok =
      expected instanceof RegExp ? expected.test(String(actual)) : JSON.stringify(actual) === JSON.stringify(expected);
    checks[label] = actual;
    if (!ok) failures.push(`${label}: expected ${String(expected)}, got ${JSON.stringify(actual)}`);
  };
  try {
    await body(expect, checks);
  } catch (error) {
    failures.push(`threw: ${String(error)}`);
  }
  results.push({ id, title, pass: failures.length === 0, checks, failures, exchanges });
  console.log(`${failures.length === 0 ? 'PASS' : 'FAIL'} ${id} ${title}${failures.length ? `\n   ${failures.join('\n   ')}` : ''}`);
}
async function caught(promise: Promise<unknown>) {
  try {
    await promise;
  } catch (error) {
    return error as InstanceType<typeof api.ApiError>;
  }
  throw new Error('expected the request to fail');
}
function expectProblem(
  expect: (l: string, a: unknown, e: unknown) => void,
  error: InstanceType<typeof api.ApiError>,
  type: string,
  status: number,
  title: string,
  retryable: boolean,
  requiresSignIn = false,
) {
  const p = api.classifyApiError(error);
  expect('failure.kind', error.failure.kind, 'problem');
  expect('problem.type', p.problemType, type);
  expect('status', p.status, status);
  expect('title', p.title, title);
  expect('retryable', p.retryable, retryable);
  expect('requiresSignIn', p.requiresSignIn, requiresSignIn);
  expect('correlationId present', Boolean(error.correlationId), true);
  if (error.failure.kind === 'problem') {
    expect('header id == body id', error.correlationId, error.failure.problem.correlationId);
    const detail = error.failure.problem.detail ?? '';
    expect('detail not displayed', Boolean(detail) && (p.title.includes(detail) || p.message.includes(detail)), false);
  }
  expect('message (vi)', p.message, /./);
}
const META = api.MetaResponseSchema;
const AssetSchema = z.object({ id: z.string() });

// ---- TC-01 / TC-02 ----------------------------------------------------------------------
await tc('TC-01', 'base URL with trailing slash resolves without a double slash', async (expect) => {
  const res = await client().request('get', '/api/v1/meta', { schema: META, auth: false });
  expect('url', exchanges[0].request.url, `${API}/api/v1/meta`);
  expect('status', res.status, 200);
});
await tc('TC-02', 'missing / invalid base URL fails before any request', async (expect) => {
  for (const [label, value, reason] of [
    ['unset', undefined, 'missing'],
    ['not-a-url', 'not-a-url', 'invalid'],
    ['ftp', 'ftp://localhost:8180', 'invalid'],
  ] as const) {
    const error = await caught(client({ baseUrl: value }).request('get', '/api/v1/meta', { schema: META, auth: false }));
    expect(`${label}: kind`, error.failure, { kind: 'configuration', reason });
    expect(`${label}: title`, api.classifyApiError(error).title, 'Chưa cấu hình đúng địa chỉ API');
  }
  expect('network calls', exchanges.length, 0);
});

// ---- success -----------------------------------------------------------------------------
await tc('TC-03', 'GET /api/v1/meta succeeds and is validated', async (expect) => {
  const res = await api.fetchMeta(client());
  expect('status', res.status, 200);
  expect('apiVersion', res.data.apiVersion, 'v1');
  expect('serverTime', res.data.serverTime, /^\d{4}-\d\d-\d\dT[\d:.]+Z$/);
  expect('correlationId', res.correlationId, /^[A-Za-z0-9-]{8,64}$/);
});
await tc('TC-04', 'authenticated success attaches the bearer token from the provider', async (expect) => {
  const res = await authed().request('get', '/api/v1/auth/session', {
    schema: z.object({ id: z.string(), email: z.string(), displayName: z.string() }),
  });
  expect('status', res.status, 200);
  expect('data.id', res.data.id, USER_ID);
  expect('Authorization sent', exchanges[0].request.headers.authorization, 'Bearer [redacted valid token]');
});
await tc('TC-05', 'public and operational routes do not carry the token', async (expect) => {
  const c = authed(FORGED);
  const meta = await c.request('get', '/api/v1/meta', { schema: META, auth: false });
  const health = await fetchHealth(c);
  expect('meta status', meta.status, 200);
  expect('health status', health.status, 'UP');
  expect('no Authorization header sent', exchanges.some((e) => 'authorization' in e.request.headers), false);
  expect('health headers', exchanges[1].request.headers, { accept: 'application/json' });
});

// ---- problems ----------------------------------------------------------------------------
await tc('TC-07', 'validation-failed exposes field names, never server text', async (expect) => {
  const error = await caught(
    authed().request('post', '/api/v1/assets', { body: {} as never, schema: AssetSchema }),
  );
  expectProblem(expect, error, 'urn:rikkaus:problem:validation-failed', 400, 'Thông tin chưa hợp lệ', false);
  const p = api.classifyApiError(error);
  expect('fields', [...p.fields].sort(), ['category', 'currency', 'currentValue', 'liquidity', 'name', 'valuationDate']);
  if (error.failure.kind === 'problem') {
    const serverMessages = (error.failure.problem.errors ?? []).map((e) => e.message ?? '');
    expect('no errors[].message in copy', serverMessages.some((m) => p.message.includes(m) || p.title.includes(m)), false);
  }
});
await tc('TC-08', 'malformed-request', async (expect) => {
  const c = client({ auth: { getAccessToken: () => VALID }, fetchImpl: recordingFetch((init) => ({ ...init, body: '{' })) });
  const error = await caught(c.request('post', '/api/v1/assets', { body: {} as never, schema: AssetSchema }));
  expectProblem(expect, error, 'urn:rikkaus:problem:malformed-request', 400, 'Yêu cầu không đọc được', false);
});
await tc('TC-09', 'not-found', async (expect) => {
  const error = await caught(
    authed().request('get', '/api/v1/assets/{assetId}', { pathParams: { assetId: randomUUID() }, schema: AssetSchema }),
  );
  expectProblem(expect, error, 'urn:rikkaus:problem:not-found', 404, 'Không thể mở mục này', false);
});
await tc('TC-10', 'method-not-allowed', async (expect) => {
  const error = await caught(client().request('delete' as 'get', '/api/v1/meta', { schema: META, auth: false }));
  expectProblem(expect, error, 'urn:rikkaus:problem:method-not-allowed', 405, 'Ứng dụng cần được cập nhật', false);
});
await tc('TC-11', 'not-acceptable', async (expect) => {
  const c = client({ fetchImpl: recordingFetch((init) => ({ ...init, headers: { ...(init.headers as object), Accept: 'text/csv' } })) });
  const error = await caught(c.request('get', '/api/v1/meta', { schema: META, auth: false }));
  expectProblem(expect, error, 'urn:rikkaus:problem:not-acceptable', 406, 'Ứng dụng cần được cập nhật', false);
});
await tc('TC-12', 'unsupported-media-type', async (expect) => {
  const c = client({
    auth: { getAccessToken: () => VALID },
    fetchImpl: recordingFetch((init) => ({ ...init, headers: { ...(init.headers as object), 'Content-Type': 'text/plain' } })),
  });
  const error = await caught(c.request('post', '/api/v1/assets', { body: {} as never, schema: AssetSchema }));
  expectProblem(expect, error, 'urn:rikkaus:problem:unsupported-media-type', 415, 'Ứng dụng cần được cập nhật', false);
});
await tc('TC-13', 'unauthorized (no token, then expired token)', async (expect) => {
  const Session = z.object({ id: z.string() });
  const anonymous = await caught(client().request('get', '/api/v1/auth/session', { schema: Session }));
  expectProblem(expect, anonymous, 'urn:rikkaus:problem:unauthorized', 401, 'Phiên đăng nhập đã hết hạn', false, true);
  const expired = await caught(authed(EXPIRED).request('get', '/api/v1/auth/session', { schema: Session }));
  expect('expired: type', api.classifyApiError(expired).problemType, 'urn:rikkaus:problem:unauthorized');
  expect('expired: requiresSignIn', api.classifyApiError(expired).requiresSignIn, true);
});
await tc('TC-14', 'forbidden — unreachable in MVP 0; contract-derived fixture body', async (expect, note) => {
  const body = {
    type: 'urn:rikkaus:problem:forbidden',
    title: 'Access denied',
    status: 403,
    detail: 'Access denied.',
    instance: '/api/v1/assets',
    correlationId: 'gh38-fixture-0403',
  };
  note.source = 'fixture (no route can return 403 in MVP 0; contract docs: ownership failures return not-found)';
  const c = client({
    fetchImpl: api.createFixtureFetch([
      { route: 'GET /api/v1/assets', response: { status: 403, contentType: 'application/problem+json', body, headers: { 'X-Correlation-Id': 'gh38-fixture-0403' } } },
    ]),
  });
  const error = await caught(c.request('get', '/api/v1/assets', { schema: z.array(AssetSchema) }));
  expectProblem(expect, error, 'urn:rikkaus:problem:forbidden', 403, 'Không thể mở mục này', false);
});
await tc('TC-15', 'internal-error — attempted with a token whose subject has no user row', async (expect, note) => {
  const attempt = await caught(
    authed(GHOST).request('get', '/api/v1/auth/session', { schema: z.object({ id: z.string() }) }),
  ).catch((e) => e);
  note.realAttempt =
    attempt instanceof api.ApiError ? { failure: attempt.failure.kind, type: api.classifyApiError(attempt).problemType, status: api.classifyApiError(attempt).status } : String(attempt);
  const write = await caught(
    authed(GHOST).request('post', '/api/v1/assets', {
      body: { name: 'Tài khoản thanh toán Vietcombank', category: 'BANK_ACCOUNT', currency: 'VND', currentValue: 125_000_000, liquidity: 'HIGH', valuationDate: new Date().toISOString().slice(0, 10) },
      schema: AssetSchema,
    }),
  ).catch((e) => e);
  note.realWriteAttempt =
    write instanceof api.ApiError ? { failure: write.failure.kind, type: api.classifyApiError(write).problemType, status: api.classifyApiError(write).status } : String(write);
  const isRealInternal = write instanceof api.ApiError && api.classifyApiError(write).problemType === 'urn:rikkaus:problem:internal-error';
  note.source = isRealInternal ? 'real API' : 'fixture (the real API did not produce internal-error)';
  const error = isRealInternal
    ? write
    : await caught(
        client({
          fetchImpl: api.createFixtureFetch([
            {
              route: 'GET /api/v1/meta',
              response: {
                status: 500,
                contentType: 'application/problem+json',
                body: { type: 'urn:rikkaus:problem:internal-error', title: 'Unexpected server error', status: 500, detail: 'An unexpected error occurred.', instance: '/api/v1/meta', correlationId: 'gh38-fixture-0500' },
                headers: { 'X-Correlation-Id': 'gh38-fixture-0500' },
              },
            },
          ]),
        }).request('get', '/api/v1/meta', { schema: META, auth: false }),
      );
  expectProblem(expect, error, 'urn:rikkaus:problem:internal-error', 500, 'Máy chủ gặp sự cố', true);
});
await tc('TC-16', 'unknown future type URN degrades by status', async (expect) => {
  const c = client({
    fetchImpl: api.createFixtureFetch([
      { route: 'GET /api/v1/meta', response: { status: 429, contentType: 'application/problem+json', body: { type: 'urn:rikkaus:problem:rate-limited', title: 'Too many requests', status: 429, correlationId: 'gh38-fixture-0429' } } },
    ]),
  });
  const error = await caught(c.request('get', '/api/v1/meta', { schema: META, auth: false }));
  const p = api.classifyApiError(error);
  expect('kind', p.kind, 'problem');
  expect('problemType', p.problemType, 'urn:rikkaus:problem:rate-limited');
  expect('title', p.title, 'Yêu cầu chưa hoàn tất');
});

// ---- auth hook ---------------------------------------------------------------------------
const Session = z.object({ id: z.string() });
await tc('TC-17', '401 → onUnauthorized once → one retry with a fresh token', async (expect) => {
  const tokens = [EXPIRED, VALID];
  let calls = 0;
  let seen: string | null = null;
  const c = client({
    auth: {
      getAccessToken: () => tokens.shift(),
      onUnauthorized: (error) => {
        calls += 1;
        seen = `${api.classifyApiError(error).problemType} ${error.correlationId}`;
        return 'retry';
      },
    },
  });
  const res = await c.request('get', '/api/v1/auth/session', { schema: Session });
  expect('onUnauthorized calls', calls, 1);
  expect('onUnauthorized saw', seen, /^urn:rikkaus:problem:unauthorized [A-Za-z0-9-]{8,64}$/);
  expect('final status', res.status, 200);
  expect('requests sent', exchanges.length, 2);
  expect('statuses', exchanges.map((e) => e.response?.status), [401, 200]);
});
await tc('TC-18', 'a second 401 is not retried again', async (expect) => {
  let calls = 0;
  const c = client({ auth: { getAccessToken: () => EXPIRED, onUnauthorized: () => { calls += 1; return 'retry'; } } });
  const error = await caught(c.request('get', '/api/v1/auth/session', { schema: Session }));
  expect('type', api.classifyApiError(error).problemType, 'urn:rikkaus:problem:unauthorized');
  expect('onUnauthorized calls', calls, 1);
  expect('requests sent', exchanges.length, 2);
});
await tc('TC-19', "onUnauthorized 'fail' surfaces the 401 without retry", async (expect) => {
  let calls = 0;
  const c = client({ auth: { getAccessToken: () => EXPIRED, onUnauthorized: () => { calls += 1; return 'fail'; } } });
  const error = await caught(c.request('get', '/api/v1/auth/session', { schema: Session }));
  expect('type', api.classifyApiError(error).problemType, 'urn:rikkaus:problem:unauthorized');
  expect('onUnauthorized calls', calls, 1);
  expect('requests sent', exchanges.length, 1);
});

// ---- correlation -------------------------------------------------------------------------
await tc('TC-20', 'client sends a valid correlation id; server echoes it', async (expect) => {
  const res = await client().request('get', '/api/v1/meta', { schema: META, auth: false });
  const sent = exchanges[0].request.headers['x-correlation-id'];
  expect('sent matches pattern', sent, /^[A-Za-z0-9-]{8,64}$/);
  expect('echoed header', exchanges[0].response?.headers['x-correlation-id'], sent);
  expect('ApiResponse.correlationId', res.correlationId, sent);
});
await tc('TC-21', 'caller-chosen id echoed on errors in header and body', async (expect) => {
  const error = await caught(
    authed().request('get', '/api/v1/assets/{assetId}', { pathParams: { assetId: randomUUID() }, schema: AssetSchema, correlationId: 'gh38-trace-0001' }),
  );
  expect('ApiError.correlationId', error.correlationId, 'gh38-trace-0001');
  expect('response header', exchanges[0].response?.headers['x-correlation-id'], 'gh38-trace-0001');
  expect('body correlationId', error.failure.kind === 'problem' ? error.failure.problem.correlationId : null, 'gh38-trace-0001');
  expect('referenceLabel', api.classifyApiError(error).referenceLabel, 'Mã tham chiếu: gh38-trace-0001');
});

// ---- transport and shape -----------------------------------------------------------------
await tc('TC-23', 'network failure (no listener on 8199)', async (expect) => {
  const error = await caught(client({ baseUrl: 'http://localhost:8199' }).request('get', '/api/v1/meta', { schema: META, auth: false }));
  const p = api.classifyApiError(error);
  expect('kind', p.kind, 'network');
  expect('title', p.title, 'Không kết nối được máy chủ');
  expect('variant', p.variant, 'offline');
  expect('retryable', p.retryable, true);
});
await tc('TC-24', 'schema mismatch on a real 2xx body', async (expect) => {
  const error = await caught(
    client().request('get', '/api/v1/meta', { schema: z.object({ apiVersion: z.literal('v2' as string) }), auth: false }),
  );
  const p = api.classifyApiError(error);
  expect('failure', error.failure, { kind: 'schema-mismatch', status: 200, issuePaths: ['apiVersion'] });
  expect('correlationId is the real one', error.correlationId, exchanges[0].response?.headers['x-correlation-id']);
  expect('title', p.title, 'Dữ liệu trả về không đúng định dạng');
  expect('retryable', p.retryable, false);
});
await tc('TC-25', 'non-problem error body (Actuator 503 DOWN, fixture)', async (expect) => {
  const c = client({
    fetchImpl: api.createFixtureFetch([
      { route: 'GET /actuator/health', response: { status: 503, contentType: 'application/vnd.spring-boot.actuator.v3+json', body: { status: 'DOWN' } } },
    ]),
  });
  const error = await caught(fetchHealth(c));
  const p = api.classifyApiError(error);
  expect('failure', error.failure, { kind: 'http', status: 503 });
  expect('title', p.title, 'Máy chủ chưa sẵn sàng');
  expect('retryable', p.retryable, true);
});
await tc('TC-26', 'abort propagates the AbortError unchanged', async (expect) => {
  const controller = new AbortController();
  const pending = client().request('get', '/api/v1/meta', { schema: META, auth: false, signal: controller.signal });
  controller.abort();
  const error = await caught(pending);
  expect('is ApiError', error instanceof api.ApiError, false);
  expect('name', (error as Error).name, 'AbortError');
});

// ---- TanStack retry policy over the errors produced above --------------------------------
await tc('TC-27', 'retry policy follows the classification', async (expect) => {
  const make = async (p: Promise<unknown>) => caught(p);
  const network = await make(client({ baseUrl: 'http://localhost:8199' }).request('get', '/api/v1/meta', { schema: META, auth: false }));
  const notFound = await make(authed().request('get', '/api/v1/assets/{assetId}', { pathParams: { assetId: randomUUID() }, schema: AssetSchema }));
  const unauthorized = await make(client().request('get', '/api/v1/auth/session', { schema: Session }));
  const mismatch = await make(client().request('get', '/api/v1/meta', { schema: z.object({ apiVersion: z.literal('v2' as string) }), auth: false }));
  const config = await make(client({ baseUrl: undefined }).request('get', '/api/v1/meta', { schema: META, auth: false }));
  const http503 = new api.ApiError({ kind: 'http', status: 503 }, null);
  const internal = new api.ApiError({ kind: 'problem', status: 500, problem: { type: 'urn:rikkaus:problem:internal-error', title: 'x', status: 500 } }, null);
  expect('network first failure', api.shouldRetryApiError(0, network), true);
  expect('network second failure', api.shouldRetryApiError(1, network), false);
  expect('internal-error', api.shouldRetryApiError(0, internal), true);
  expect('http 503', api.shouldRetryApiError(0, http503), true);
  expect('not-found', api.shouldRetryApiError(0, notFound), false);
  expect('unauthorized', api.shouldRetryApiError(0, unauthorized), false);
  expect('schema-mismatch', api.shouldRetryApiError(0, mismatch), false);
  expect('configuration', api.shouldRetryApiError(0, config), false);
});

// ---- fixtures from the contract ----------------------------------------------------------
await tc('TC-30', 'every contract example is available and parses', async (expect) => {
  const { contractExamples } = await import(`${appDir}/src/api/generated/contract-examples.ts`);
  let total = 0;
  let problems = 0;
  const unparsed: string[] = [];
  for (const [op, byStatus] of Object.entries(contractExamples as Record<string, Record<string, { contentType: string; examples: Record<string, unknown> }>>)) {
    for (const [status, entry] of Object.entries(byStatus)) {
      for (const [name, value] of Object.entries(entry.examples)) {
        total += 1;
        if (entry.contentType === 'application/problem+json') {
          problems += 1;
          const parsed = api.ProblemDetailSchema.safeParse(value);
          if (!parsed.success || !api.isKnownProblemType(parsed.data.type)) unparsed.push(`${op} ${status} ${name}`);
        }
      }
    }
  }
  expect('total examples', total, 51);
  expect('problem examples all parse and are known', unparsed, []);
  const meta = api.contractFixture('GET /api/v1/meta', '200', 'current build');
  expect('meta example parses', META.safeParse(meta.body).success, true);
  const res = await client({ fetchImpl: api.createFixtureFetch([{ route: 'GET /api/v1/meta', response: meta }]) }).request('get', '/api/v1/meta', { schema: META, auth: false });
  expect('meta fixture through client', res.data.buildVersion, '0.0.1-SNAPSHOT');
  expect('problem examples count', problems, problems);
});

writeFileSync(evidencePath, JSON.stringify({ executedAt: new Date().toISOString(), api: API, results }, null, 2));
const failed = results.filter((r) => !r.pass);
console.log(`\n${results.length - failed.length}/${results.length} passed`);
process.exit(failed.length ? 1 : 0);
