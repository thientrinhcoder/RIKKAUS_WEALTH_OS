import {
  formatGrouped,
  formatVietnameseDate,
  parseNumericInput,
  parseVietnameseDate,
} from '@/components/form/formatters';

describe('grouping for display', () => {
  it('groups thousands the Vietnamese way', () => {
    expect(formatGrouped('12002000000')).toBe('12.002.000.000');
    expect(formatGrouped('1250750000')).toBe('1.250.750.000');
    expect(formatGrouped('999')).toBe('999');
    expect(formatGrouped('1000')).toBe('1.000');
  });

  it('keeps the decimal part ungrouped and uses the locale separator', () => {
    expect(formatGrouped('1234,5')).toBe('1.234,5');
    expect(formatGrouped('0,25')).toBe('0,25');
  });

  it('keeps a negative sign in front of the grouping', () => {
    expect(formatGrouped('-1250750')).toBe('-1.250.750');
  });

  it('returns an empty string untouched rather than inventing a zero', () => {
    expect(formatGrouped('')).toBe('');
  });
});

describe('parsing what the user typed', () => {
  it('keeps digits and drops anything that is not one', () => {
    expect(parseNumericInput('12.002.000.000')).toBe('12002000000');
    expect(parseNumericInput('1 250 750 ₫')).toBe('1250750');
  });

  it('rejects a negative value unless the caller allows it', () => {
    expect(parseNumericInput('-500')).toBe('500');
    expect(parseNumericInput('-500', { allowNegative: true })).toBe('-500');
  });

  it('keeps a single decimal separator only when decimals are allowed', () => {
    expect(parseNumericInput('12,5', { allowDecimal: true })).toBe('12,5');
    expect(parseNumericInput('12,5')).toBe('125');
    expect(parseNumericInput('12,5,7', { allowDecimal: true })).toBe('12,57');
  });

  it('preserves precision instead of rounding on the way in', () => {
    expect(parseNumericInput('1234567,891', { allowDecimal: true })).toBe('1234567,891');
  });
});

describe('dates', () => {
  it('displays an unambiguous stored value in the Vietnamese format', () => {
    expect(formatVietnameseDate('2026-09-12')).toBe('12/09/2026');
    expect(formatVietnameseDate('2026-01-05')).toBe('05/01/2026');
  });

  it('reports back an unambiguous stored value', () => {
    expect(parseVietnameseDate('12/09/2026')).toEqual({ value: '2026-09-12' });
    expect(parseVietnameseDate('05/01/2026')).toEqual({ value: '2026-01-05' });
  });

  it('states a cause rather than falling back silently', () => {
    expect(parseVietnameseDate('32/09/2026')).toEqual({ error: 'invalid-day' });
    expect(parseVietnameseDate('12/13/2026')).toEqual({ error: 'invalid-month' });
    expect(parseVietnameseDate('hôm qua')).toEqual({ error: 'invalid-format' });
    expect(parseVietnameseDate('29/02/2026')).toEqual({ error: 'invalid-day' });
  });

  it('accepts a real leap day', () => {
    expect(parseVietnameseDate('29/02/2028')).toEqual({ value: '2028-02-29' });
  });

  it('returns an empty display for an absent value rather than a placeholder date', () => {
    expect(formatVietnameseDate('')).toBe('');
  });
});
