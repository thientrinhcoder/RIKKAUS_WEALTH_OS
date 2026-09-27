import { StyleSheet } from 'react-native';
import { fireEvent, screen } from '@testing-library/react-native';
import { Text } from 'react-native-paper';

import { EmptyState } from '@/components/feedback/empty-state';
import { ErrorState } from '@/components/feedback/error-state';
import { LoadingState } from '@/components/feedback/loading-state';
import { PartialState } from '@/components/feedback/partial-state';
import { StaleState } from '@/components/feedback/stale-state';
import { StateView } from '@/components/feedback/state-view';
import { SuccessState } from '@/components/feedback/success-state';
import { renderWithProviders } from '../../test/test-utils';

describe('the shared state anatomy', () => {
  it('renders the message, the detail and the action', async () => {
    await renderWithProviders(
      <StateView
        action={<Text>Cập nhật định giá</Text>}
        detail={<Text>Giá trị gần nhất ghi nhận ngày 12/09/2026.</Text>}
        message="Định giá đã cũ"
        testID="s"
      />,
    );

    expect(screen.getByTestId('s-message')).toHaveTextContent('Định giá đã cũ');
    expect(screen.getByTestId('s-detail')).toBeTruthy();
    expect(screen.getByTestId('s-action')).toBeTruthy();
  });

  it('carries an icon alongside a tone, so no state is colour-only', async () => {
    await renderWithProviders(
      <StateView icon="alert-outline" message="Không tải được dữ liệu" testID="s" tone="error" />,
    );

    expect(screen.getByTestId('s-icon')).toBeTruthy();
    expect(screen.getByTestId('s-message')).toHaveTextContent('Không tải được dữ liệu');
  });

  it('announces politely by default', async () => {
    await renderWithProviders(<StateView message="Đã lưu thay đổi" testID="s" />);

    expect(screen.getByTestId('s').props.accessibilityLiveRegion).toBe('polite');
    expect(screen.getByTestId('s').props.accessibilityRole).toBeUndefined();
  });

  it('announces assertively and as an alert only when told to', async () => {
    await renderWithProviders(
      <StateView live="assertive" message="Không lưu được" testID="s" />,
    );

    expect(screen.getByTestId('s').props.accessibilityLiveRegion).toBe('assertive');
    expect(screen.getByTestId('s').props.accessibilityRole).toBe('alert');
  });
});

describe('empty state', () => {
  it('names what is absent and offers exactly one next action', async () => {
    const onAction = jest.fn();

    await renderWithProviders(
      <EmptyState
        actionLabel="Thêm tài sản đầu tiên"
        detail="Thêm tài sản để bắt đầu theo dõi giá trị ròng."
        message="Chưa có tài sản nào"
        onAction={onAction}
        testID="empty"
      />,
    );

    expect(screen.getByTestId('empty-message')).toHaveTextContent('Chưa có tài sản nào');
    expect(screen.getAllByRole('button')).toHaveLength(1);

    fireEvent.press(screen.getByRole('button', { name: 'Thêm tài sản đầu tiên' }));

    expect(onAction).toHaveBeenCalledTimes(1);
  });
});

describe('loading state', () => {
  it('reserves exactly the height the loaded content will occupy', async () => {
    await renderWithProviders(
      <LoadingState accessibilityLabel="Đang tải danh sách tài sản" reservedHeight={184} />,
    );

    const style = StyleSheet.flatten(
      screen.getByTestId('loading-state').props.style,
    ) as { height?: number };

    expect(style.height).toBe(184);
  });

  it('exposes a busy state naming what is loading', async () => {
    await renderWithProviders(
      <LoadingState accessibilityLabel="Đang tải danh sách tài sản" reservedHeight={184} />,
    );

    const skeleton = screen.getByTestId('loading-state');

    expect(skeleton.props.accessibilityState).toMatchObject({ busy: true });
    expect(skeleton.props.accessibilityLabel).toBe('Đang tải danh sách tài sản');
  });

  it('renders no value at all, so no placeholder number can be read as real', async () => {
    await renderWithProviders(
      <LoadingState accessibilityLabel="Đang tải giá trị ròng" reservedHeight={120} />,
    );

    expect(screen.queryByText(/\d/)).toBeNull();
    expect(screen.queryByText(/₫/)).toBeNull();
  });
});

