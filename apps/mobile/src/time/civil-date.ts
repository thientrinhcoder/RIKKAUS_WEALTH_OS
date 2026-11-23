/**
 * Business dates and the product's clock.
 *
 * A valuation date or a rate's effective date is a date-only fact (`2026-09-12`). It is a
 * Vietnamese civil date, not an instant, so it is read and written as text and never passed
 * through `Date` in the device's own time zone, where it could shift by a day.
 *
 * "Today" is the date in `Asia/Ho_Chi_Minh`, the zone the API judges `effectiveDate` against.
 * Vietnam has kept UTC+7 with no daylight saving since 1975, so the offset is applied directly
 * rather than through `Intl` time-zone data, which is not guaranteed on every Hermes build.
 */

export const PRODUCT_TIME_ZONE = 'Asia/Ho_Chi_Minh';
export const PRODUCT_UTC_OFFSET_MINUTES = 7 * 60;
export const PRODUCT_UTC_OFFSET_LABEL = 'GMT+7';

declare const isoDateBrand: unique symbol;

/** A real calendar date written `YYYY-MM-DD`. */
export type IsoDate = string & { readonly [isoDateBrand]: true };

const MS_PER_DAY = 24 * 60 * 60 * 1000;

/** Validates `YYYY-MM-DD` and that the day exists (`2026-02-30` does not). */
export function parseIsoDate(value: unknown): IsoDate | null {
  if (typeof value !== 'string') {
    return null;
  }

  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);

  if (match === null) {
    return null;
  }

  const [, year, month, day] = match.map(Number);
  const check = new Date(Date.UTC(year, month - 1, day));

  if (
    check.getUTCFullYear() !== year ||
    check.getUTCMonth() !== month - 1 ||
    check.getUTCDate() !== day
  ) {
    return null;
  }

  return value as IsoDate;
}

/** `2026-09-12` → `12/09/2026`, identical whatever zone the device is in. */
export function formatDate(date: IsoDate): string {
  const [year, month, day] = date.split('-');

  return `${day}/${month}/${year}`;
}

/** Shifts an instant so its UTC fields read as the wall clock in Vietnam. */
export function toProductWallClock(instant: Date): Date {
  return new Date(instant.getTime() + PRODUCT_UTC_OFFSET_MINUTES * 60 * 1000);
}

function pad(value: number, length = 2): string {
  return String(value).padStart(length, '0');
}

/** The civil date in Vietnam at that instant: `2026-09-12T17:00:00Z` is already the 13th. */
export function civilDateOf(instant: Date): IsoDate {
  const wall = toProductWallClock(instant);

  return `${pad(wall.getUTCFullYear(), 4)}-${pad(wall.getUTCMonth() + 1)}-${pad(
    wall.getUTCDate(),
  )}` as IsoDate;
}

export function todayInProductZone(now: Date = new Date()): IsoDate {
  return civilDateOf(now);
}

/** Whole calendar days from `from` to `to`; negative when `to` is earlier. */
export function daysBetween(from: IsoDate, to: IsoDate): number {
  const [fy, fm, fd] = from.split('-').map(Number);
  const [ty, tm, td] = to.split('-').map(Number);

  return Math.round((Date.UTC(ty, tm - 1, td) - Date.UTC(fy, fm - 1, fd)) / MS_PER_DAY);
}
