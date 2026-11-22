import type { DecimalString } from './decimal';

/**
 * Reads a number the user typed in Vietnamese format and returns a canonical decimal string, or
 * names the reason it could not. Section 11.1 requires the cause rather than a silent fallback.
 *
 * `.` is always a thousands separator and `,` always the decimal separator, so `1.250` is one
 * thousand two hundred and fifty, never one and a quarter.
 */

export type DecimalParseError =
  | 'empty'
  | 'invalid-format'
  | 'negative-not-allowed'
  | 'too-many-decimals'
  | 'too-large';

export type DecimalParseResult =
  | { value: DecimalString; error?: undefined }
  | { error: DecimalParseError; value?: undefined };

export interface DecimalParseOptions {
  /** Section 8: negatives only where the product contract permits them. */
  allowNegative?: boolean;
  /** `NUMERIC(19, 2)` money takes 2; `NUMERIC(19, 6)` rates take 6. */
  maxFractionDigits?: number;
  /** `NUMERIC(19, 2)` holds 17 integer digits; `NUMERIC(19, 6)` holds 13. */
  maxIntegerDigits?: number;
}

/** Inline messages for each failure, written for the field the user is looking at. */
export const DECIMAL_PARSE_MESSAGES: Record<DecimalParseError, string> = {
  empty: 'Nhập số tiền.',
  'invalid-format': 'Số không hợp lệ. Ví dụ: 12.002.000.000 hoặc 1.250,50.',
  'negative-not-allowed': 'Giá trị này không được âm.',
  'too-many-decimals': 'Quá nhiều chữ số thập phân.',
  'too-large': 'Giá trị vượt quá giới hạn cho phép.',
};

/** Money as the API stores it: `NUMERIC(19, 2)`. */
export const MONEY_LIMITS = { maxFractionDigits: 2, maxIntegerDigits: 17 } as const;

/** An exchange rate as the API stores it: `NUMERIC(19, 6)`. */
export const RATE_LIMITS = { maxFractionDigits: 6, maxIntegerDigits: 13 } as const;

/** Grouped in threes (`12.002.000`) or plain digits, then an optional `,` fraction. */
const VIETNAMESE_NUMBER = /^(\d{1,3}(?:\.\d{3})+|\d+)(?:,(\d+))?$/;

/** Units and spacing a user may paste along with the number. */
const IGNORED = /[\s  ₫]|vnd|usd|đ/gi;

export function parseVietnameseDecimal(
  input: string,
  options: DecimalParseOptions = {},
): DecimalParseResult {
  const {
    allowNegative = false,
    maxFractionDigits = MONEY_LIMITS.maxFractionDigits,
    maxIntegerDigits = MONEY_LIMITS.maxIntegerDigits,
  } = options;

  let text = input.replace(IGNORED, '');

  if (text === '') {
    return { error: 'empty' };
  }

  const negative = text.startsWith('-') || text.startsWith('−');

  if (negative) {
    text = text.slice(1);
  }

  const match = VIETNAMESE_NUMBER.exec(text);

  if (match === null) {
    return { error: 'invalid-format' };
  }

  const [, groupedInteger, fraction = ''] = match;
  const integer = groupedInteger.replace(/\./g, '').replace(/^0+(?=\d)/, '');

  if (negative && !allowNegative) {
    return { error: 'negative-not-allowed' };
  }

  if (fraction.length > maxFractionDigits) {
    return { error: 'too-many-decimals' };
  }

  if (integer.length > maxIntegerDigits) {
    return { error: 'too-large' };
  }

  const unsigned = fraction === '' ? integer : `${integer}.${fraction}`;
  const isZero = /^0+(\.0+)?$/.test(unsigned);

  return { value: (negative && !isZero ? `-${unsigned}` : unsigned) as DecimalString };
}

/**
 * The form kit keeps a field's value as the user's normalized digits with a `,` decimal
 * (`1250,50`, see `parseNumericInput`). This turns that into the canonical decimal the API takes,
 * or `null` while the field is empty or mid-entry (`-`).
 */
export function fromFormValue(normalized: string): DecimalString | null {
  const text = normalized.replace(',', '.').replace(/\.$/, '');

  return /^-?\d+(\.\d+)?$/.test(text) ? (text as DecimalString) : null;
}

/** The reverse of `fromFormValue`, for seeding a form field from an API value. */
export function toFormValue(value: DecimalString): string {
  return value.replace('.', ',');
}