describe('partial state', () => {
  it('renders all four elements the contract requires', async () => {
    const onComplete = jest.fn();

    await renderWithProviders(
      <PartialState
        available="Đã tính trên 8 trong 11 tài sản"
        completionLabel="Cập nhật 3 tài sản còn lại"
        impact="Giá trị ròng hiển thị đang thấp hơn thực tế."
        missing="3 tài sản chưa có định giá."
        onComplete={onComplete}
        testID="partial"
      />,
    );

    expect(screen.getByTestId('partial-message')).toHaveTextContent(
      'Đã tính trên 8 trong 11 tài sản',
    );
    expect(screen.getByTestId('partial-missing')).toHaveTextContent('3 tài sản chưa có định giá.');
    expect(screen.getByTestId('partial-impact')).toHaveTextContent(/thấp hơn thực tế/);

    fireEvent.press(screen.getByRole('button', { name: 'Cập nhật 3 tài sản còn lại' }));

    expect(onComplete).toHaveBeenCalledTimes(1);
  });
});

describe('stale state', () => {
  it('names the affected value, carries an absolute date, and offers an update path', async () => {
    const onUpdate = jest.fn();

    await renderWithProviders(
      <StaleState
        affectedValue="Giá trị căn hộ Thảo Điền"
        asOfDate="Định giá gần nhất 12/09/2026"
        onUpdate={onUpdate}
        statusLabel="Cần cập nhật"
        testID="stale"
        updateLabel="Cập nhật định giá"
      />,
    );

    expect(screen.getByTestId('stale-message')).toHaveTextContent('Giá trị căn hộ Thảo Điền');
    expect(screen.getByTestId('stale-as-of')).toHaveTextContent(/12\/09\/2026/);

    fireEvent.press(screen.getByRole('button', { name: 'Cập nhật định giá' }));

    expect(onUpdate).toHaveBeenCalledTimes(1);
  });

  it('carries a shape as well as text, so staleness survives without colour', async () => {
    await renderWithProviders(
      <StaleState
        affectedValue="Giá trị căn hộ Thảo Điền"
        asOfDate="Định giá gần nhất 12/09/2026"
        onUpdate={jest.fn()}
        statusLabel="Cần cập nhật"
        testID="stale"
        updateLabel="Cập nhật định giá"
      />,
    );

    expect(screen.getByTestId('stale-icon')).toBeTruthy();
    expect(screen.getByTestId('stale-indicator-icon')).toBeTruthy();
    expect(screen.getByTestId('stale-indicator-label')).toHaveTextContent('Cần cập nhật');
  });
});

describe('error state', () => {
  const COMMON = {
    message: 'Không lưu được định giá mới.',
    preserved: 'Giá trị bạn vừa nhập vẫn được giữ lại.',
    retryLabel: 'Thử lại',
  };

  it.each(['request', 'offline'] as const)(
    'states what failed, what was preserved, and the retry path for the %s variant',
    async (variant) => {
      const onRetry = jest.fn();

      await renderWithProviders(
        <ErrorState {...COMMON} onRetry={onRetry} testID="err" variant={variant} />,
      );

      expect(screen.getByTestId('err-message')).toHaveTextContent(COMMON.message);
      expect(screen.getByTestId('err-preserved')).toHaveTextContent(COMMON.preserved);

      fireEvent.press(screen.getByRole('button', { name: 'Thử lại' }));

      expect(onRetry).toHaveBeenCalledTimes(1);
    },
  );

  it('offers a safe way back alongside retry when one is given', async () => {
    const onSafeReturn = jest.fn();

    await renderWithProviders(
      <ErrorState
        {...COMMON}
        onRetry={jest.fn()}
        onSafeReturn={onSafeReturn}
        safeReturnLabel="Quay lại danh sách"
        testID="err"
        variant="request"
      />,
    );

    fireEvent.press(screen.getByRole('button', { name: 'Quay lại danh sách' }));

    expect(onSafeReturn).toHaveBeenCalledTimes(1);
  });

  it('announces assertively, because the user is waiting on the action that failed', async () => {
    await renderWithProviders(
      <ErrorState {...COMMON} onRetry={jest.fn()} testID="err" variant="request" />,
    );

    expect(screen.getByTestId('err').props.accessibilityLiveRegion).toBe('assertive');
  });
});

describe('success state', () => {
  it('states what changed and offers the logical next action, announced politely', async () => {
    const onNextAction = jest.fn();

    await renderWithProviders(
      <SuccessState
        message="Đã lưu định giá mới cho căn hộ Thảo Điền."
        nextActionLabel="Xem lịch sử định giá"
        onNextAction={onNextAction}
        testID="ok"
      />,
    );

    expect(screen.getByTestId('ok-message')).toHaveTextContent(/Đã lưu định giá mới/);
    expect(screen.getByTestId('ok').props.accessibilityLiveRegion).toBe('polite');

    fireEvent.press(screen.getByRole('button', { name: 'Xem lịch sử định giá' }));

    expect(onNextAction).toHaveBeenCalledTimes(1);
  });
});
