import { elevation } from '@/ui/elevation';
import { colors } from '@/ui/tokens';
import { appTheme } from '@/ui/theme';

describe('Paper MD3 adapter', () => {
  it('applies the ten approved MD3 mappings', () => {
    const { colors: themeColors } = appTheme;

    expect(themeColors.background).toBe(colors.canvas);

    expect(themeColors.surface).toBe(colors.surface);
    expect(themeColors.onPrimary).toBe(colors.surface);
    expect(themeColors.onError).toBe(colors.surface);

    expect(themeColors.surfaceVariant).toBe(colors.surfaceSubtle);

    expect(themeColors.onBackground).toBe(colors.ink);
    expect(themeColors.onSurface).toBe(colors.ink);

    expect(themeColors.onSurfaceVariant).toBe(colors.inkSecondary);

    expect(themeColors.primary).toBe(colors.primary);
    expect(themeColors.onPrimaryContainer).toBe(colors.primary);

    expect(themeColors.primaryContainer).toBe(colors.primaryContainer);
    expect(themeColors.error).toBe(colors.error);

    expect(themeColors.outlineVariant).toBe(colors.border);
    expect(themeColors.outline).toBe(colors.controlBorder);
  });

  it('exposes the seven roles MD3 cannot express as project extensions', () => {
    const { colors: themeColors } = appTheme;

    expect(themeColors.brandNavy).toBe(colors.brandNavy);
    expect(themeColors.primaryPressed).toBe(colors.primaryPressed);
    expect(themeColors.heritageGold).toBe(colors.heritageGold);
    expect(themeColors.success).toBe(colors.success);
    expect(themeColors.warning).toBe(colors.warning);
    expect(themeColors.info).toBe(colors.info);
    expect(themeColors.focus).toBe(colors.focus);
  });

  it('stays light-only for the MVP baseline', () => {
    expect(appTheme.dark).toBe(false);
  });

  it('never routes a decorative divider colour onto an interactive boundary', () => {
    expect(appTheme.colors.outline).not.toBe(colors.border);
  });
});

describe('elevation recipes', () => {
  it('provides flat, raised card and modal recipes at the approved Paper levels', () => {
    expect(elevation.flat.level).toBe(0);
    expect(elevation.raisedCard.level).toBe(1);
    expect(elevation.modal.level).toBe(3);
  });

  it('carries the documented web shadow equivalents and the modal scrim', () => {
    expect(elevation.flat.webShadow).toBeNull();
    expect(elevation.raisedCard.webShadow).toBe('0 2px 8px rgba(24, 43, 69, 0.12)');
    expect(elevation.modal.webShadow).toBe('0 12px 32px rgba(24, 43, 69, 0.22)');
    expect(elevation.modal.scrim).toBe('rgba(23, 33, 43, 0.56)');
  });
});
