import type { TextStyle } from 'react-native';

/**
 * The eight typography roles from docs/design-guidelines.md section 5.2 and their approved
 * React Native Paper variants.
 */

export const SANS_FONT_STACK = ['IBM Plex Sans', 'system-ui', 'sans-serif'] as const;

/**
 * Restricted by section 5.2 to the wordmark, display titles at 24pt or larger, printable
 * cover titles and the net-worth hero label. No role below defaults to it; a brand surface
 * opts in explicitly.
 */
export const SERIF_FONT_STACK = ['Noto Serif Display', 'Georgia', 'serif'] as const;

export const sansFontFamily = SANS_FONT_STACK.join(', ');
export const serifFontFamily = SERIF_FONT_STACK.join(', ');

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
    fontFamily: sansFontFamily,
    fontSize: 32,
    lineHeight: 40,
    fontWeight: '600',
    letterSpacing: 0,
    fontVariant: TABULAR,
  },
  heading1: {
    paperVariant: 'headlineSmall',
    fontFamily: sansFontFamily,
    fontSize: 24,
    lineHeight: 32,
    fontWeight: '600',
    letterSpacing: 0,
  },
  heading2: {
    paperVariant: 'titleLarge',
    fontFamily: sansFontFamily,
    fontSize: 20,
    lineHeight: 28,
    fontWeight: '600',
    letterSpacing: 0,
  },
  title: {
    paperVariant: 'titleMedium',
    fontFamily: sansFontFamily,
    fontSize: 18,
    lineHeight: 24,
    fontWeight: '600',
    letterSpacing: 0.15,
  },
  body: {
    paperVariant: 'bodyLarge',
    fontFamily: sansFontFamily,
    fontSize: 16,
    lineHeight: 24,
    fontWeight: '400',
    letterSpacing: 0.15,
    fontVariant: TABULAR,
  },
  bodyStrong: {
    paperVariant: 'labelLarge',
    fontFamily: sansFontFamily,
    fontSize: 16,
    lineHeight: 24,
    fontWeight: '600',
    letterSpacing: 0.1,
    fontVariant: TABULAR,
  },
  supporting: {
    paperVariant: 'bodyMedium',
    fontFamily: sansFontFamily,
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '400',
    letterSpacing: 0.25,
    fontVariant: TABULAR,
  },
  label: {
    paperVariant: 'labelSmall',
    fontFamily: sansFontFamily,
    fontSize: 12,
    lineHeight: 16,
    fontWeight: '500',
    letterSpacing: 0.5,
    fontVariant: TABULAR,
  },
};

export type PaperFontStyle = Omit<TypographyRoleStyle, 'paperVariant'>;

export function paperFontOverrides(): Record<PaperVariant, PaperFontStyle> {
  const overrides = {} as Record<PaperVariant, PaperFontStyle>;

  for (const role of Object.values(typographyRoles)) {
    const { paperVariant, ...style } = role;
    overrides[paperVariant] = style;
  }

  return overrides;
}
