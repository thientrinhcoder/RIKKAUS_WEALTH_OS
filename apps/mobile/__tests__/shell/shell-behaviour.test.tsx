import { AccessibilityInfo, StyleSheet, useWindowDimensions } from 'react-native';
import { screen, within } from '@testing-library/react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { QueryClientProvider } from '@tanstack/react-query';
import { PaperProvider } from 'react-native-paper';
import { render } from '@testing-library/react-native';
import type { PropsWithChildren, ReactElement } from 'react';

import { AppShell } from '@/components/shell';
import { DESTINATIONS } from '@/components/shell/destinations';
import { focusHeading, ScreenHeading } from '@/components/shell/screen-heading';
import { appTheme } from '@/ui/theme';
import { createTestQueryClient, renderWithProviders } from '../../test/test-utils';

jest.mock('react-native/Libraries/Utilities/useWindowDimensions');

const mockedUseWindowDimensions = jest.mocked(useWindowDimensions);

function atWidth(width: number, fontScale = 1) {
  mockedUseWindowDimensions.mockReturnValue({ width, height: 900, scale: 2, fontScale });
}

/** Non-zero insets, as a notched phone reports them. */
const INSET_METRICS = {
  frame: { x: 0, y: 0, width: 375, height: 812 },
  insets: { top: 47, left: 0, right: 0, bottom: 34 },
};

async function renderWithInsets(ui: ReactElement) {
  const queryClient = createTestQueryClient();

  function Wrapper({ children }: PropsWithChildren) {
    return (
      <SafeAreaProvider initialMetrics={INSET_METRICS}>
        <QueryClientProvider client={queryClient}>
          <PaperProvider theme={appTheme}>{children}</PaperProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    );
  }

  return render(ui, { wrapper: Wrapper });
}

function flatten(testID: string) {
  return (StyleSheet.flatten(screen.getByTestId(testID).props.style) ?? {}) as Record<
    string,
    number | undefined
  >;
}

describe('focus on route change', () => {
  it('moves focus through the web focus method when the platform provides one', () => {
    const focus = jest.fn();

    expect(focusHeading({ focus })).toBe(true);
    expect(focus).toHaveBeenCalledTimes(1);
  });

  it('reports failure rather than claiming success when there is nothing to focus', () => {
    expect(focusHeading(null)).toBe(false);
    expect(focusHeading({})).toBe(false);
  });

  it('exposes the destination name as a header', async () => {
    atWidth(375);
    await renderWithProviders(<ScreenHeading>Dòng tiền</ScreenHeading>);

    expect(screen.getByRole('header', { name: 'Dòng tiền' })).toBeTruthy();
  });

  it('renders exactly one main heading per destination', async () => {
    atWidth(375);
    await renderWithProviders(<ScreenHeading>Mục tiêu</ScreenHeading>);

    expect(screen.getAllByTestId('screen-heading')).toHaveLength(1);
  });
});

describe('safe areas', () => {
  it('reserves the bottom inset in the bottom navigation', async () => {
    atWidth(375);
    await renderWithInsets(<AppShell activeRoute="/">{null}</AppShell>);

    expect(flatten('shell-bottom-nav').paddingBottom).toBe(INSET_METRICS.insets.bottom);
  });

  it('reserves the top inset in the rail', async () => {
    atWidth(768);
    await renderWithInsets(<AppShell activeRoute="/">{null}</AppShell>);

    expect(flatten('shell-nav-rail').paddingTop).toBeGreaterThanOrEqual(
      INSET_METRICS.insets.top,
    );
  });

  it('keeps the create action inside the shell even with insets applied', async () => {
    atWidth(375);
    await renderWithInsets(<AppShell activeRoute="/">{null}</AppShell>);

    expect(screen.getByTestId('shell-root')).toContainElement(
      screen.getByTestId('shell-create-action'),
    );
  });
});

describe('layout safety across the reviewed widths', () => {
  it.each([360, 375, 768, 1024, 1440])('never scrolls horizontally at %ipx', async (width) => {
    atWidth(width);
    await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

    const root = flatten('shell-root');
    const content = flatten('shell-content');

    expect(root.width).toBeUndefined();
    expect(content.width).toBe('100%' as unknown as number);
    expect(content.minWidth).toBeUndefined();
  });

  it.each([360, 375])(
    'keeps the bottom bar to one line per destination at %ipx',
    async (width) => {
      atWidth(width, 1.6);
      await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

      for (const destination of DESTINATIONS) {
        const item = screen.getByTestId(`shell-destination-${destination.key}`);
        const [label] = within(item).getAllByText(destination.shortLabel);

        /**
         * The bottom bar gives each destination a fifth of the width. A wrapped label makes its
         * item taller than its siblings and knocks the row out of alignment, so the bar shows the
         * short label on a single line instead.
         */
        expect(label.props.numberOfLines).toBe(1);
      }
    },
  );

  it.each([768, 1024, 1440])(
    'lets the rail and sidebar labels wrap freely at %ipx',
    async (width) => {
      atWidth(width, 1.6);
      await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

      for (const destination of DESTINATIONS) {
        const item = screen.getByTestId(`shell-destination-${destination.key}`);
        const [label] = within(item).getAllByText(destination.label);

        /** There is room here, so section 5.2's preference for wrapping over truncation holds. */
        expect(label.props.ellipsizeMode).toBeUndefined();
      }
    },
  );

  it('drops the visible label on a very narrow screen but never the accessible name', async () => {
    atWidth(320);
    await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

    for (const destination of DESTINATIONS) {
      /** The name a screen reader announces is the full one at every width. */
      expect(screen.getByRole('tab', { name: destination.label })).toBeTruthy();
    }

    expect(screen.queryByText('TS & Nợ')).toBeNull();
  });

  it('shows the short label once there is room for it', async () => {
    atWidth(375);
    await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

    expect(screen.getByText('TS & Nợ')).toBeTruthy();
    /** The full label is still what assistive technology hears. */
    expect(screen.getByRole('tab', { name: 'Tài sản & Nợ' })).toBeTruthy();
  });
});

describe('hierarchy consistency', () => {
  it.each([375, 768, 1440])(
    'keeps one navigation list and one profile entry at %ipx',
    async (width) => {
      atWidth(width);
      await renderWithProviders(<AppShell activeRoute="/">{null}</AppShell>);

      /**
       * The chrome carries accessibilityRole="tablist" for assistive technology, but that role
       * is not queryable through the testing library, so the container is found by test id and
       * the role asserted on its props.
       */
      const chromes = ['shell-bottom-nav', 'shell-nav-rail', 'shell-sidebar']
        .map((testID) => screen.queryByTestId(testID))
        .filter((node) => node !== null);

      expect(chromes).toHaveLength(1);
      expect(chromes[0]?.props.accessibilityRole).toBe('tablist');
      expect(screen.getAllByRole('button', { name: 'Hồ sơ và cài đặt' })).toHaveLength(1);
      expect(screen.getAllByRole('tab')).toHaveLength(DESTINATIONS.length);
    },
  );
});
