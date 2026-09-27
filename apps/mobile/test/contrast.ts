/**
 * WCAG 2.2 relative luminance and contrast ratio, used by the token verification specs.
 * Test-only helper: no production code consumes it.
 */

function channelLuminance(value: number): number {
  const channel = value / 255;
  return channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4;
}

export function relativeLuminance(hex: string): number {
  const normalized = hex.replace('#', '');

  if (!/^[0-9a-fA-F]{6}$/.test(normalized)) {
    throw new Error(`Expected a six-digit hex colour, received "${hex}"`);
  }

  const [red, green, blue] = [0, 2, 4].map((offset) =>
    channelLuminance(Number.parseInt(normalized.slice(offset, offset + 2), 16)),
  );

  return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
}

export function contrastRatio(foreground: string, background: string): number {
  const first = relativeLuminance(foreground);
  const second = relativeLuminance(background);
  const lighter = Math.max(first, second);
  const darker = Math.min(first, second);

  return (lighter + 0.05) / (darker + 0.05);
}

export const WCAG_AA = {
  normalText: 4.5,
  largeText: 3,
  uiGraphic: 3,
} as const;
