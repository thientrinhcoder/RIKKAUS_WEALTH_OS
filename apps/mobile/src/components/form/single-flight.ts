/**
 * Runs an async action at most once at a time.
 *
 * Section 8 requires duplicate-submission prevention. A state flag cannot provide it: two
 * activations in the same tick both read the pre-update value and both get through. The flag
 * therefore lives in a closure, which updates synchronously.
 */
export type SingleFlightOutcome = 'ran' | 'skipped';

export function singleFlight<Args extends unknown[]>(
  run: (...args: Args) => Promise<void> | void,
): (...args: Args) => Promise<SingleFlightOutcome> {
  let running = false;

  return async (...args: Args) => {
    if (running) {
      return 'skipped';
    }

    running = true;

    try {
      await run(...args);
      return 'ran';
    } finally {
      running = false;
    }
  };
}
