import { useWindowDimensions } from 'react-native';

export const CONTENT_MAX_WIDTH = 720;
export const WIDE_LAYOUT_BREAKPOINT = 768;

export interface ResponsiveLayout {
  horizontalPadding: 16 | 24;
}

export function getResponsiveLayout(width: number): ResponsiveLayout {
  return {
    horizontalPadding: width >= WIDE_LAYOUT_BREAKPOINT ? 24 : 16,
  };
}

export function useResponsiveLayout(): ResponsiveLayout {
  const { width } = useWindowDimensions();
  return getResponsiveLayout(width);
}
