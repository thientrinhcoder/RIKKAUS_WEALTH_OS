import { StyleSheet } from 'react-native';
import { fireEvent, screen } from '@testing-library/react-native';

import {
  ActionButton,
  ACTION_BUTTON_VARIANTS,
  actionButtonMinHeight,
  resolveActionIcon,
} from '@/components/action';
import { sizing } from '@/ui/tokens';
import { renderWithProviders } from '../../test/test-utils';

/**
 * Paper applies `contentStyle`, and the accessibility state this component passes, to
 * elements around the one carrying `testID`. These helpers walk the rendered tree rather than
 * assuming a layer, so they survive a Paper internal change.
 */
function busyStateInTree(root: unknown): boolean {
  let busy = false;

  const visit = (node: unknown): void => {
    if (node === null || typeof node !== 'object') {
      return;
    }

    const element = node as {
      props?: { accessibilityState?: { busy?: boolean }; children?: unknown };
      children?: unknown;
    };

    if (element.props?.accessibilityState?.busy === true) {
      busy = true;
    }

    const children = (element.children ?? element.props?.children) as unknown;

    if (Array.isArray(children)) {
      children.forEach(visit);
    } else if (children !== undefined) {
      visit(children);
    }
  };

  visit(root);

  return busy;
}

function renderedMinHeights(testID: string): number[] {
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

describe('variants', () => {
  it('covers the five approved variants', () => {
    expect([...ACTION_BUTTON_VARIANTS]).toEqual([
      'primary',
      'secondary',
      'tonal',
      'text',
      'destructive',
    ]);
  });

  it.each([...ACTION_BUTTON_VARIANTS])('renders %s as a button with its visible label', async (variant) => {
    await renderWithProviders(
      <ActionButton testID={`action-${variant}`} variant={variant}>
        Lưu thay đổi
      </ActionButton>,
    );

    expect(screen.getByRole('button', { name: 'Lưu thay đổi' })).toBeTruthy();
  });
});

describe('disabled state', () => {
  it('exposes the disabled state and blocks activation', async () => {
    const onPress = jest.fn();

    await renderWithProviders(
      <ActionButton disabled onPress={onPress} testID="action-disabled" variant="primary">
        Lưu thay đổi
      </ActionButton>,
    );

    const button = screen.getByRole('button', { name: 'Lưu thay đổi' });

    expect(button).toBeDisabled();

    fireEvent.press(button);
    expect(onPress).not.toHaveBeenCalled();
  });
});

describe('loading state', () => {
  it('exposes a busy state and blocks activation', async () => {
    const onPress = jest.fn();

    await renderWithProviders(
      <ActionButton loading onPress={onPress} testID="action-loading" variant="primary">
        Đang lưu
      </ActionButton>,
    );

    const button = screen.getByRole('button', { name: 'Đang lưu' });

    expect(busyStateInTree(screen.getByTestId('action-loading'))).toBe(true);

    fireEvent.press(button);
    expect(onPress).not.toHaveBeenCalled();
  });

  it('does not claim to be busy when it is idle', async () => {
    await renderWithProviders(
      <ActionButton onPress={jest.fn()} testID="action-idle" variant="primary">
        Lưu thay đổi
      </ActionButton>,
    );

    expect(busyStateInTree(screen.getByTestId('action-idle'))).toBe(false);
  });

  it('still activates an ordinary press when it is neither loading nor disabled', async () => {
    const onPress = jest.fn();

    await renderWithProviders(
      <ActionButton onPress={onPress} testID="action-active" variant="primary">
        Lưu thay đổi
      </ActionButton>,
    );

    fireEvent.press(screen.getByRole('button', { name: 'Lưu thay đổi' }));
    expect(onPress).toHaveBeenCalledTimes(1);
  });
});

describe('icon-only buttons', () => {
  it('uses its accessible label as the accessible name', async () => {
    await renderWithProviders(
      <ActionButton
        accessibilityLabel="Xoá tài sản"
        icon="delete-outline"
        testID="action-icon"
        variant="text"
      />,
    );

    expect(screen.getByRole('button', { name: 'Xoá tài sản' })).toBeTruthy();
  });

  it('keeps a full-size hit area at or above the minimum touch target', async () => {
    await renderWithProviders(
      <ActionButton
        accessibilityLabel="Xoá tài sản"
        icon="delete-outline"
        testID="action-icon"
        variant="text"
      />,
    );

    expect(renderedMinHeights('action-icon')).toContain(actionButtonMinHeight('text'));
    expect(actionButtonMinHeight('text')).toBeGreaterThanOrEqual(sizing.minTouchTarget.android);
  });
});

describe('sizing', () => {
  it('gives the primary button the approved 52 to 56pt height', () => {
    expect(actionButtonMinHeight('primary')).toBeGreaterThanOrEqual(52);
    expect(actionButtonMinHeight('primary')).toBeLessThanOrEqual(56);
  });

  it('gives the destructive button the same height as the primary action', () => {
    expect(actionButtonMinHeight('destructive')).toBe(actionButtonMinHeight('primary'));
  });

  it.each([...ACTION_BUTTON_VARIANTS])('keeps %s at or above the minimum touch target', (variant) => {
    expect(actionButtonMinHeight(variant)).toBeGreaterThanOrEqual(sizing.minTouchTarget.ios);
    expect(actionButtonMinHeight(variant)).toBeGreaterThanOrEqual(sizing.minTouchTarget.android);
  });

  it('actually applies the height to the rendered button', async () => {
    await renderWithProviders(
      <ActionButton testID="action-primary" variant="primary">
        Lưu thay đổi
      </ActionButton>,
    );

    expect(renderedMinHeights('action-primary')).toContain(actionButtonMinHeight('primary'));
  });
});

describe('destructive actions', () => {
  it('is distinguishable from an ordinary action by more than colour', () => {
    expect(resolveActionIcon('destructive')).toBeTruthy();
  });

  it('does not put an icon on an ordinary action by default', () => {
    for (const variant of ['primary', 'secondary', 'tonal', 'text'] as const) {
      expect(resolveActionIcon(variant)).toBeUndefined();
    }
  });

  it('lets a caller substitute a more specific icon without losing the distinction', () => {
    expect(resolveActionIcon('destructive', 'delete-outline')).toBe('delete-outline');
  });
});
