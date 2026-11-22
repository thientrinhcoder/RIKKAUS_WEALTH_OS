import { isZeroDecimal, roundHalfUp, splitDecimal, type DecimalString } from './decimal';

/**
 * Vietnamese number display, per docs/design-guidelines.md section 9: `.` groups thousands and
 * `,` separates decimals — `12.002.000.000`, `1.250,50`, `18,4`.
 */
export const GROUP_SEPARATOR = '.';
export const DECIMAL_SEPARATOR = ',';

/** Groups a run of integer digits in threes: `12002000000` → `12.002.000.000`. */
export function groupThousands(integerDigits: string): string {
  return integerDigits.replace(/\B(?=(\d{3})+(?!\d))/g, GROUP_SEPARATOR);
}

export interface DecimalDisplayOptions {
  /** Places kept after half-up rounding. Display only; the stored value is never rounded. */
  maxFractionDigits: number;
  /**
   * `trim` drops trailing zeros (`25.100,5`, `18,4`, `100`). `all-or-none` shows either no
   * decimals or exactly `maxFractionDigits` (`20.000`, `1.250,50`), the convention for USD.
   */
  fractionStyle: 'trim' | 'all-or-none';
}

/**
 * Formats a decimal for reading. The sign is dropped when the rounded value is zero, so `-0,4 ₫`
 * displays as `0 ₫` rather than a negative zero.
 */
export function formatDecimal(value: DecimalString, options: DecimalDisplayOptions): string {
  const rounded = roundHalfUp(value, options.maxFractionDigits);
  const { negative, integer, fraction } = splitDecimal(rounded);

  let shownFraction = fraction;

  if (options.fractionStyle === 'trim') {
    shownFraction = fraction.replace(/0+$/, '');
  } else if (/^0*$/.test(fraction)) {
    shownFraction = '';
  }

  const body =
    shownFraction === ''
      ? groupThousands(integer)
      : `${groupThousands(integer)}${DECIMAL_SEPARATOR}${shownFraction}`;

  return negative && !isZeroDecimal(rounded) ? `-${body}` : body;
}

/** True when `formatDecimal` would show a minus sign. */
export function displaysNegative(value: DecimalString, maxFractionDigits: number): boolean {
  const rounded = roundHalfUp(value, maxFractionDigits);

  return rounded.startsWith('-') && !isZeroDecimal(rounded);
}
