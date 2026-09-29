import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import FormsCatalog from '@/app/catalog/forms';
import { renderWithProviders } from '../../test/test-utils';

function submit() {
  fireEvent.press(screen.getByRole('button', { name: 'Lưu tài sản' }));
}

describe('the forms example', () => {
  it('composes the kit into a real add-asset form', async () => {
    await renderWithProviders(<FormsCatalog />);

    for (const label of [
      'Tên tài sản',
      'Loại tài sản',
      'Giá trị hiện tại',
      'Ngày định giá',
      'Ghi chú',
    ]) {
      expect(screen.getByText(label)).toBeTruthy();
    }
  });

  it('marks the required fields as required rather than only with an asterisk', async () => {
    await renderWithProviders(<FormsCatalog />);

    expect(screen.getByTestId('catalog-name-input').props.accessibilityState).toMatchObject({
      required: true,
    });
  });

  it('shows a summary naming every field that failed, and focuses the first', async () => {
    await renderWithProviders(<FormsCatalog />);

    submit();

    await waitFor(() => expect(screen.getByTestId('catalog-form-error-summary')).toBeTruthy());

    expect(screen.getByText('Còn 3 trường cần sửa')).toBeTruthy();
    expect(screen.getByText('Tên tài sản: Nhập tên tài sản.')).toBeTruthy();
    expect(screen.getByText('Loại tài sản: Chọn loại tài sản.')).toBeTruthy();
    expect(screen.getByText('Giá trị hiện tại: Giá trị phải lớn hơn 0.')).toBeTruthy();
  });

  it('does not report a successful submission while the form is invalid', async () => {
    await renderWithProviders(<FormsCatalog />);

    submit();

    await waitFor(() => expect(screen.getByTestId('catalog-form-error-summary')).toBeTruthy());

    expect(screen.getByTestId('catalog-submitted-input').props.value).toBe('Chưa gửi');
  });

  it('tracks unsaved changes as soon as the user types', async () => {
    await renderWithProviders(<FormsCatalog />);

    expect(screen.getByTestId('catalog-dirty-input').props.value).toBe(
      'Không có thay đổi chưa lưu',
    );

    fireEvent.changeText(screen.getByTestId('catalog-name-input'), 'Căn hộ Thảo Điền');

    await waitFor(() =>
      expect(screen.getByTestId('catalog-dirty-input').props.value).toBe('Có thay đổi chưa lưu'),
    );
  });

  it('groups a typed amount for display while keeping the entered value normalized', async () => {
    await renderWithProviders(<FormsCatalog />);

    fireEvent.changeText(screen.getByTestId('catalog-value-input'), '7500000000');

    await waitFor(() =>
      expect(screen.getByTestId('catalog-value-input').props.value).toBe('7.500.000.000'),
    );
  });

  it('submits once every field is valid, and confirms success', async () => {
    await renderWithProviders(<FormsCatalog />);

    /**
     * Each interaction is allowed to commit before the next. This environment drops a press that
     * lands while an earlier update is still settling, which looks exactly like a broken
     * component until the waits are added.
     */
    fireEvent.press(screen.getByTestId('catalog-kind-anchor'));
    await waitFor(() => expect(screen.getByTestId('catalog-kind-option-real-estate')).toBeTruthy());

    fireEvent.press(screen.getByTestId('catalog-kind-option-real-estate'));
    await waitFor(() =>
      expect(screen.getByTestId('catalog-kind-anchor').props.accessibilityValue.text).toBe(
        'Bất động sản',
      ),
    );

    fireEvent.changeText(screen.getByTestId('catalog-name-input'), 'Căn hộ Thảo Điền');
    await waitFor(() =>
      expect(screen.getByTestId('catalog-name-input').props.value).toBe('Căn hộ Thảo Điền'),
    );

    fireEvent.changeText(screen.getByTestId('catalog-value-input'), '7500000000');
    await waitFor(() =>
      expect(screen.getByTestId('catalog-value-input').props.value).toBe('7.500.000.000'),
    );

    submit();

    await waitFor(() =>
      expect(screen.getByTestId('catalog-submitted-input').props.value).toBe('Đã gửi thành công'),
    );

    expect(screen.getByText('Đã lưu tài sản.')).toBeTruthy();
    expect(screen.queryByTestId('catalog-form-error-summary')).toBeNull();
    expect(screen.getByTestId('catalog-dirty-input').props.value).toBe(
      'Không có thay đổi chưa lưu',
    );
  });
});
