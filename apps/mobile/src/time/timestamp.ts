import {
  PRODUCT_UTC_OFFSET_LABEL,
  civilDateOf,
  daysBetween,
  formatDate,
  toProductWallClock,
} from './civil-date';

/**
 * System timestamps (`updatedAt`): stored and sent in UTC, shown in Vietnamese time with the
 * offset written out, so `09:30, 13/09/2026 (GMT+7)` cannot be mistaken for a business date.
 * Relative time ("Khoảng 2 giờ trước") supplements the absolute value and never replaces it.
 */

/**
 * An ISO-8601 instant with an explicit `Z` or offset. Fractions beyond milliseconds — the API
 * sends microseconds — are truncated first, because not every JavaScript engine parses them.
 */
const INSTANT = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?)(?:\.(\d+))?(Z|[+-]\d{2}:\d{2})$/;

export function parseInstant(value: unknown): Date | null {
  if (typeof value !== 'string') {
    return null;
  }

  const match = INSTANT.exec(value);

  if (match === null) {
    return null;
  }

  const [, base, fraction, zone] = match;
  const millis = fraction === undefined ? '' : `.${fraction.slice(0, 3).padEnd(3, '0')}`;
  const instant = new Date(`${base}${millis}${zone}`);

  return Number.isNaN(instant.getTime()) ? null : instant;
}

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

/** `2026-09-13T02:30:00Z` → `09:30, 13/09/2026 (GMT+7)`. */
export function formatTimestamp(instant: Date): string {
  const wall = toProductWallClock(instant);

  return `${pad(wall.getUTCHours())}:${pad(wall.getUTCMinutes())}, ${formatDate(
    civilDateOf(instant),
  )} (${PRODUCT_UTC_OFFSET_LABEL})`;
}

/** The UTC source value, for an audit disclosure: `2026-09-13T02:30:00Z`. */
export function formatUtcSource(instant: Date): string {
  return instant.toISOString().replace('.000Z', 'Z');
}

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;

/**
 * A short reading aid beside an absolute time. Past a month it returns `null`: by then only the
 * absolute date is useful.
 */
export function formatRelative(instant: Date, now: Date = new Date()): string | null {
  const elapsed = now.getTime() - instant.getTime();

  if (elapsed < 0) {
    return null;
  }

  if (elapsed < MINUTE) {
    return 'Vừa xong';
  }

  if (elapsed < HOUR) {
    return `${Math.floor(elapsed / MINUTE)} phút trước`;
  }

  const days = daysBetween(civilDateOf(instant), civilDateOf(now));

  if (days === 0) {
    return `Khoảng ${Math.floor(elapsed / HOUR)} giờ trước`;
  }

  if (days === 1) {
    return 'Hôm qua';
  }

  return days <= 30 ? `${days} ngày trước` : null;
}
