import { fireEvent, waitFor } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';

import DiagnosticsScreen from '@/app/diagnostics';
import { useResponsiveLayout } from '@/ui/responsive';

import { renderWithProviders } from '../test/test-utils';

jest.mock('@/ui/responsive', () => {
  const actual = jest.requireActual('@/ui/responsive');

  return {
    ...actual,
    useResponsiveLayout: jest.fn(),
  };
});

const mockedUseResponsiveLayout = jest.mocked(useResponsiveLayout);

function responseWith(payload: unknown, options?: { ok?: boolean; status?: number }): Response {
  return {
    json: jest.fn().mockResolvedValue(payload),
    ok: options?.ok ?? true,
    status: options?.status ?? 200,
  } as unknown as Response;
}

describe('diagnostics screen', () => {
  beforeEach(() => {
    delete process.env.EXPO_PUBLIC_API_BASE_URL;
    globalThis.fetch = jest.fn();
    mockedUseResponsiveLayout.mockReturnValue({ horizontalPadding: 16 });
  });

  it('shows the empty configuration state without making a request', async () => {
    const view = await renderWithProviders(<DiagnosticsScreen />);

    expect(view.getByText('Chưa cấu hình địa chỉ API.')).toBeTruthy();
    expect(view.getByText('Đặt EXPO_PUBLIC_API_BASE_URL rồi khởi động lại Expo.')).toBeTruthy();
    /** The empty state carries a next action rather than being a bare message. */
    expect(view.getByRole('button', { name: 'Xem hướng dẫn cấu hình' })).toBeTruthy();
    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it('shows loading while the initial request is pending', async () => {
    process.env.EXPO_PUBLIC_API_BASE_URL = 'http://localhost:8080';
    globalThis.fetch = jest.fn(() => new Promise<Response>(() => undefined));
    const view = await renderWithProviders(<DiagnosticsScreen />);

    const skeleton = view.getByLabelText('Đang kiểm tra tình trạng API');

    expect(skeleton).toBeTruthy();
    expect(skeleton.props.accessibilityState).toMatchObject({ busy: true });
    /** The skeleton reserves the card's height, so nothing jumps when the result arrives. */
    expect(StyleSheet.flatten(skeleton.props.style)).toMatchObject({ height: 184 });
  });

  it('shows an error and can retry successfully', async () => {
    process.env.EXPO_PUBLIC_API_BASE_URL = 'http://localhost:8080';
    globalThis.fetch = jest
      .fn()
      .mockResolvedValueOnce(responseWith({}, { ok: false, status: 503 }))
      .mockResolvedValueOnce(responseWith({ status: 'UP' }));
    const view = await renderWithProviders(<DiagnosticsScreen />);

    expect(await view.findByText('Kiểm tra tình trạng API thất bại.')).toBeTruthy();
    await fireEvent.press(view.getByRole('button', { name: 'Thử lại' }));

    expect(await view.findByText(/API phản hồi bình thường/)).toBeTruthy();
    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledTimes(2));
  });

  it('shows an error for an invalid configured API URL without calling fetch', async () => {
    process.env.EXPO_PUBLIC_API_BASE_URL = 'not-a-url';
    const view = await renderWithProviders(<DiagnosticsScreen />);

    expect(await view.findByText('Kiểm tra tình trạng API thất bại.')).toBeTruthy();
    /** The error names what was preserved, per the shared state anatomy. */
    expect(view.getByText('The API endpoint configuration is invalid.')).toBeTruthy();
    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it('shows the validated healthy response', async () => {
    process.env.EXPO_PUBLIC_API_BASE_URL = 'http://localhost:8080';
    globalThis.fetch = jest.fn().mockResolvedValue(responseWith({ status: 'UP' }));
    const view = await renderWithProviders(<DiagnosticsScreen />);

    expect(await view.findByText('API phản hồi bình thường. Trạng thái: UP')).toBeTruthy();
    /** Success offers the logical next action rather than a retry for a failure that did not happen. */
    expect(view.getByRole('button', { name: 'Kiểm tra lại' })).toBeTruthy();
    expect(view.queryByRole('button', { name: 'Thử lại' })).toBeNull();
  });

  it('applies the mobile and wide horizontal padding branches', async () => {
    const mobileView = await renderWithProviders(<DiagnosticsScreen />);
    expect(StyleSheet.flatten(mobileView.getByTestId('screen-content').props.contentContainerStyle))
      .toMatchObject({ paddingHorizontal: 16 });
    await mobileView.unmount();

    mockedUseResponsiveLayout.mockReturnValue({ horizontalPadding: 24 });
    const wideView = await renderWithProviders(<DiagnosticsScreen />);
    expect(StyleSheet.flatten(wideView.getByTestId('screen-content').props.contentContainerStyle))
      .toMatchObject({ paddingHorizontal: 24 });
  });
});
