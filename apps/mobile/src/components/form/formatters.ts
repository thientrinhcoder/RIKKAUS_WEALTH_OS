/**
 * Display formatting and input parsing for financial fields, per docs/design-guidelines.md
 * section 9.
 *
 * The parsed value is the source of truth and is never rounded on the way in: section 9 requires
 * calculation precision to be preserved while display does not imply more precision than a
 * manual valuation supports. Formatting is therefore a display concern only.
 */

const GROUP_SEPARATOR = '.';
const DECIMAL_SEPARATOR = ',';

export interface ParseOptions {
  allowNegative?: boolean;
  allowDecimal?: boolean;
}

/** Applies Vietnamese thousands grouping to the integer part, leaving decimals alone. */
export function formatGrouped(value: string): string {
  if (value === '') {
    return '';
  }

  const negative = value.startsWith('-');
  const unsigned = negative ? value.slice(1) : value;
  const [integerPart, decimalPart] = unsigned.split(DECIMAL_SEPARATOR);

  const grouped = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, GROUP_SEPARATOR);
  const rendered =
    decimalPart === undefined ? grouped : `${grouped}${DECIMAL_SEPARATOR}${decimalPart}`;

  return negative ? `-${rendered}` : rendered;
}

/**
 * Reduces whatever the user typed to a normalized numeric string. Section 8 accepts negative
 * values only where the product contract permits them, so the sign is dropped unless the caller
 * opts in rather than being silently accepted.
 */
export function parseNumericInput(value: string, options: ParseOptions = {}): string {
  const negative = options.allowNegative === true && value.trim().startsWith('-');
  const digitsAndSeparators = value.replace(/[^\d,]/g, '');

  let normalized: string;

  if (options.allowDecimal === true) {
    const [first, ...rest] = digitsAndSeparators.split(DECIMAL_SEPARATOR);
    normalized =
      rest.length === 0 ? first : `${first}${DECIMAL_SEPARATOR}${rest.join('')}`;
  } else {
    normalized = digitsAndSeparators.replace(/,/g, '');
  }

  return negative && normalized !== '' ? `-${normalized}` : normalized;
}

/** Renders an ISO date in the Vietnamese day-first format used across the product. */
export function formatVietnameseDate(isoDate: string): string {
  if (isoDate === '') {
    return '';
  }

  const [year, month, day] = isoDate.split('-');

  return `${day}/${month}/${year}`;
}

export type DateParseResult =
  | { value: string; error?: undefined }
  | { error: 'invalid-format' | 'invalid-day' | 'invalid-month'; value?: undefined };

/**
 * Reads a day-first Vietnamese date and reports an unambiguous stored value, or names the
 * reason it could not. Section 11.1 requires a cause rather than a silent fallback.
 */
export function parseVietnameseDate(input: string): DateParseResult {
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(input.trim());

  if (match === null) {
    return { error: 'invalid-format' };
  }

  const [, day, month, year] = match;
  const monthNumber = Number(month);
  const dayNumber = Number(day);

  if (monthNumber < 1 || monthNumber > 12) {
    return { error: 'invalid-month' };
  }

  const daysInMonth = new Date(Number(year), monthNumber, 0).getDate();

  if (dayNumber < 1 || dayNumber > daysInMonth) {
    return { error: 'invalid-day' };
  }

  return { value: `${year}-${month}-${day}` };
}
