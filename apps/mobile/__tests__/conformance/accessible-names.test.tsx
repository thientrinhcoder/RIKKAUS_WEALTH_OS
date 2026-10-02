import { useWindowDimensions } from 'react-native';
import { screen } from '@testing-library/react-native';
import type { ReactElement } from 'react';

import DataCatalog from '@/app/catalog/data';
import FormsCatalog from '@/app/catalog/forms';
import OverlaysCatalog from '@/app/catalog/overlays';
import ShellCatalog from '@/app/catalog/shell';
import StatesCatalog from '@/app/catalog/states';
import { SnackbarProvider } from '@/components/feedback';
import { renderWithProviders } from '../../test/test-utils';

jest.mock('react-native/Libraries/Utilities/useWindowDimensions');

jest
  .mocked(useWindowDimensions)
  .mockReturnValue({ width: 375, height: 812, scale: 2, fontScale: 1 });

/**
 * An unnamed interactive element is the most common accessibility regression and the easiest to
 * introduce: an icon-only control added in a hurry, or a label that moved onto a wrapper. This
 * audit fails the whole catalog if a single one appears.
 */
const SCREENS: ReadonlyArray<readonly [string, ReactElement]> = [
  ['forms', <FormsCatalog key="forms" />],
  ['data', <DataCatalog key="data" />],
  ['states', <SnackbarProvider key="states"><StatesCatalog /></SnackbarProvider>],
  ['overlays', <OverlaysCatalog key="overlays" />],
  ['shell', <ShellCatalog key="shell" />],
];

/**
 * Icon fonts render their glyphs as Private Use Area characters. A screen reader announces those
 * as nothing meaningful, so they must not count as a name: without this, an icon-only button with
 * no label looks named and the audit passes while the control is unusable.
 */
function withoutIconGlyphs(text: string): string {
  return text.replace(/[\uE000-\uF8FF]|[\u{F0000}-\u{FFFFD}]|[\u{100000}-\u{10FFFD}]/gu, '');
}

/** Reads what a screen reader would announce for an element. */
function accessibleName(element: {
  props: { accessibilityLabel?: string; children?: unknown };
}): string {
  if (typeof element.props.accessibilityLabel === 'string') {
    return withoutIconGlyphs(element.props.accessibilityLabel).trim();
  }

  const collected: string[] = [];

  const visit = (node: unknown): void => {
    if (typeof node === 'string') {
      collected.push(node);
      return;
    }

    if (node === null || typeof node !== 'object') {
      return;
    }

    const child = node as { props?: { children?: unknown }; children?: unknown };
    const children = (child.children ?? child.props?.children) as unknown;

    if (Array.isArray(children)) {
      children.forEach(visit);
    } else if (children !== undefined) {
      visit(children);
    }
  };

  visit(element);

  return withoutIconGlyphs(collected.join(' ')).trim();
}

describe('every interactive element in the catalog is named', () => {
  it.each(SCREENS)('names every control on the %s screen', async (_name, element) => {
    await renderWithProviders(element);

    const interactive = [
      ...screen.queryAllByRole('button'),
      ...screen.queryAllByRole('tab'),
      ...screen.queryAllByRole('radio'),
      ...screen.queryAllByRole('search'),
    ];

    expect(interactive.length).toBeGreaterThan(0);

    const unnamed = interactive
      .map((node) => ({
        testID: node.props.testID ?? '(no test id)',
        name: accessibleName(node),
      }))
      .filter(({ name }) => name === '');

    expect(unnamed).toEqual([]);
  });

  it.each(SCREENS)('gives every text input on the %s screen a label', async (_name, element) => {
    await renderWithProviders(element);

    const unnamed = screen
      .queryAllByRole('none')
      .filter((node) => node.props.editable !== undefined)
      .map((node) => ({
        testID: node.props.testID ?? '(no test id)',
        label: node.props.accessibilityLabel ?? '',
      }))
      .filter(({ label }) => label === '');

    expect(unnamed).toEqual([]);
  });
});
