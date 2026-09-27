import type { PropsWithChildren } from 'react';
import { PaperProvider } from 'react-native-paper';
import { act, fireEvent, renderHook, screen, waitFor } from '@testing-library/react-native';

import { Banner } from '@/components/feedback/banner';
import {
  SNACKBAR_MINIMUM_MS,
  SNACKBAR_WITH_ACTION_MS,
  SnackbarProvider,
  durationFor,
  useSnackbar,
} from '@/components/feedback/snackbar-provider';
import { StatusIndicator, STATUS_TONES } from '@/components/feedback/status-indicator';
import { paperSettings } from '@/providers/app-providers';
import { appTheme } from '@/ui/theme';
import { renderWithProviders } from '../../test/test-utils';

describe('status indicator', () => {
  it.each([...STATUS_TONES])('renders %s with both an icon and text', async (tone) => {
    await renderWithProviders(
      <StatusIndicator label="Nhãn do miền nghiệp vụ cung cấp" testID="st" tone={tone} />,
    );

    expect(screen.getByTestId('st-icon')).toBeTruthy();
    expect(screen.getByTestId('st-label')).toHaveTextContent(
      'Nhãn do miền nghiệp vụ cung cấp',
    );
  });

  it('covers exactly the five generic tones, with no goal vocabulary among them', () => {
    expect([...STATUS_TONES]).toEqual(['success', 'warning', 'error', 'info', 'stale']);
  });

  it('renders whatever label the caller supplies, including goal language from the domain', async () => {
    await renderWithProviders(
      <StatusIndicator label="Chậm tiến độ" testID="st" tone="warning" />,
    );

    expect(screen.getByTestId('st-label')).toHaveTextContent('Chậm tiến độ');
  });

  it('exposes the label as the accessible name', async () => {
    await renderWithProviders(<StatusIndicator label="Cần chú ý" testID="st" tone="warning" />);

    expect(screen.getByTestId('st').props.accessibilityLabel).toBe('Cần chú ý');
  });
});

