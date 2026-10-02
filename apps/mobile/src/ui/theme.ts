import { MD3LightTheme, useTheme } from 'react-native-paper';

import { buildPaperFonts } from './typography';
import { colors } from './tokens';

/**
 * The project token set is the design authority; this Paper theme is an adapter over it.
 * The ten direct MD3 mappings and the seven project extensions are both recorded in
 * docs/design-guidelines.md section 5.1, "React Native Paper MD3 handoff".
 *
 * A Paper upgrade that renames an MD3 role changes this file, not every component.
 */
export const appTheme = {
  ...MD3LightTheme,
  dark: false as const,
  colors: {
    ...MD3LightTheme.colors,

    // The ten approved MD3 mappings.
    background: colors.canvas,
    surface: colors.surface,
    onPrimary: colors.surface,
    onError: colors.surface,
    surfaceVariant: colors.surfaceSubtle,
    onBackground: colors.ink,
    onSurface: colors.ink,
    onSurfaceVariant: colors.inkSecondary,
    primary: colors.primary,
    onPrimaryContainer: colors.primary,
    primaryContainer: colors.primaryContainer,
    error: colors.error,
    /** Decorative dividers and card separation only. */
    outlineVariant: colors.border,
    /** Interactive-control boundaries. Never the quieter decorative border. */
    outline: colors.controlBorder,

    // The seven roles MD3 cannot express faithfully, as typed project extensions.
    brandNavy: colors.brandNavy,
    primaryPressed: colors.primaryPressed,
    heritageGold: colors.heritageGold,
    success: colors.success,
    warning: colors.warning,
    info: colors.info,
    focus: colors.focus,
  },
  fonts: buildPaperFonts(MD3LightTheme.fonts),
};

export type AppTheme = typeof appTheme;

/**
 * Reads the theme with the project extensions typed, so `useAppTheme().colors.focus`
 * compiles and a misspelled role is a typecheck failure rather than a runtime undefined.
 * Components must use this rather than Paper's untyped `useTheme`.
 */
export function useAppTheme(): AppTheme {
  return useTheme<AppTheme>();
}
