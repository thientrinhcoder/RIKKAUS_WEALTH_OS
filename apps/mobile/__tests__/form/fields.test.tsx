import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { CurrencyField } from '@/components/form/fields/currency-field';
import { DateField, DATE_ERROR_MESSAGES } from '@/components/form/fields/date-field';
import { NumberField } from '@/components/form/fields/number-field';
import { SearchField } from '@/components/form/fields/search-field';
import { SelectField } from '@/components/form/fields/select-field';
import { TextField } from '@/components/form/fields/text-field';
import { TextareaField } from '@/components/form/fields/textarea-field';
import { renderWithProviders } from '../../test/test-utils';

function input(testID: string) {
  return screen.getByTestId(`${testID}-input`);
}

describe('text field', () => {
  it('takes its accessible name from the label', async () => {
    await renderWithProviders(<TextField label="Tên tài sản" testID="name" value="" />);

    expect(input('name').props.accessibilityLabel).toBe('Tên tài sản');
  });

  it('reports what the user typed', async () => {
    const onChangeValue = jest.fn();

    await renderWithProviders(
      <TextField label="Tên tài sản" onChangeValue={onChangeValue} testID="name" value="" />,
    );

    fireEvent.changeText(input('name'), 'Căn hộ Thảo Điền');

    expect(onChangeValue).toHaveBeenCalledWith('Căn hộ Thảo Điền');
  });

  it('never uses a placeholder in place of the label', async () => {
    await renderWithProviders(<TextField label="Tên tài sản" testID="name" value="" />);

    expect(screen.getByText('Tên tài sản')).toBeTruthy();
    expect(input('name').props.label).toBeUndefined();
  });
});

describe('currency field', () => {
  it('puts the unit beside the value rather than inside a placeholder', async () => {
    await renderWithProviders(
      <CurrencyField label="Giá trị hiện tại" testID="value" value="12002000000" />,
    );

    expect(screen.getByText('₫')).toBeTruthy();
    expect(input('value').props.placeholder).toBeUndefined();
  });

  it('groups the displayed amount the Vietnamese way', async () => {
    await renderWithProviders(
      <CurrencyField label="Giá trị hiện tại" testID="value" value="12002000000" />,
    );

    expect(input('value').props.value).toBe('12.002.000.000');
  });

  it('reports a normalized, unrounded value', async () => {
    const onChangeValue = jest.fn();

    await renderWithProviders(
      <CurrencyField
        label="Giá trị hiện tại"
        onChangeValue={onChangeValue}
        testID="value"
        value=""
      />,
    );

    fireEvent.changeText(input('value'), '1.250.750.000');

    expect(onChangeValue).toHaveBeenCalledWith('1250750000');
  });

  it('rejects a negative amount unless the caller allows it', async () => {
    const onChangeValue = jest.fn();

    await renderWithProviders(
      <CurrencyField
        label="Giá trị hiện tại"
        onChangeValue={onChangeValue}
        testID="value"
        value=""
      />,
    );

    fireEvent.changeText(input('value'), '-500000');

    expect(onChangeValue).toHaveBeenCalledWith('500000');
  });

  it('opens a numeric keyboard', async () => {
    await renderWithProviders(<CurrencyField label="Giá trị" testID="value" value="" />);

    expect(input('value').props.keyboardType).toBe('numeric');
  });
});

describe('number field', () => {
  it('opens a decimal keyboard when decimals are allowed', async () => {
    await renderWithProviders(<NumberField label="Tỷ giá" testID="rate" value="" />);

    expect(input('rate').props.keyboardType).toBe('decimal-pad');
  });

  it('preserves precision in the reported value', async () => {
    const onChangeValue = jest.fn();

    await renderWithProviders(
      <NumberField label="Tỷ giá" onChangeValue={onChangeValue} testID="rate" value="" />,
    );

    fireEvent.changeText(input('rate'), '25.100,456');

    expect(onChangeValue).toHaveBeenCalledWith('25100,456');
  });

  it('shows its unit beside the value when one is given', async () => {
    await renderWithProviders(
      <NumberField label="Tỷ giá" testID="rate" unit="VND" value="25100" />,
    );

    expect(screen.getByText('VND')).toBeTruthy();
  });
});