describe('banner', () => {
  it('stays until it is dismissed, and its dismiss control has an accessible name', async () => {
    const onDismiss = jest.fn();

    await renderWithProviders(
      <Banner
        message="Tỷ giá USD đang dùng bản cập nhật ngày 12/09/2026."
        onDismiss={onDismiss}
        testID="b"
      />,
    );

    expect(screen.getByTestId('b-message')).toHaveTextContent(/12\/09\/2026/);

    fireEvent.press(screen.getByRole('button', { name: 'Đóng thông báo' }));

    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it('announces politely rather than interrupting', async () => {
    await renderWithProviders(
      <Banner message="Tỷ giá USD đang dùng bản cũ." onDismiss={jest.fn()} testID="b" />,
    );

    expect(screen.getByTestId('b').props.accessibilityLiveRegion).toBe('polite');
  });

  it('carries an optional action alongside dismissal', async () => {
    const onAction = jest.fn();

    await renderWithProviders(
      <Banner
        actionLabel="Cập nhật tỷ giá"
        message="Tỷ giá USD đang dùng bản cũ."
        onAction={onAction}
        onDismiss={jest.fn()}
        testID="b"
      />,
    );

    fireEvent.press(screen.getByRole('button', { name: 'Cập nhật tỷ giá' }));

    expect(onAction).toHaveBeenCalledTimes(1);
  });
});

describe('snackbar dwell', () => {
  it('gives a plain message the five-second minimum', () => {
    expect(durationFor({})).toBe(SNACKBAR_MINIMUM_MS);
    expect(SNACKBAR_MINIMUM_MS).toBeGreaterThanOrEqual(5000);
  });

  it('gives a message carrying an action longer, so the action can be reached', () => {
    expect(durationFor({ actionLabel: 'Hoàn tác' })).toBe(SNACKBAR_WITH_ACTION_MS);
    expect(SNACKBAR_WITH_ACTION_MS).toBeGreaterThan(SNACKBAR_MINIMUM_MS);
  });
});

/**
 * The queue is exercised through the hook rather than through button presses. Several presses
 * in one test leave this environment's renderer unusable for the tests that follow, and the
 * queue's contract is about what the provider holds, not about how a button was activated.
 */
function wrapper({ children }: PropsWithChildren) {
  return (
    <PaperProvider settings={paperSettings} theme={appTheme}>
      <SnackbarProvider>{children}</SnackbarProvider>
    </PaperProvider>
  );
}

describe('the snackbar queue', () => {
  it('starts with nothing shown', async () => {
    const { result } = await renderHook(() => useSnackbar(), { wrapper });

    expect(result.current.current).toBeNull();
    expect(result.current.queued).toBe(0);
  });

  it('shows the first message raised', async () => {
    const { result } = await renderHook(() => useSnackbar(), { wrapper });

    await act(async () => {
      result.current.show({ message: 'Đã lưu định giá mới.' });
    });

    expect(result.current.current?.message).toBe('Đã lưu định giá mới.');
    expect(result.current.queued).toBe(0);
  });

  it('queues a second message rather than overwriting the first', async () => {
    const { result } = await renderHook(() => useSnackbar(), { wrapper });

    await act(async () => {
      result.current.show({ message: 'Đã lưu định giá mới.' });
    });
    await act(async () => {
      result.current.show({ message: 'Đã xoá tài sản.', actionLabel: 'Hoàn tác' });
    });

    expect(result.current.current?.message).toBe('Đã lưu định giá mới.');
    expect(result.current.queued).toBe(1);
  });

  it('promotes the queued message when the first is dismissed', async () => {
    const { result } = await renderHook(() => useSnackbar(), { wrapper });

    await act(async () => {
      result.current.show({ message: 'Đã lưu định giá mới.' });
    });
    await act(async () => {
      result.current.show({ message: 'Đã xoá tài sản.', actionLabel: 'Hoàn tác' });
    });
    await act(async () => {
      result.current.dismiss();
    });

    expect(result.current.current?.message).toBe('Đã xoá tài sản.');
    expect(result.current.current?.actionLabel).toBe('Hoàn tác');
    expect(result.current.queued).toBe(0);
  });

  it('shows nothing once every message has been dismissed', async () => {
    const { result } = await renderHook(() => useSnackbar(), { wrapper });

    await act(async () => {
      result.current.show({ message: 'Đã lưu định giá mới.' });
    });
    await act(async () => {
      result.current.dismiss();
    });

    expect(result.current.current).toBeNull();
  });

  it('gives each message its own identity, so two identical texts both get shown', async () => {
    const { result } = await renderHook(() => useSnackbar(), { wrapper });

    await act(async () => {
      result.current.show({ message: 'Đã lưu định giá mới.' });
    });
    await act(async () => {
      result.current.show({ message: 'Đã lưu định giá mới.' });
    });

    expect(result.current.queued).toBe(1);
  });
});

describe('the snackbar surface', () => {
  it('renders the current message politely and without taking focus', async () => {
    const { result } = await renderHook(() => useSnackbar(), { wrapper });

    await act(async () => {
      result.current.show({ message: 'Đã lưu định giá mới.' });
    });

    await waitFor(() => expect(screen.getByTestId('snackbar')).toBeTruthy());

    const snackbar = screen.getByTestId('snackbar');

    expect(snackbar.props.accessibilityLiveRegion).toBe('polite');
    expect(snackbar.props.accessible).toBeFalsy();
    expect(screen.getByTestId('snackbar-message')).toHaveTextContent('Đã lưu định giá mới.');
  });

  it('renders the action of the message that is on screen', async () => {
    const { result } = await renderHook(() => useSnackbar(), { wrapper });

    await act(async () => {
      result.current.show({ message: 'Đã xoá tài sản.', actionLabel: 'Hoàn tác' });
    });

    await waitFor(() => expect(screen.getByRole('button', { name: 'Hoàn tác' })).toBeTruthy());
  });
});
