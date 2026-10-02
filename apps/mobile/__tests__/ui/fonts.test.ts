import { fontAssets, SANS_FAMILY_BY_WEIGHT, SERIF_FAMILY, sansFamilyFor } from '@/ui/fonts';
import { appTheme } from '@/ui/theme';
import { NUMERIC_TYPOGRAPHY_ROLES, typographyRoles } from '@/ui/typography';

describe('bundled font assets', () => {
  it('resolves every bundled family to a real asset rather than undefined', () => {
    for (const [family, asset] of Object.entries(fontAssets)) {
      expect(asset).toBeDefined();
      expect(family).toMatch(/^(IBMPlexSans|NotoSerifDisplay)_/);
    }
  });

  it('bundles exactly the three sans weights the approved roles use, plus the brand serif', () => {
    expect(Object.keys(fontAssets)).toHaveLength(4);
    expect(SANS_FAMILY_BY_WEIGHT).toEqual({
      '400': 'IBMPlexSans_400Regular',
      '500': 'IBMPlexSans_500Medium',
      '600': 'IBMPlexSans_600SemiBold',
    });
    expect(SERIF_FAMILY).toBe('NotoSerifDisplay_600SemiBold');
  });

  it('registers every family a typography role asks for', () => {
    for (const role of Object.values(typographyRoles)) {
      expect(Object.keys(fontAssets)).toContain(role.fontFamily);
    }
  });
});

describe('weight selection', () => {
  it('names the family for a weight, because expo-font cannot switch weight by fontWeight', () => {
    expect(sansFamilyFor('400')).toBe('IBMPlexSans_400Regular');
    expect(sansFamilyFor('500')).toBe('IBMPlexSans_500Medium');
    expect(sansFamilyFor('600')).toBe('IBMPlexSans_600SemiBold');
  });

  it('gives every role the family matching its own declared weight', () => {
    for (const role of Object.values(typographyRoles)) {
      expect(role.fontFamily).toBe(sansFamilyFor(role.fontWeight as never));
    }
  });

  it('keeps every critical value on the bundled sans, never the brand serif', () => {
    for (const role of NUMERIC_TYPOGRAPHY_ROLES) {
      expect(typographyRoles[role].fontFamily).not.toBe(SERIF_FAMILY);
    }
  });

  it('keeps the brand serif out of every typography role', () => {
    const serifRoles = Object.entries(typographyRoles)
      .filter(([, role]) => role.fontFamily === SERIF_FAMILY)
      .map(([name]) => name);

    expect(serifRoles).toEqual([]);
  });
});

describe('no text can escape the bundled family', () => {
  const bundled = Object.keys(fontAssets);

  it('covers every Paper typescale variant, not only the eight approved roles', () => {
    const variants = Object.entries(appTheme.fonts).filter(
      ([, style]) => style !== null && typeof style === 'object',
    );

    expect(variants.length).toBeGreaterThan(typographyRoles ? 8 : 0);

    for (const [variant, style] of variants) {
      const family = (style as { fontFamily?: string }).fontFamily;

      expect({ variant, family }).toEqual({ variant, family: expect.any(String) });
      expect(bundled).toContain(family);
    }
  });

  it('never leaves a variant on a default platform family', () => {
    for (const [, style] of Object.entries(appTheme.fonts)) {
      if (style === null || typeof style !== 'object') {
        continue;
      }

      const family = (style as { fontFamily?: string }).fontFamily ?? '';

      expect(family).not.toMatch(/Roboto|System|Helvetica|Arial|sans-serif/i);
    }
  });
});
