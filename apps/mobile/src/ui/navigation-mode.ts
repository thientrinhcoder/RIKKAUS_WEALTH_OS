import { useWindowDimensions } from 'react-native';

import { BREAKPOINTS } from './responsive';

/**
 * The three navigation modes approved in docs/design-guidelines.md section 6.3. This module
 * maps width to a mode and knows nothing about components, so the shell can change its chrome
 * without the breakpoint table moving with it.
 */
export const NAVIGATION_MODES = ['bottom', 'rail', 'sidebar'] as const;

export type NavigationMode = (typeof NAVIGATION_MODES)[number];

export function getNavigationMode(width: number): NavigationMode {
  if (width >= BREAKPOINTS.desktop) {
    return 'sidebar';
  }

  return width >= BREAKPOINTS.tablet ? 'rail' : 'bottom';
}

export function useNavigationMode(): NavigationMode {
  const { width } = useWindowDimensions();
  return getNavigationMode(width);
}
