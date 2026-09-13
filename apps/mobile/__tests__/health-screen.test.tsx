import { fireEvent, waitFor } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';

import HomeScreen from '@/app/index';
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

describe('health screen', () => {
  beforeEach(() => {
    delete process.env.EXPO_PUBLIC_API_BASE_URL;
    globalThis.fetch = jest.fn();
    mockedUseResponsiveLayout.mockReturnValue({ horizontalPadding: 16 });
  });

  it('shows the empty configuration state without making a request', async () => {
    const view = await renderWithProviders(<HomeScreen />);

    expect(view.getByText('API endpoint is not configured.')).toBeTruthy();
    expect(view.getByText('Set EXPO_PUBLIC_API_BASE_URL and restart Expo.')).toBeTruthy();
    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it('shows loading while the initial request is pending', async () => {
    process.env.EXPO_PUBLIC_API_BASE_URL = 'http://localhost:8080';
    globalThis.fetch = jest.fn(() => new Promise<Response>(() => undefined));
    const view = await renderWithProviders(<HomeScreen />);

    expect(view.getByText('Checking API health…')).toBeTruthy();
    expect(view.getByLabelText('Checking API health')).toBeTruthy();
  });

  it('shows an error and can retry successfully', async () => {
    process.env.EXPO_PUBLIC_API_BASE_URL = 'http://localhost:8080';
    globalThis.fetch = jest
      .fn()
      .mockResolvedValueOnce(responseWith({}, { ok: false, status: 503 }))
      .mockResolvedValueOnce(responseWith({ status: 'UP' }));
    const view = await renderWithProviders(<HomeScreen />);

    expect(await view.findByText('API health check failed.')).toBeTruthy();
    await fireEvent.press(view.getByLabelText('Try API health check again'));

    expect(await view.findByText('API is reachable.')).toBeTruthy();
    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledTimes(2));
  });

  it('shows an error for an invalid configured API URL without calling fetch', async () => {
    process.env.EXPO_PUBLIC_API_BASE_URL = 'not-a-url';
    const view = await renderWithProviders(<HomeScreen />);

    expect(await view.findByText('API health check failed.')).toBeTruthy();
    expect(view.getByText('The API endpoint configuration is invalid.')).toBeTruthy();
    expect(globalThis.fetch).not.toHaveBeenCalled();
  });

  it('shows the validated healthy response', async () => {
    process.env.EXPO_PUBLIC_API_BASE_URL = 'http://localhost:8080';
    globalThis.fetch = jest.fn().mockResolvedValue(responseWith({ status: 'UP' }));
    const view = await renderWithProviders(<HomeScreen />);

    expect(await view.findByText('API is reachable.')).toBeTruthy();
    expect(view.getByText('Status: UP')).toBeTruthy();
    expect(view.queryByText('Try again')).toBeNull();
  });

  it('applies the mobile and wide horizontal padding branches', async () => {
    const mobileView = await renderWithProviders(<HomeScreen />);
    expect(StyleSheet.flatten(mobileView.getByTestId('screen-content').props.contentContainerStyle))
      .toMatchObject({ paddingHorizontal: 16 });
    await mobileView.unmount();

    mockedUseResponsiveLayout.mockReturnValue({ horizontalPadding: 24 });
    const wideView = await renderWithProviders(<HomeScreen />);
    expect(StyleSheet.flatten(wideView.getByTestId('screen-content').props.contentContainerStyle))
      .toMatchObject({ paddingHorizontal: 24 });
  });
});
