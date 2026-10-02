import { useWindowDimensions } from 'react-native';

import { sizing, spacing } from './tokens';

/** Long forms and explanatory prose, section 5.3. */
export const CONTENT_MAX_WIDTH = sizing.contentMaxWidth;

/** Retained name for existing callers; the tablet breakpoint of the section 6.3 table. */
export const WIDE_LAYOUT_BREAKPOINT = 768;

export const BREAKPOINTS = {
  tablet: 768,
  desktop: 1024,
} as const;

export type Breakpoint = 'phone' | 'tablet' | 'desktop';

export type ScreenHorizontalPadding =
  (typeof spacing.screenHorizontal)[keyof typeof spacing.screenHorizontal];

export interface ResponsiveLayout {
  horizontalPadding: ScreenHorizontalPadding;
}

export function getBreakpoint(width: number): Breakpoint {
  if (width >= BREAKPOINTS.desktop) {
    return 'desktop';
  }

  if (width >= BREAKPOINTS.tablet) {
    return 'tablet';
  }

  return 'phone';
}

export function getResponsiveLayout(width: number): ResponsiveLayout {
  return {
    horizontalPadding: spacing.screenHorizontal[getBreakpoint(width)],
  };
}

export function useResponsiveLayout(): ResponsiveLayout {
  const { width } = useWindowDimensions();
  return getResponsiveLayout(width);
}

export function useBreakpoint(): Breakpoint {
  const { width } = useWindowDimensions();
  return getBreakpoint(width);
}
