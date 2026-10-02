import { singleFlight } from '@/components/form/single-flight';

describe('single flight', () => {
  it('runs the action', async () => {
    const run = jest.fn().mockResolvedValue(undefined);

    await expect(singleFlight(run)()).resolves.toBe('ran');
    expect(run).toHaveBeenCalledTimes(1);
  });

  it('skips activations that arrive while one is already running', async () => {
    let release: () => void = () => {};
    const run = jest.fn(
      () =>
        new Promise<void>((resolve) => {
          release = resolve;
        }),
    );

    const guarded = singleFlight(run);

    const first = guarded();
    const second = guarded();
    const third = guarded();

    await expect(second).resolves.toBe('skipped');
    await expect(third).resolves.toBe('skipped');

    release();

    await expect(first).resolves.toBe('ran');
    expect(run).toHaveBeenCalledTimes(1);
  });

  it('blocks a second activation in the same tick, which a state flag could not', async () => {
    const run = jest.fn().mockResolvedValue(undefined);
    const guarded = singleFlight(run);

    const outcomes = await Promise.all([guarded(), guarded(), guarded()]);

    expect(outcomes).toEqual(['ran', 'skipped', 'skipped']);
    expect(run).toHaveBeenCalledTimes(1);
  });

  it('accepts the next activation once the first has finished', async () => {
    const run = jest.fn().mockResolvedValue(undefined);
    const guarded = singleFlight(run);

    await guarded();
    await guarded();

    expect(run).toHaveBeenCalledTimes(2);
  });

  it('releases the guard even when the action throws', async () => {
    const run = jest.fn().mockRejectedValue(new Error('mạng không phản hồi'));
    const guarded = singleFlight(run);

    await expect(guarded()).rejects.toThrow('mạng không phản hồi');

    run.mockResolvedValue(undefined);

    await expect(guarded()).resolves.toBe('ran');
  });

  it('forwards the arguments it was given', async () => {
    const run = jest.fn().mockResolvedValue(undefined);

    await singleFlight(run)('tài sản', 12002000000);

    expect(run).toHaveBeenCalledWith('tài sản', 12002000000);
  });
});
