/**
 * Exact decimals for money, rates and percentages.
 *
 * `ARCHITECTURE_TECHNOLOGY_DECISIONS.md` forbids binary floating point for any stored or
 * calculated money value, so an amount lives in the client as a canonical decimal string
 * (`-1245000000.50`) and every operation on it is string or BigInt arithmetic. A JavaScript
 * `number` is only ever an input that is converted once, without arithmetic, on arrival.
 *
 * The >2^53 caveat: the API publishes money as bare JSON numbers (api-contract-conventions.md,
 * "money as bare JSON numbers"). `JSON.parse` has already rounded any value with more than about
 * 15 significant digits before this module sees it, and nothing here can recover the lost digits.
 * `NUMERIC(19, 2)` allows 17 integer digits, past that range, so `isPrecisionAtRisk` flags a
 * number that may have been rounded in transit. Realistic Vietnamese portfolio figures sit
 * several orders of magnitude below it. A value that arrives as a string is kept exactly.
 */

declare const decimalBrand: unique symbol;

/** A canonical decimal: optional `-`, digits, optional `.` and digits. No grouping, no exponent. */
export type DecimalString = string & { readonly [decimalBrand]: true };

const CANONICAL = /^-?\d+(\.\d+)?$/;

/** Significant digits an IEEE-754 double is guaranteed to round-trip. */
const SAFE_SIGNIFICANT_DIGITS = 15;

export function isDecimalString(value: string): value is DecimalString {
  return CANONICAL.test(value);
}

/** Trusts a literal already known to be canonical; throws on anything else. */
export function decimal(value: string): DecimalString {
  if (!isDecimalString(value)) {
    throw new Error(`Not a canonical decimal: ${value}`);
  }

  return normalizeSign(value);
}

function normalizeSign(value: string): DecimalString {
  return (/^-0+(\.0+)?$/.test(value) ? value.slice(1) : value) as DecimalString;
}

/**
 * Rewrites `1e-7` or `1.5e+21` as plain digits by moving the decimal point in the string.
 * `String(number)` only produces an exponent below 1e-6 or from 1e21, so this is rare.
 */
function expandExponent(text: string): string {
  const match = /^(-?)(\d+)(?:\.(\d+))?e([+-]\d+)$/i.exec(text);

  if (match === null) {
    return text;
  }

  const [, sign, integerDigits, fractionDigits = '', exponentText] = match;
  const digits = integerDigits + fractionDigits;
  const pointAt = integerDigits.length + Number(exponentText);

  let unsigned: string;

  if (pointAt <= 0) {
    unsigned = `0.${'0'.repeat(-pointAt)}${digits}`;
  } else if (pointAt >= digits.length) {
    unsigned = digits + '0'.repeat(pointAt - digits.length);
  } else {
    unsigned = `${digits.slice(0, pointAt)}.${digits.slice(pointAt)}`;
  }

  return sign + unsigned;
}

/**
 * Converts a value read from an API payload into a decimal string. A number is stringified, not
 * computed on; a string must already be canonical, because the API never sends grouped text.
 * Anything absent or unreadable is `null`, which the UI renders as unavailable — never as zero.
 */
export function fromApiValue(value: unknown): DecimalString | null {
  if (typeof value === 'number') {
    if (!Number.isFinite(value)) {
      return null;
    }

    return normalizeSign(expandExponent(String(value)));
  }

  if (typeof value === 'string' && isDecimalString(value)) {
    return normalizeSign(value);
  }

  return null;
}

/**
 * True when a JSON number carries more significant digits than a double holds exactly, so the
 * value displayed may differ from the one the server stored. See the module comment.
 */
export function isPrecisionAtRisk(value: unknown): boolean {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return false;
  }

  if (Math.abs(value) > Number.MAX_SAFE_INTEGER) {
    return true;
  }

  const significant = expandExponent(String(value)).replace(/[-.]/g, '').replace(/^0+/, '');

  return significant.length > SAFE_SIGNIFICANT_DIGITS;
}

export interface DecimalParts {
  negative: boolean;
  integer: string;
  fraction: string;
}

export function splitDecimal(value: DecimalString): DecimalParts {
  const negative = value.startsWith('-');
  const [integer, fraction = ''] = (negative ? value.slice(1) : value).split('.');

  return { negative, integer, fraction };
}

interface Scaled {
  units: bigint;
  scale: number;
}

const ZERO = BigInt(0);
const ONE = BigInt(1);
const TWO = BigInt(2);
const TEN = BigInt(10);

function toScaled(value: DecimalString): Scaled {
  const { negative, integer, fraction } = splitDecimal(value);
  const units = BigInt(integer + fraction);

  return { units: negative ? -units : units, scale: fraction.length };
}

function fromScaled(units: bigint, scale: number): DecimalString {
  const negative = units < ZERO;
  const digits = (negative ? -units : units).toString().padStart(scale + 1, '0');
  const integer = digits.slice(0, digits.length - scale);
  const fraction = digits.slice(digits.length - scale);
  const unsigned = scale === 0 ? integer : `${integer}.${fraction}`;

  return normalizeSign(negative ? `-${unsigned}` : unsigned);
}

/** Divides by a power of ten, rounding half away from zero (HALF_UP, as the API does). */
function rescale(units: bigint, fromScale: number, toScale: number): bigint {
  if (toScale >= fromScale) {
    return units * TEN ** BigInt(toScale - fromScale);
  }

  const divisor = TEN ** BigInt(fromScale - toScale);
  const negative = units < ZERO;
  const magnitude = negative ? -units : units;
  const quotient = magnitude / divisor;
  const rounded = (magnitude % divisor) * TWO >= divisor ? quotient + ONE : quotient;

  return negative ? -rounded : rounded;
}

/** Rounds to `digits` decimal places, half away from zero, always returning exactly that many. */
export function roundHalfUp(value: DecimalString, digits: number): DecimalString {
  const { units, scale } = toScaled(value);

  return fromScaled(rescale(units, scale, digits), digits);
}

/** Exact product of two decimals, rounded half-up to `digits` places. */
export function multiplyDecimal(
  left: DecimalString,
  right: DecimalString,
  digits: number,
): DecimalString {
  const a = toScaled(left);
  const b = toScaled(right);

  return fromScaled(rescale(a.units * b.units, a.scale + b.scale, digits), digits);
}

export function isZeroDecimal(value: DecimalString): boolean {
  return /^-?0+(\.0+)?$/.test(value);
}
