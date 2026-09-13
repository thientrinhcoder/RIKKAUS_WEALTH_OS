import {
  fetchHealth,
  HealthCheckError,
  normalizeApiBaseUrl,
} from '@/features/health/health-client';

function responseWith(payload: unknown, options?: { ok?: boolean; status?: number }): Response {
  return {
    json: jest.fn().mockResolvedValue(payload),
    ok: options?.ok ?? true,
    status: options?.status ?? 200,
  } as unknown as Response;
}

describe('health client', () => {
  it('normalizes one or more trailing slashes', () => {
    expect(normalizeApiBaseUrl('http://localhost:8080///')).toBe('http://localhost:8080');
  });

  it('requests and validates the Actuator health endpoint', async () => {
    const fetchMock = jest.fn().mockResolvedValue(
      responseWith({ status: 'UP', components: { database: { status: 'UP' } } }),
    );
    globalThis.fetch = fetchMock;

    await expect(fetchHealth('http://localhost:8080/')).resolves.toMatchObject({ status: 'UP' });
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/actuator/health', {
      headers: { Accept: 'application/json' },
      signal: undefined,
    });
  });

  it('reports non-successful HTTP responses without parsing a body', async () => {
    globalThis.fetch = jest.fn().mockResolvedValue(responseWith({}, { ok: false, status: 503 }));

    await expect(fetchHealth('http://localhost:8080')).rejects.toEqual(
      new HealthCheckError('The API returned HTTP 503.'),
    );
  });

  it('rejects invalid configured URLs', async () => {
    await expect(fetchHealth('not-a-url')).rejects.toEqual(
      new HealthCheckError('The API endpoint configuration is invalid.'),
    );
  });
});
