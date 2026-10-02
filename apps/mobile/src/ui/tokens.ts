/**
 * Quiet Heritage foundation tokens, approved in issue #36 and recorded in
 * docs/design-guidelines.md sections 5.1 and 5.3.
 *
 * These are the only place a raw value may appear. Components consume semantic roles.
 * Before changing a colour, re-run __tests__/ui/contrast.test.ts: several pairs sit close
 * to their WCAG threshold, and controlBorder on surfaceSubtle clears 3:1 by 0.01.
 */

export const colors = {
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
} as const;

export type ColorRole = keyof typeof colors;

/**
 * Roles MD3 has no faithful equivalent for. They ship as project-theme extensions so the
 * approved meaning survives a Paper upgrade.
 */
export const PROJECT_EXTENSION_ROLES = [
  'brandNavy',
  'primaryPressed',
  'heritageGold',
  'success',
  'warning',
  'info',
  'focus',
] as const satisfies readonly ColorRole[];

export const spacing = {
  base: 4,
  increment: 8,
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  screenHorizontal: {
    phone: 16,
    tablet: 24,
    desktop: 32,
  },
  section: { min: 24, max: 32 },
  relatedControls: { min: 8, max: 16 },
  adjacentTouchTargetGap: 8,
} as const;

export const sizing = {
  minTouchTarget: {
    ios: 44,
    web: 44,
    android: 48,
  },
  minControlHeight: 48,
  primaryButtonHeight: 52,
  /** Long forms and explanatory prose, section 5.3. */
  contentMaxWidth: 720,
  /** The shell's bounded content area at 1024px and above, section 5.3. */
  shellContentMaxWidth: 1120,
} as const;

export const radius = {
  card: 12,
  input: 10,
  sheet: 20,
  dialog: 20,
} as const;
