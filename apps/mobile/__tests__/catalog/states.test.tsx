import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import StatesCatalog from '@/app/catalog/states';
import { SnackbarProvider } from '@/components/feedback';
import { renderWithProviders } from '../../test/test-utils';

function renderStates() {
  return renderWithProviders(
    <SnackbarProvider>
      <StatesCatalog />
    </SnackbarProvider>,
  );
}

describe('the states example', () => {
  it('renders all six states the contract requires', async () => {
    await renderStates();

    for (const testID of [
      'catalog-loading',
      'catalog-empty',
      'catalog-partial',
      'catalog-stale',
      'catalog-error',
      'catalog-success',
    ]) {
      expect(screen.getByTestId(testID)).toBeTruthy();
    }
  });

  it('reserves space while loading and shows no value at all', async () => {
    await renderStates();

    const skeleton = screen.getByTestId('catalog-loading');

    expect(skeleton.props.accessibilityState).toMatchObject({ busy: true });
    expect(skeleton.props.accessibilityLabel).toBe('Đang tải danh sách tài sản');
  });

  it('gives the empty state a next action rather than a bare message', async () => {
    await renderStates();

    expect(screen.getByTestId('catalog-empty-message')).toHaveTextContent('Chưa có tài sản nào');
    expect(screen.getByRole('button', { name: 'Thêm tài sản đầu tiên' })).toBeTruthy();
  });

  it('gives the partial state all four of its required parts', async () => {
    await renderStates();

    expect(screen.getByTestId('catalog-partial-message')).toHaveTextContent(/4 trong 5 tài sản/);
    expect(screen.getByTestId('catalog-partial-missing')).toHaveTextContent(/chưa có định giá/);
    expect(screen.getByTestId('catalog-partial-impact')).toHaveTextContent(/thấp hơn thực tế/);
    expect(screen.getByRole('button', { name: 'Cập nhật tài sản còn lại' })).toBeTruthy();
  });

  it('gives the stale state an absolute date, a shape and an update path', async () => {
    await renderStates();

    expect(screen.getByTestId('catalog-stale-as-of')).toHaveTextContent(/15\/03\/2026/);
    expect(screen.getByTestId('catalog-stale-icon')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Cập nhật định giá' })).toBeTruthy();
  });

  it('names the affected value in the stale state', async () => {
    await renderStates();

    expect(screen.getByTestId('catalog-stale-message')).toHaveTextContent(/Căn hộ Thảo Điền/);
    expect(screen.getByTestId('catalog-stale-message')).toHaveTextContent(/7\.500\.000\.000 ₫/);
  });

  it('distinguishes the offline error from the request error', async () => {
    await renderStates();

    expect(screen.getByTestId('catalog-error-message')).toHaveTextContent(
      'Không lưu được định giá mới.',
    );
    expect(screen.getByTestId('catalog-offline-message')).toHaveTextContent(
      'Thiết bị đang ngoại tuyến.',
    );
  });

  it('states what was preserved in both error variants', async () => {
    await renderStates();

    expect(screen.getByTestId('catalog-error-preserved')).toHaveTextContent(/vẫn được giữ lại/);
    expect(screen.getByTestId('catalog-offline-preserved')).toHaveTextContent(/vẫn nằm trên máy/);
  });

  it('announces errors assertively and success politely', async () => {
    await renderStates();

    expect(screen.getByTestId('catalog-error').props.accessibilityLiveRegion).toBe('assertive');
    expect(screen.getByTestId('catalog-success').props.accessibilityLiveRegion).toBe('polite');
  });

  it('renders every status tone with an icon and a caller-supplied label', async () => {
    await renderStates();

    const tones = ['success', 'warning', 'error', 'info', 'stale'];

    for (const tone of tones) {
      expect(screen.getByTestId(`catalog-indicator-${tone}-icon`)).toBeTruthy();
      expect(screen.getByTestId(`catalog-indicator-${tone}-label`)).toBeTruthy();
    }
  });

  it('raises a queued snackbar from a screen, proving the provider is reachable', async () => {
    await renderStates();

    fireEvent.press(screen.getByRole('button', { name: 'Hiện thông báo nhanh' }));

    await waitFor(() => expect(screen.getByTestId('snackbar-message')).toBeTruthy());

    expect(screen.getByTestId('snackbar-message')).toHaveTextContent('Đã lưu định giá mới.');
    expect(screen.getByTestId('snackbar').props.accessibilityLiveRegion).toBe('polite');
  });
});
