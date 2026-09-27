import { StyleSheet } from 'react-native';
import { fireEvent, screen, waitFor } from '@testing-library/react-native';
import { useState } from 'react';

import { CurrencyField } from '@/components/form/fields/currency-field';
import { SelectField } from '@/components/form/fields/select-field';
import { TextField } from '@/components/form/fields/text-field';
import { TextareaField } from '@/components/form/fields/textarea-field';
import { sizing } from '@/ui/tokens';
import { renderWithProviders } from '../../test/test-utils';

function input(testID: string) {
  return screen.getByTestId(`${testID}-input`);
}

/**
 * Paper applies the control style to a wrapper around the element carrying the test id, so the
 * assertion walks the rendered subtree rather than assuming which layer holds it.
 */
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

describe('accessible state coverage', () => {
  it.each([
    ['required', { required: true }, { required: true }],
    ['disabled', { disabled: true }, { disabled: true }],
    ['invalid', { errorText: 'Nhập giá trị.' }, { invalid: true }],
  ] as const)('reports the %s state on the control itself', async (_name, props, expected) => {
    await renderWithProviders(
      <TextField label="Tên tài sản" testID="f" value="" {...props} />,
    );

    expect(input('f').props.accessibilityState).toMatchObject(expected);
  });

  it('keeps a read-only control readable rather than presenting it as unusable', async () => {
    await renderWithProviders(
      <TextField label="Giá trị quy đổi" readOnly testID="f" value="1.250.750.000" />,
    );

    expect(input('f').props.accessibilityState).toMatchObject({ disabled: false });
    expect(input('f').props.editable).toBe(false);
  });

  it('describes the control with its error when one is present', async () => {
    await renderWithProviders(
      <TextField errorText="Nhập tên tài sản." label="Tên tài sản" testID="f" value="" />,
    );

    expect(input('f').props.accessibilityHint).toBe('Nhập tên tài sản.');
  });
});

describe('error recovery', () => {
  function RecoverableField() {
    const [value, setValue] = useState('');

    return (
      <CurrencyField
        errorText={value === '' ? 'Giá trị phải lớn hơn 0.' : undefined}
        helperText="Nhập theo đồng Việt Nam."
        label="Giá trị hiện tại"
        onChangeValue={setValue}
        testID="value"
        value={value}
      />
    );
  }

  it('clears the error once the field is corrected, and restores the helper text', async () => {
    await renderWithProviders(<RecoverableField />);

    expect(screen.getByText('Giá trị phải lớn hơn 0.')).toBeTruthy();
    expect(screen.queryByText('Nhập theo đồng Việt Nam.')).toBeNull();

    fireEvent.changeText(input('value'), '1250750000');

    await waitFor(() => expect(screen.queryByText('Giá trị phải lớn hơn 0.')).toBeNull());
    expect(screen.getByText('Nhập theo đồng Việt Nam.')).toBeTruthy();
    expect(input('value').props.accessibilityState).toMatchObject({ invalid: false });
  });
});

describe('text scaling', () => {
  it.each([
    ['text', <TextField key="t" label="Tên tài sản đầy đủ theo giấy tờ" testID="f" value="" />],
    ['currency', <CurrencyField key="c" label="Giá trị hiện tại đã quy đổi" testID="f" value="" />],
    ['textarea', <TextareaField key="a" label="Ghi chú chi tiết về tài sản" testID="f" value="" />],
  ])('lets the %s label wrap rather than clip at a large font scale', async (_name, element) => {
    await renderWithProviders(element as React.ReactElement);

    const label = screen.getByText(/Tên tài sản đầy đủ|Giá trị hiện tại đã|Ghi chú chi tiết/);

    expect(label.props.numberOfLines).toBeUndefined();
    expect(label.props.ellipsizeMode).toBeUndefined();
  });

  it('keeps every control at or above the minimum control height', async () => {
    await renderWithProviders(<TextField label="Tên tài sản" testID="f" value="" />);

    expect(renderedMinHeights('f')).toContain(sizing.minControlHeight);
  });

  it('keeps each select option at or above the minimum touch target', async () => {
    await renderWithProviders(
      <SelectField
        label="Loại tài sản"
        options={[{ value: 'a', label: 'Bất động sản' }]}
        testID="kind"
        value=""
      />,
    );

    fireEvent.press(screen.getByTestId('kind-anchor'));

    const option = await screen.findByTestId('kind-option-a');
    const style = Array.isArray(option.props.style)
      ? Object.assign({}, ...option.props.style)
      : option.props.style;

    expect(style.minHeight).toBeGreaterThanOrEqual(sizing.minTouchTarget.ios);
  });
});
