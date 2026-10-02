import { colors } from '@/ui/tokens';
import { contrastRatio, WCAG_AA } from '../../test/contrast';

describe('contrast helper', () => {
  it('returns 21:1 for black on white', () => {
    expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 2);
  });

  it('returns 1:1 for a colour against itself', () => {
    expect(contrastRatio(colors.primary, colors.primary)).toBeCloseTo(1, 5);
  });

  it('is order independent', () => {
    expect(contrastRatio(colors.ink, colors.canvas)).toBeCloseTo(
      contrastRatio(colors.canvas, colors.ink),
      5,
    );
  });

  it('rejects a value that is not a six-digit hex colour', () => {
    expect(() => contrastRatio('#FFF', '#000000')).toThrow(/six-digit hex/);
  });
});

describe('required normal-text pairs meet WCAG 2.2 AA', () => {
  const pairs: ReadonlyArray<readonly [string, string, string]> = [
    ['ink on canvas', colors.ink, colors.canvas],
    ['ink on surface', colors.ink, colors.surface],
    ['ink on surfaceSubtle', colors.ink, colors.surfaceSubtle],
    ['inkSecondary on canvas', colors.inkSecondary, colors.canvas],
    ['inkSecondary on surface', colors.inkSecondary, colors.surface],
    ['inkSecondary on surfaceSubtle', colors.inkSecondary, colors.surfaceSubtle],
    ['primary on canvas', colors.primary, colors.canvas],
    ['primary on surface', colors.primary, colors.surface],
    ['primary on primaryContainer', colors.primary, colors.primaryContainer],
    ['primaryPressed on surface', colors.primaryPressed, colors.surface],
    ['surface on primary', colors.surface, colors.primary],
    ['surface on primaryPressed', colors.surface, colors.primaryPressed],
    ['surface on error', colors.surface, colors.error],
    ['surface on brandNavy', colors.surface, colors.brandNavy],
    ['success on surface', colors.success, colors.surface],
    ['warning on surface', colors.warning, colors.surface],
    ['error on surface', colors.error, colors.surface],
    ['info on surface', colors.info, colors.surface],
    ['brandNavy on canvas', colors.brandNavy, colors.canvas],
  ];

  it.each(pairs)('%s clears 4.5:1', (_name, foreground, background) => {
    expect(contrastRatio(foreground, background)).toBeGreaterThanOrEqual(WCAG_AA.normalText);
  });
});

describe('required UI-graphic pairs meet the 3:1 threshold', () => {
  const pairs: ReadonlyArray<readonly [string, string, string]> = [
    ['controlBorder on canvas', colors.controlBorder, colors.canvas],
    ['controlBorder on surface', colors.controlBorder, colors.surface],
    ['controlBorder on surfaceSubtle', colors.controlBorder, colors.surfaceSubtle],
    ['focus on canvas', colors.focus, colors.canvas],
    ['focus on surface', colors.focus, colors.surface],
    ['focus on surfaceSubtle', colors.focus, colors.surfaceSubtle],
  ];

  it.each(pairs)('%s clears 3:1', (_name, foreground, background) => {
    expect(contrastRatio(foreground, background)).toBeGreaterThanOrEqual(WCAG_AA.uiGraphic);
  });
});

describe('documented exceptions stay documented', () => {
  it('keeps heritage gold usable for large text but not for normal text', () => {
    expect(contrastRatio(colors.heritageGold, colors.canvas)).toBeGreaterThanOrEqual(
      WCAG_AA.largeText,
    );
    expect(contrastRatio(colors.heritageGold, colors.canvas)).toBeLessThan(WCAG_AA.normalText);
  });

  it('confirms the decorative border is too quiet to be an interactive boundary', () => {
    expect(contrastRatio(colors.border, colors.surface)).toBeLessThan(WCAG_AA.uiGraphic);
  });
});
