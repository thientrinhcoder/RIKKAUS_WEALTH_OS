import { createApiClient } from '@/api';
import { fetchHealth } from '@/features/health/health-client';

function responseWith(payload: unknown, options?: { ok?: boolean; status?: number }): Response {
  return {
    text: jest.fn().mockResolvedValue(JSON.stringify(payload)),
    headers: new Headers({ 'Content-Type': 'application/json' }),
    ok: options?.ok ?? true,
    status: options?.status ?? 200,
  } as unknown as Response;
}

describe('health client', () => {
  it('requests and validates the Actuator health endpoint without custom headers', async () => {
    const fetchMock = jest
      .fn()
      .mockResolvedValue(
        responseWith({ status: 'UP', components: { database: { status: 'UP' } } }),
      );
    const client = createApiClient({ baseUrl: 'http://localhost:8080///', fetchImpl: fetchMock });

    await expect(fetchHealth(client)).resolves.toMatchObject({ status: 'UP' });
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/actuator/health', {
      method: 'GET',
      headers: { Accept: 'application/json' },
      body: undefined,
      signal: undefined,
    });
  });

  it('reports non-successful HTTP responses as an http failure', async () => {
    const client = createApiClient({
      baseUrl: 'http://localhost:8080',
      fetchImpl: jest
        .fn()
        .mockResolvedValue(responseWith({ status: 'DOWN' }, { ok: false, status: 503 })),
    });

    await expect(fetchHealth(client)).rejects.toMatchObject({
      failure: { kind: 'http', status: 503 },
    });
  });

  it('rejects invalid configured URLs without calling fetch', async () => {
    const fetchMock = jest.fn();
    const client = createApiClient({ baseUrl: 'not-a-url', fetchImpl: fetchMock });

    await expect(fetchHealth(client)).rejects.toMatchObject({
      failure: { kind: 'configuration', reason: 'invalid' },
    });
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
