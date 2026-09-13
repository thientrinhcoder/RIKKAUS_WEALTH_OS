import { HealthResponseSchema } from '@/features/health/health-schema';

describe('HealthResponseSchema', () => {
  it('accepts the minimal healthy response', () => {
    expect(HealthResponseSchema.parse({ status: 'UP' })).toEqual({ status: 'UP' });
  });

  it('tolerates additional Actuator fields', () => {
    expect(
      HealthResponseSchema.parse({ status: 'UP', components: { database: { status: 'UP' } } }),
    ).toEqual({ status: 'UP', components: { database: { status: 'UP' } } });
  });

  it.each([{ status: 'DOWN' }, {}, null])('rejects an unhealthy or invalid response: %p', (value) => {
    expect(() => HealthResponseSchema.parse(value)).toThrow();
  });
});
