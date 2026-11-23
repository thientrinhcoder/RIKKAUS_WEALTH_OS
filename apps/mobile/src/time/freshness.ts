import { daysBetween, todayInProductZone, type IsoDate } from './civil-date';

/**
 * Whether a dated value (a rate's effective date, a valuation date) is still current.
 *
 * The backend makes no freshness judgement ("No stale-rate rule" in api-contract-conventions.md)
 * and the product has not fixed an N-day threshold, so the caller always supplies `maxAgeDays`.
 * There is deliberately no default: a default would be an unapproved business rule.
 */
export type Freshness =
  | { status: 'fresh'; ageDays: number; asOf: IsoDate }
  | { status: 'stale'; ageDays: number; asOf: IsoDate }
  | { status: 'missing' };

export function assessFreshness(
  asOf: IsoDate | null,
  maxAgeDays: number,
  today: IsoDate = todayInProductZone(),
): Freshness {
  if (asOf === null) {
    return { status: 'missing' };
  }

  // The API refuses future dates; a clock skew that produces one still reads as current.
  const ageDays = Math.max(0, daysBetween(asOf, today));

  return { status: ageDays > maxAgeDays ? 'stale' : 'fresh', ageDays, asOf };
}
