import { screen } from '@testing-library/react-native';
import { Text } from 'react-native-paper';

import { FieldShell, fieldAccessibility } from '@/components/form/fields/field-shell';
import { renderWithProviders } from '../../test/test-utils';

describe('accessible props', () => {
  it('names the field from its label, never from a placeholder', () => {
    expect(fieldAccessibility({ label: 'Giá trị hiện tại' })).toMatchObject({
      accessibilityLabel: 'Giá trị hiện tại',
    });
  });

  it('marks a required field as required rather than relying on an asterisk', () => {
    const props = fieldAccessibility({ label: 'Tên tài sản', required: true });

    expect(props.accessibilityState).toMatchObject({ required: true });
    expect(props['aria-required']).toBe(true);
  });

  it('exposes an invalid state and the error as the description', () => {
    const props = fieldAccessibility({
      label: 'Giá trị hiện tại',
      errorText: 'Giá trị phải lớn hơn 0.',
    });

    expect(props.accessibilityState).toMatchObject({ invalid: true });
    expect(props['aria-invalid']).toBe(true);
    expect(props.accessibilityHint).toBe('Giá trị phải lớn hơn 0.');
  });

  it('falls back to helper text as the description when there is no error', () => {
    const props = fieldAccessibility({
      label: 'Ngày định giá',
      helperText: 'Dùng ngày bạn thực sự kiểm tra giá trị.',
    });

    expect(props.accessibilityHint).toBe('Dùng ngày bạn thực sự kiểm tra giá trị.');
    expect(props.accessibilityState).toMatchObject({ invalid: false });
  });

  it('keeps read-only distinct from disabled', () => {
    const readOnly = fieldAccessibility({ label: 'Giá trị quy đổi', readOnly: true });
    const disabled = fieldAccessibility({ label: 'Giá trị quy đổi', disabled: true });

    expect(readOnly.accessibilityState).toMatchObject({ disabled: false });
    expect(readOnly['aria-readonly']).toBe(true);

    expect(disabled.accessibilityState).toMatchObject({ disabled: true });
    expect(disabled['aria-readonly']).toBeUndefined();
  });
});

describe('rendering', () => {
  it('keeps the label visible even when the field already has a value', async () => {
    await renderWithProviders(
      <FieldShell label="Tên tài sản" testID="field">
        <Text>Căn hộ Thảo Điền</Text>
      </FieldShell>,
    );

    expect(screen.getByText('Tên tài sản')).toBeTruthy();
    expect(screen.getByText('Căn hộ Thảo Điền')).toBeTruthy();
  });

  it('shows helper text when there is no error', async () => {
    await renderWithProviders(
      <FieldShell helperText="Nhập theo đồng Việt Nam." label="Giá trị" testID="field">
        <Text>0</Text>
      </FieldShell>,
    );

    expect(screen.getByText('Nhập theo đồng Việt Nam.')).toBeTruthy();
  });

  it('replaces helper text with the error, so the correction is what the user reads', async () => {
    await renderWithProviders(
      <FieldShell
        errorText="Giá trị phải lớn hơn 0."
        helperText="Nhập theo đồng Việt Nam."
        label="Giá trị"
        testID="field"
      >
        <Text>0</Text>
      </FieldShell>,
    );

    expect(screen.getByText('Giá trị phải lớn hơn 0.')).toBeTruthy();
    expect(screen.queryByText('Nhập theo đồng Việt Nam.')).toBeNull();
  });

  it('announces the error politely rather than stealing focus', async () => {
    await renderWithProviders(
      <FieldShell errorText="Giá trị phải lớn hơn 0." label="Giá trị" testID="field">
        <Text>0</Text>
      </FieldShell>,
    );

    expect(screen.getByTestId('field-error').props.accessibilityLiveRegion).toBe('polite');
  });

  it('marks a required field visibly as well as semantically', async () => {
    await renderWithProviders(
      <FieldShell label="Tên tài sản" required testID="field">
        <Text>—</Text>
      </FieldShell>,
    );

    expect(screen.getByTestId('field-required-marker')).toBeTruthy();
  });
});
