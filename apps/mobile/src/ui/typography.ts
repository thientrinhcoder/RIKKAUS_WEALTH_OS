import type { TextStyle } from 'react-native';

import { SANS_FAMILY_BY_WEIGHT, type SansWeight } from './fonts';

/**
 * The eight typography roles from docs/design-guidelines.md section 5.2 and their approved
 * React Native Paper variants.
 *
 * The families below are the names expo-font registers the bundled files under, so a weight is
 * selected by naming its family. fontWeight is still declared because the web target honours it
 * and because it documents the approved weight next to the role that carries it.
 */

export type TypographyRole =
  | 'display'
  | 'heading1'
  | 'heading2'
  | 'title'
  | 'body'
  | 'bodyStrong'
  | 'supporting'
  | 'label';

/**
 * Paper's typescale accepts only these weights, a narrower set than React Native's
 * TextStyle. Declaring it here keeps the theme assignable to Paper's ThemeProp.
 */
export type FontWeight =
  | 'normal'
  | 'bold'
  | '100'
  | '200'
  | '300'
  | '400'
  | '500'
  | '600'
  | '700'
  | '800'
  | '900';

export type PaperVariant =
  | 'displaySmall'
  | 'headlineSmall'
  | 'titleLarge'
  | 'titleMedium'
  | 'bodyLarge'
  | 'labelLarge'
  | 'bodyMedium'
  | 'labelSmall';

export interface TypographyRoleStyle {
  paperVariant: PaperVariant;
  fontFamily: string;
  fontSize: number;
  lineHeight: number;
  fontWeight: FontWeight;
  letterSpacing: number;
  fontVariant?: TextStyle['fontVariant'];
}

/**
 * Roles that can carry an amount, percentage, date, rate or chart label, and therefore need
 * tabular figures so digits stay aligned down a column of money.
 */
export const NUMERIC_TYPOGRAPHY_ROLES = [
  'display',
  'body',
  'bodyStrong',
  'supporting',
  'label',
] as const satisfies readonly TypographyRole[];

const TABULAR: TextStyle['fontVariant'] = ['tabular-nums'];

export const typographyRoles: Record<TypographyRole, TypographyRoleStyle> = {
  display: {
    paperVariant: 'displaySmall',
    fontFamily: SANS_FAMILY_BY_WEIGHT['600'],
    fontSize: 32,
    lineHeight: 40,
    fontWeight: '600',
    letterSpacing: 0,
    fontVariant: TABULAR,
  },
  heading1: {
    paperVariant: 'headlineSmall',
    fontFamily: SANS_FAMILY_BY_WEIGHT['600'],
    fontSize: 24,
    lineHeight: 32,
    fontWeight: '600',
    letterSpacing: 0,
  },
  heading2: {
    paperVariant: 'titleLarge',
    fontFamily: SANS_FAMILY_BY_WEIGHT['600'],
    fontSize: 20,
    lineHeight: 28,
    fontWeight: '600',
    letterSpacing: 0,
  },
  title: {
    paperVariant: 'titleMedium',
    fontFamily: SANS_FAMILY_BY_WEIGHT['600'],
    fontSize: 18,
    lineHeight: 24,
    fontWeight: '600',
    letterSpacing: 0.15,
  },
  body: {
    paperVariant: 'bodyLarge',
    fontFamily: SANS_FAMILY_BY_WEIGHT['400'],
    fontSize: 16,
    lineHeight: 24,
    fontWeight: '400',
    letterSpacing: 0.15,
    fontVariant: TABULAR,
  },
  bodyStrong: {
    paperVariant: 'labelLarge',
    fontFamily: SANS_FAMILY_BY_WEIGHT['600'],
    fontSize: 16,
    lineHeight: 24,
    fontWeight: '600',
    letterSpacing: 0.1,
    fontVariant: TABULAR,
  },
  supporting: {
    paperVariant: 'bodyMedium',
    fontFamily: SANS_FAMILY_BY_WEIGHT['400'],
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '400',
    letterSpacing: 0.25,
    fontVariant: TABULAR,
  },
  label: {
    paperVariant: 'labelSmall',
    fontFamily: SANS_FAMILY_BY_WEIGHT['500'],
    fontSize: 12,
    lineHeight: 16,
    fontWeight: '500',
    letterSpacing: 0.5,
    fontVariant: TABULAR,
  },
};

export type PaperFontStyle = Omit<TypographyRoleStyle, 'paperVariant'>;

function nearestSansWeight(weight: TextStyle['fontWeight']): SansWeight {
  const numeric = Number(weight ?? '400');

  if (weight === 'bold' || numeric >= 600) {
    return '600';
  }

  return numeric >= 500 ? '500' : '400';
}

/**
 * Builds the Paper typescale.
 *
 * Paper ships around fifteen variants and the contract approves eight. The eight get their exact
 * approved size, line height, weight and tabular figures. Every remaining variant keeps its MD3
 * metrics but is moved onto the bundled family, because a component reaching for an unapproved
 * variant would otherwise render in Paper's default Roboto and quietly leave the type system.
 */
export function buildPaperFonts<T extends Record<string, unknown>>(
  base: T,
): T & Record<PaperVariant, PaperFontStyle> {
  const fonts = { ...base } as Record<string, unknown>;

  for (const [variant, style] of Object.entries(base)) {
    if (style === null || typeof style !== 'object') {
      continue;
    }

    const textStyle = style as TextStyle;

    fonts[variant] = {
      ...textStyle,
      fontFamily: SANS_FAMILY_BY_WEIGHT[nearestSansWeight(textStyle.fontWeight)],
    };
  }

  for (const role of Object.values(typographyRoles)) {
    const { paperVariant, ...style } = role;
    fonts[paperVariant] = style;
  }

  return fonts as T & Record<PaperVariant, PaperFontStyle>;
}
