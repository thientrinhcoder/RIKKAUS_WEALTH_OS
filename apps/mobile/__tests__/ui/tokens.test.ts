import { colors, radius, sizing, spacing } from '@/ui/tokens';

describe('semantic color roles', () => {
  it('defines every approved role with its baseline value', () => {
    expect(colors).toEqual({
      canvas: '#F7F5EF',
      surface: '#FFFFFF',
      surfaceSubtle: '#EFEBE2',
      ink: '#17212B',
      inkSecondary: '#52606D',
      border: '#D7D2C7',
      controlBorder: '#8B877E',
      brandNavy: '#182B45',
      primary: '#0F5C5A',
      primaryPressed: '#0B4746',
      primaryContainer: '#D9EFEC',
      heritageGold: '#A87428',
      success: '#1E6A45',
      warning: '#8A5A00',
      error: '#B42318',
      info: '#2458A6',
      focus: '#147D79',
    });
  });

  it('exposes exactly the seventeen roles the contract names', () => {
    expect(Object.keys(colors)).toHaveLength(17);
  });
});

describe('spacing rhythm', () => {
  it('uses a 4pt base with 8pt preferred increments', () => {
    expect(spacing.base).toBe(4);
    expect(spacing.increment).toBe(8);
  });

  it('derives every named step from the 4pt base', () => {
    for (const step of [spacing.xs, spacing.sm, spacing.md, spacing.lg, spacing.xl]) {
      expect(step % spacing.base).toBe(0);
    }
  });

  it('scales screen horizontal padding across the three breakpoints', () => {
    expect(spacing.screenHorizontal).toEqual({ phone: 16, tablet: 24, desktop: 32 });
  });

  it('keeps adjacent touch targets at least 8pt apart', () => {
    expect(spacing.adjacentTouchTargetGap).toBe(8);
  });
});

describe('sizing constraints', () => {
  it('meets the platform minimum touch targets', () => {
    expect(sizing.minTouchTarget.ios).toBe(44);
    expect(sizing.minTouchTarget.web).toBe(44);
    expect(sizing.minTouchTarget.android).toBe(48);
  });

  it('keeps form controls at least 48pt tall', () => {
    expect(sizing.minControlHeight).toBeGreaterThanOrEqual(48);
  });

  it('keeps the primary button height inside the approved 52 to 56pt band', () => {
    expect(sizing.primaryButtonHeight).toBeGreaterThanOrEqual(52);
    expect(sizing.primaryButtonHeight).toBeLessThanOrEqual(56);
  });

  it('bounds form and prose content to the approved 640 to 720px measure', () => {
    expect(sizing.contentMaxWidth).toBeGreaterThanOrEqual(640);
    expect(sizing.contentMaxWidth).toBeLessThanOrEqual(720);
  });

  it('bounds the shell content area to 1120px on desktop', () => {
    expect(sizing.shellContentMaxWidth).toBe(1120);
  });
});

describe('radius scale', () => {
  it('keeps each surface radius inside its approved band', () => {
    expect(radius.card).toBeGreaterThanOrEqual(12);
    expect(radius.card).toBeLessThanOrEqual(16);
    expect(radius.input).toBeGreaterThanOrEqual(10);
    expect(radius.input).toBeLessThanOrEqual(12);
    expect(radius.sheet).toBeGreaterThanOrEqual(20);
    expect(radius.sheet).toBeLessThanOrEqual(24);
    expect(radius.dialog).toBeGreaterThanOrEqual(20);
    expect(radius.dialog).toBeLessThanOrEqual(24);
  });
});
