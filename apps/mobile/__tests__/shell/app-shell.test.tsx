/// <reference types="node" />
// The final block reads a source file, so this spec needs Node's globals.
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { StyleSheet, useWindowDimensions } from 'react-native';
import { screen } from '@testing-library/react-native';

import { AppShell } from '@/components/shell';
import { DESTINATIONS } from '@/components/shell/destinations';
import { sizing } from '@/ui/tokens';
import { renderWithProviders } from '../../test/test-utils';

jest.mock('react-native/Libraries/Utilities/useWindowDimensions');

const mockedUseWindowDimensions = jest.mocked(useWindowDimensions);

function atWidth(width: number) {
  mockedUseWindowDimensions.mockReturnValue({
    width,
    height: 900,
    scale: 2,
    fontScale: 1,
  });
}

function minHeightsIn(testID: string): number[] {
  const found: number[] = [];

  const visit = (node: unknown): void => {
    if (node === null || typeof node !== 'object') {
      return;
    }

    const element = node as { props?: { style?: unknown; children?: unknown }; children?: unknown };
    const style = StyleSheet.flatten(element.props?.style as never) as
      | { minHeight?: number }
      | undefined;

    if (typeof style?.minHeight === 'number') {
      found.push(style.minHeight);
    }

    const children = (element.children ?? element.props?.children) as unknown;

    if (Array.isArray(children)) {
      children.forEach(visit);
    } else if (children !== undefined) {
      visit(children);
    }
  };

  visit(screen.getByTestId(testID));

  return found;
}

const CHROME = {
  bottom: 'shell-bottom-nav',
  rail: 'shell-nav-rail',
  sidebar: 'shell-sidebar',
} as const;

function expectOnlyChrome(expected: keyof typeof CHROME) {
  for (const [mode, testID] of Object.entries(CHROME)) {
    if (mode === expected) {
      expect(screen.getByTestId(testID)).toBeTruthy();
    } else {
      expect(screen.queryByTestId(testID)).toBeNull();
    }
  }
}

describe('navigation chrome by width', () => {
  it.each([
    [375, 'bottom'],
    [768, 'rail'],
    [1440, 'sidebar'],
  ] as const)('renders only the %i chrome', async (width, expected) => {
    atWidth(width);
    await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

    expectOnlyChrome(expected);
  });

  it.each([375, 768, 1440])(
    'shows the same five destination names in the same order at %ipx',
    async (width) => {
      atWidth(width);
      await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

      for (const destination of DESTINATIONS) {
        expect(screen.getByRole('tab', { name: destination.label })).toBeTruthy();
      }
    },
  );
});

describe('active destination', () => {
  it('exposes the selected state on the active destination only', async () => {
    atWidth(375);
    await renderWithProviders(
      <AppShell activeRoute={DESTINATIONS[2].route}>{null}</AppShell>,
    );

    for (const destination of DESTINATIONS) {
      const tab = screen.getByRole('tab', { name: destination.label });
      const expected = destination.route === DESTINATIONS[2].route;

      expect({
        label: destination.label,
        selected: tab.props.accessibilityState?.selected === true,
      }).toEqual({ label: destination.label, selected: expected });
    }
  });
});

describe('profile and settings entry', () => {
  it('is reachable from the top app bar avatar rather than primary navigation', async () => {
    atWidth(375);
    await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

    expect(screen.getByRole('button', { name: 'Hồ sơ và cài đặt' })).toBeTruthy();
  });
});

describe('touch targets', () => {
  it.each([375, 768, 1440])('meets the minimum touch target at %ipx', async (width) => {
    atWidth(width);
    await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

    const chrome = width < 768 ? CHROME.bottom : width < 1024 ? CHROME.rail : CHROME.sidebar;
    const heights = minHeightsIn(chrome);

    expect(heights.length).toBeGreaterThan(0);

    for (const height of heights) {
      expect(height).toBeGreaterThanOrEqual(sizing.minTouchTarget.ios);
    }
  });
});

describe('bounded content', () => {
  it('bounds the content area at desktop width', async () => {
    atWidth(1440);
    await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

    const style = StyleSheet.flatten(
      screen.getByTestId('shell-content').props.style,
    ) as { maxWidth?: number };

    expect(style.maxWidth).toBe(sizing.shellContentMaxWidth);
  });

  it('does not bound the content area on a phone', async () => {
    atWidth(375);
    await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

    const style = StyleSheet.flatten(
      screen.getByTestId('shell-content').props.style,
    ) as { maxWidth?: number };

    expect(style.maxWidth).toBeUndefined();
  });
});

describe('the create action', () => {
  it.each([375, 768, 1440])('stays inside the shell at %ipx', async (width) => {
    atWidth(width);
    await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

    expect(screen.getByRole('button', { name: 'Thêm' })).toBeTruthy();
    expect(screen.getByTestId('shell-root')).toContainElement(
      screen.getByTestId('shell-create-action'),
    );
  });
});

describe('the web target keeps the selected state', () => {
  /**
   * jest-expo runs the native preset, which strips aria-selected before it reaches the tree, so
   * the assertion above can only cover native. react-native-web ignores accessibilityState for
   * this element and reads aria-selected instead, and without it a screen reader on web cannot
   * tell which destination is active. This guards the declaration itself; the rendered result
   * was verified in a browser against the built app.
   */
  it('declares aria-selected alongside the native accessibility state', () => {
    const source = readFileSync(
      join(__dirname, '..', '..', 'src', 'components', 'shell', 'destination-item.tsx'),
      'utf8',
    );

    expect(source).toMatch(/accessibilityState=\{\{ selected \}\}/);
    expect(source).toMatch(/aria-selected=\{selected\}/);
  });
});