describe('date field', () => {
  it('displays the stored value in the Vietnamese format', async () => {
    await renderWithProviders(
      <DateField label="Ngày định giá" testID="date" value="2026-09-12" />,
    );

    expect(input('date').props.value).toBe('12/09/2026');
  });

  it('reports back an unambiguous stored value when typed', async () => {
    const onChangeValue = jest.fn();

    await renderWithProviders(
      <DateField label="Ngày định giá" onChangeValue={onChangeValue} testID="date" value="" />,
    );

    fireEvent.changeText(input('date'), '12/09/2026');

    expect(onChangeValue).toHaveBeenCalledWith('2026-09-12');
  });

  it('states the cause instead of falling back silently', async () => {
    const onChangeValue = jest.fn();
    const onInvalid = jest.fn();

    await renderWithProviders(
      <DateField
        label="Ngày định giá"
        onChangeValue={onChangeValue}
        onInvalid={onInvalid}
        testID="date"
        value=""
      />,
    );

    fireEvent.changeText(input('date'), '32/09/2026');

    expect(onInvalid).toHaveBeenCalledWith(DATE_ERROR_MESSAGES['invalid-day']);
    expect(onChangeValue).not.toHaveBeenCalled();
  });

  it('offers a named control that opens the platform picker', async () => {
    await renderWithProviders(<DateField label="Ngày định giá" testID="date" value="" />);

    /**
     * Section 16.2 wants the platform picker with typing as the alternative, so both routes are
     * present: the field accepts a typed date and this control opens the picker.
     */
    expect(screen.getByLabelText('Mở bộ chọn ngày')).toBeTruthy();
  });

  it('keeps the picker closed until it is asked for', async () => {
    await renderWithProviders(<DateField label="Ngày định giá" testID="date" value="" />);

    expect(screen.queryByTestId('date-picker')).toBeNull();
  });

  it('opens the picker when the control is pressed', async () => {
    await renderWithProviders(<DateField label="Ngày định giá" testID="date" value="" />);

    fireEvent.press(screen.getByLabelText('Mở bộ chọn ngày'));

    await waitFor(() => expect(screen.getByTestId('date-picker')).toBeTruthy());
  });

  it('does not offer the picker on a read-only field', async () => {
    await renderWithProviders(
      <DateField label="Ngày định giá" readOnly testID="date" value="2026-09-12" />,
    );

    expect(screen.getByLabelText('Mở bộ chọn ngày').props.accessibilityState).toMatchObject({
      disabled: true,
    });
  });
});

describe('select field', () => {
  const OPTIONS = [
    { value: 'real-estate', label: 'Bất động sản' },
    { value: 'savings', label: 'Tiền gửi tiết kiệm' },
    { value: 'equity', label: 'Cổ phiếu' },
  ];

  it('preselects nothing, so no financially meaningful answer is chosen for the user', async () => {
    await renderWithProviders(
      <SelectField label="Loại tài sản" options={OPTIONS} testID="kind" value="" />,
    );

    expect(screen.getByText('Chưa chọn')).toBeTruthy();

    for (const option of OPTIONS) {
      expect(screen.queryByText(option.label)).toBeNull();
    }
  });

  it('exposes the chosen option as the accessible value', async () => {
    await renderWithProviders(
      <SelectField label="Loại tài sản" options={OPTIONS} testID="kind" value="savings" />,
    );

    expect(screen.getByTestId('kind-anchor').props.accessibilityValue).toEqual({
      text: 'Tiền gửi tiết kiệm',
    });
  });

  it('reports the option the user picked', async () => {
    const onChangeValue = jest.fn();

    await renderWithProviders(
      <SelectField
        label="Loại tài sản"
        onChangeValue={onChangeValue}
        options={OPTIONS}
        testID="kind"
        value=""
      />,
    );

    fireEvent.press(screen.getByTestId('kind-anchor'));

    await waitFor(() => expect(screen.getByText('Cổ phiếu')).toBeTruthy());

    fireEvent.press(screen.getByText('Cổ phiếu'));

    await waitFor(() => expect(onChangeValue).toHaveBeenCalledWith('equity'));
  });
});

describe('search field', () => {
  it('identifies itself as search', async () => {
    await renderWithProviders(
      <SearchField label="Tìm tài sản" onChangeValue={jest.fn()} testID="q" value="" />,
    );

    expect(input('q').props.accessibilityRole).toBe('search');
    expect(input('q').props.accessibilityLabel).toBe('Tìm tài sản');
  });

  it('offers no clear control until there is something to clear', async () => {
    await renderWithProviders(
      <SearchField label="Tìm tài sản" onChangeValue={jest.fn()} testID="q" value="" />,
    );

    expect(screen.queryByTestId('q-clear')).toBeNull();
  });

  it('clears the query through a control with an accessible name', async () => {
    const onChangeValue = jest.fn();

    await renderWithProviders(
      <SearchField
        label="Tìm tài sản"
        onChangeValue={onChangeValue}
        testID="q"
        value="Vietcombank"
      />,
    );

    const clear = screen.getByLabelText('Xoá từ khoá tìm kiếm');

    expect(clear).toBeTruthy();

    fireEvent.press(clear);

    expect(onChangeValue).toHaveBeenCalledWith('');
  });
});

describe('textarea field', () => {
  it('is multiline and keeps its label visible', async () => {
    await renderWithProviders(<TextareaField label="Ghi chú" testID="note" value="" />);

    expect(input('note').props.multiline).toBe(true);
    expect(screen.getByText('Ghi chú')).toBeTruthy();
  });

  it('imposes no line limit, so long notes wrap rather than clip', async () => {
    await renderWithProviders(<TextareaField label="Ghi chú" testID="note" value="" />);

    expect(input('note').props.ellipsizeMode).toBeUndefined();
  });
});
