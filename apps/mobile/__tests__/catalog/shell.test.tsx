import { StyleSheet, useWindowDimensions } from 'react-native';
import { screen } from '@testing-library/react-native';

import ShellCatalog from '@/app/catalog/shell';
import { ASSETS, NET_WORTH, formatVnd } from '@/catalog/fixture';
import { DESTINATIONS } from '@/components/shell/destinations';
import { sizing } from '@/ui/tokens';
import { renderWithProviders } from '../../test/test-utils';

jest.mock('react-native/Libraries/Utilities/useWindowDimensions');

const mockedUseWindowDimensions = jest.mocked(useWindowDimensions);

function atWidth(width: number, fontScale = 1) {
  mockedUseWindowDimensions.mockReturnValue({ width, height: 900, scale: 2, fontScale });
}

const WIDTHS = [360, 375, 768, 1024, 1440];

const CHROME = {
  bottom: 'shell-bottom-nav',
  rail: 'shell-nav-rail',
  sidebar: 'shell-sidebar',
} as const;

function expectedChrome(width: number) {
  if (width >= 1024) {
    return CHROME.sidebar;
  }

  return width >= 768 ? CHROME.rail : CHROME.bottom;
}

describe('the shell example across every reviewed width', () => {
  it.each(WIDTHS)('renders exactly one navigation chrome at %ipx', async (width) => {
    atWidth(width);
    await renderWithProviders(<ShellCatalog />);

    const present = Object.values(CHROME).filter(
      (testID) => screen.queryByTestId(testID) !== null,
    );

    expect(present).toEqual([expectedChrome(width)]);
  });

  it.each(WIDTHS)('shows the same five destinations at %ipx', async (width) => {
    atWidth(width);
    await renderWithProviders(<ShellCatalog />);

    for (const destination of DESTINATIONS) {
      expect(screen.getByRole('tab', { name: destination.label })).toBeTruthy();
    }
  });

  it.each(WIDTHS)('renders the real content inside the shell at %ipx', async (width) => {
    atWidth(width);
    await renderWithProviders(<ShellCatalog />);

    for (const asset of ASSETS.slice(0, 3)) {
      expect(screen.getByTestId(`catalog-shell-row-${asset.id}-title`)).toHaveTextContent(
        asset.title,
      );
    }
  });

  it.each(WIDTHS)('never lets the content exceed the bounded width at %ipx', async (width) => {
    atWidth(width);
    await renderWithProviders(<ShellCatalog />);

    const style = StyleSheet.flatten(screen.getByTestId('shell-content').props.style) as {
      maxWidth?: number;
      width?: string;
    };

    expect(style.width).toBe('100%');

    if (width >= 1024) {
      expect(style.maxWidth).toBe(sizing.shellContentMaxWidth);
    } else {
      expect(style.maxWidth).toBeUndefined();
    }
  });

  it.each(WIDTHS)('fixes no element to a width that could overflow at %ipx', async (width) => {
    atWidth(width);
    await renderWithProviders(<ShellCatalog />);

    const root = StyleSheet.flatten(screen.getByTestId('shell-root').props.style) as {
      width?: number;
      minWidth?: number;
    };

    expect(root.width).toBeUndefined();
    expect(root.minWidth).toBeUndefined();
  });

  it.each(WIDTHS)('keeps the amounts readable at a large font scale at %ipx', async (width) => {
    atWidth(width, 1.6);
    await renderWithProviders(<ShellCatalog />);

    for (const asset of ASSETS.slice(0, 3)) {
      const value = screen.getByTestId(`catalog-shell-row-${asset.id}-primary-value`);

      expect(value).toHaveTextContent(formatVnd(asset.amount));
      expect(value.props.numberOfLines).toBeUndefined();
      expect(value.props.ellipsizeMode).toBeUndefined();
    }
  });

  it('reports the navigation mode alongside the fixture total', async () => {
    atWidth(1440);
    await renderWithProviders(<ShellCatalog />);

    expect(screen.getByTestId('catalog-shell-kpi-value')).toHaveTextContent(formatVnd(NET_WORTH));
  });
});
