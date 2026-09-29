import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import OverlaysCatalog from '@/app/catalog/overlays';
import { renderWithProviders } from '../../test/test-utils';

function outcome() {
  return screen.getByTestId('catalog-outcome-input').props.value;
}

describe('the overlays example', () => {
  it('starts with no overlay open and no action taken', async () => {
    await renderWithProviders(<OverlaysCatalog />);

    expect(outcome()).toBe('Chưa có thao tác nào');
    expect(screen.queryByTestId('confirm-dialog')).toBeNull();
  });

  it('confirms an ordinary action and reports the confirmed outcome', async () => {
    await renderWithProviders(<OverlaysCatalog />);

    fireEvent.press(screen.getByRole('button', { name: 'Xác nhận thông thường' }));
    await waitFor(() => expect(screen.getByTestId('confirm-dialog')).toBeTruthy());

    fireEvent.press(screen.getByRole('button', { name: 'Ghi nhận' }));

    await waitFor(() => expect(outcome()).toBe('Đã ghi nhận định giá'));
  });

  it('cancels an ordinary action without changing anything', async () => {
    await renderWithProviders(<OverlaysCatalog />);

    fireEvent.press(screen.getByRole('button', { name: 'Xác nhận thông thường' }));
    await waitFor(() => expect(screen.getByTestId('confirm-dialog')).toBeTruthy());

    fireEvent.press(screen.getByRole('button', { name: 'Huỷ' }));

    await waitFor(() => expect(outcome()).toBe('Đã huỷ, không thay đổi gì'));
  });

  it('names the consequence before a destructive action', async () => {
    await renderWithProviders(<OverlaysCatalog />);

    fireEvent.press(screen.getByRole('button', { name: 'Xác nhận xoá' }));

    await waitFor(() => expect(screen.getByTestId('destructive-dialog')).toBeTruthy());

    expect(screen.getByTestId('destructive-dialog-consequence')).toHaveTextContent(
      /không khôi phục được/,
    );
  });

  it('takes the safe route when a destructive action is cancelled', async () => {
    await renderWithProviders(<OverlaysCatalog />);

    fireEvent.press(screen.getByRole('button', { name: 'Xác nhận xoá' }));
    await waitFor(() => expect(screen.getByTestId('destructive-dialog')).toBeTruthy());

    fireEvent.press(screen.getByRole('button', { name: 'Huỷ' }));

    await waitFor(() => expect(outcome()).toBe('Đã huỷ, không thay đổi gì'));
  });

  it('keeps the form when the unsaved-change warning is answered with keep editing', async () => {
    await renderWithProviders(<OverlaysCatalog />);

    fireEvent.press(screen.getByRole('button', { name: 'Cảnh báo chưa lưu' }));
    await waitFor(() => expect(screen.getByTestId('catalog-unsaved')).toBeTruthy());

    fireEvent.press(screen.getByRole('button', { name: 'Tiếp tục chỉnh sửa' }));

    await waitFor(() => expect(outcome()).toBe('Đã quay lại chỉnh sửa'));
  });

  it('offers exactly one way forward from a blocking error', async () => {
    await renderWithProviders(<OverlaysCatalog />);

    fireEvent.press(screen.getByRole('button', { name: 'Lỗi chặn' }));
    await waitFor(() => expect(screen.getByTestId('catalog-blocking')).toBeTruthy());

    expect(screen.getByTestId('catalog-blocking-message')).toHaveTextContent(
      /vẫn được giữ lại/,
    );

    fireEvent.press(screen.getByRole('button', { name: 'Quay lại danh sách' }));

    await waitFor(() => expect(outcome()).toBe('Đã quay lại danh sách'));
  });

  it('opens a sheet and reports the chosen option', async () => {
    await renderWithProviders(<OverlaysCatalog />);

    fireEvent.press(screen.getByRole('button', { name: 'Mở bảng chọn' }));
    await waitFor(() => expect(screen.getByTestId('catalog-sheet')).toBeTruthy());

    fireEvent.press(screen.getByRole('button', { name: 'Tháng này' }));

    await waitFor(() => expect(outcome()).toBe('Đã chọn kỳ tháng này'));
  });
});
