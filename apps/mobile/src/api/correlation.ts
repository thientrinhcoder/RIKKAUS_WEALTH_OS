export const CORRELATION_HEADER = 'X-Correlation-Id';

/** The server echoes a value matching this pattern and replaces anything else with a UUID. */
export const CORRELATION_ID_PATTERN = /^[A-Za-z0-9-]{8,64}$/;

/**
 * A UUID v4 when the platform offers `crypto.randomUUID` (web, recent Hermes), otherwise a
 * random hex identifier in the same shape. Either matches the echo pattern. It only has to be
 * unique enough to find one request in the logs; it is not a security token.
 */
export function createCorrelationId(): string {
  const cryptoApi = globalThis.crypto as Crypto | undefined;

  if (typeof cryptoApi?.randomUUID === 'function') {
    return cryptoApi.randomUUID();
  }

  const hex = (length: number) =>
    Array.from({ length }, () => Math.floor(Math.random() * 16).toString(16)).join('');

  return `${hex(8)}-${hex(4)}-${hex(4)}-${hex(4)}-${hex(12)}`;
}
