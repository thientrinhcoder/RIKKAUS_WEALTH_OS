import {
  NUMERIC_TYPOGRAPHY_ROLES,
  SANS_FONT_STACK,
  SERIF_FONT_STACK,
  typographyRoles,
  type TypographyRole,
} from '@/ui/typography';
import { appTheme } from '@/ui/theme';

const EXPECTED: Record<TypographyRole, { variant: string; size: number; lineHeight: number; weight: string }> = {
  display: { variant: 'displaySmall', size: 32, lineHeight: 40, weight: '600' },
  heading1: { variant: 'headlineSmall', size: 24, lineHeight: 32, weight: '600' },
  heading2: { variant: 'titleLarge', size: 20, lineHeight: 28, weight: '600' },
  title: { variant: 'titleMedium', size: 18, lineHeight: 24, weight: '600' },
  body: { variant: 'bodyLarge', size: 16, lineHeight: 24, weight: '400' },
  bodyStrong: { variant: 'labelLarge', size: 16, lineHeight: 24, weight: '600' },
  supporting: { variant: 'bodyMedium', size: 14, lineHeight: 20, weight: '400' },
  label: { variant: 'labelSmall', size: 12, lineHeight: 16, weight: '500' },
};

describe('typography roles', () => {
  it('covers the eight approved roles', () => {
    expect(Object.keys(typographyRoles).sort()).toEqual(Object.keys(EXPECTED).sort());
  });

  it.each(Object.entries(EXPECTED))(
    'maps %s to its approved Paper variant, size, line height and weight',
    (role, expected) => {
      const actual = typographyRoles[role as TypographyRole];

      expect(actual.paperVariant).toBe(expected.variant);
      expect(actual.fontSize).toBe(expected.size);
      expect(actual.lineHeight).toBe(expected.lineHeight);
      expect(actual.fontWeight).toBe(expected.weight);
    },
  );

  it('reaches the assembled Paper theme with the same values', () => {
    for (const [, expected] of Object.entries(EXPECTED)) {
      const paperFont = appTheme.fonts[expected.variant as keyof typeof appTheme.fonts];

      expect(paperFont).toMatchObject({
        fontSize: expected.size,
        lineHeight: expected.lineHeight,
        fontWeight: expected.weight,
      });
    }
  });
});

describe('tabular figures', () => {
  it('requests tabular numerals on every role that can carry a number', () => {
    expect(NUMERIC_TYPOGRAPHY_ROLES).toEqual(
      expect.arrayContaining(['display', 'body', 'bodyStrong', 'supporting', 'label']),
    );

    for (const role of NUMERIC_TYPOGRAPHY_ROLES) {
      expect(typographyRoles[role].fontVariant).toEqual(['tabular-nums']);
    }
  });

  it('does not force tabular numerals on prose-only roles', () => {
    for (const role of ['heading1', 'heading2', 'title'] as const) {
      expect(typographyRoles[role].fontVariant).toBeUndefined();
    }
  });
});

describe('font families', () => {
  it('uses IBM Plex Sans with a system sans fallback', () => {
    expect(SANS_FONT_STACK[0]).toBe('IBM Plex Sans');
    expect(SANS_FONT_STACK.length).toBeGreaterThan(1);
    expect(SANS_FONT_STACK[SANS_FONT_STACK.length - 1]).toBe('sans-serif');
  });

  it('keeps Noto Serif Display reachable only through the display and brand roles', () => {
    expect(SERIF_FONT_STACK[0]).toBe('Noto Serif Display');
    expect(SERIF_FONT_STACK[SERIF_FONT_STACK.length - 1]).toBe('serif');

    const serifRoles = Object.entries(typographyRoles)
      .filter(([, role]) => role.fontFamily === SERIF_FONT_STACK.join(', '))
      .map(([name]) => name);

    expect(serifRoles).toEqual([]);
  });

  it('keeps every critical value on the sans stack', () => {
    for (const role of NUMERIC_TYPOGRAPHY_ROLES) {
      expect(typographyRoles[role].fontFamily).toBe(SANS_FONT_STACK.join(', '));
    }
  });
});
