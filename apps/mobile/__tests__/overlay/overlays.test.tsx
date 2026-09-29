import { StyleSheet } from 'react-native';
import { fireEvent, screen } from '@testing-library/react-native';
import { Text } from 'react-native-paper';

import { BlockingErrorDialog } from '@/components/overlay/blocking-error-dialog';
import { BottomSheet } from '@/components/overlay/bottom-sheet';
import { ConfirmDialog } from '@/components/overlay/confirm-dialog';
import { DestructiveDialog } from '@/components/overlay/destructive-dialog';
import { OverlayShell } from '@/components/overlay/overlay-shell';
import { UnsavedChangesDialog } from '@/components/overlay/unsaved-changes-dialog';
import { elevation } from '@/ui/elevation';
import { radius } from '@/ui/tokens';
import { renderWithProviders } from '../../test/test-utils';

/**
 * The scrim is hidden from assistive technology on purpose: it is decoration, and the modal
 * content is what a screen reader should reach. The query therefore has to opt into hidden
 * elements to find it at all.
 */
function scrim(testID: string) {
  return screen.getByTestId(testID, { includeHiddenElements: true });
}

describe('overlay shell', () => {
  it('renders nothing at all while closed', async () => {
    await renderWithProviders(
      <OverlayShell accessibilityLabel="Hộp thoại" onDismiss={jest.fn()} visible={false}>
        <Text>Nội dung</Text>
      </OverlayShell>,
    );

    expect(screen.queryByTestId('overlay')).toBeNull();
    expect(screen.queryByText('Nội dung')).toBeNull();
  });

  it('announces as modal, so background content is inert to assistive technology', async () => {
    await renderWithProviders(
      <OverlayShell accessibilityLabel="Hộp thoại" onDismiss={jest.fn()} visible>
        <Text>Nội dung</Text>
      </OverlayShell>,
    );

    expect(screen.getByTestId('overlay').props.accessibilityViewIsModal).toBe(true);
  });

  it('hides the scrim itself from assistive technology', async () => {
    await renderWithProviders(
      <OverlayShell accessibilityLabel="Hộp thoại" onDismiss={jest.fn()} visible>
        <Text>Nội dung</Text>
      </OverlayShell>,
    );

    const element = scrim('overlay-scrim');

    expect(element.props.accessibilityElementsHidden).toBe(true);
    expect(element.props.importantForAccessibility).toBe('no-hide-descendants');
  });

  it('dismisses on a scrim tap when nothing is lost by dismissing', async () => {
    const onDismiss = jest.fn();

    await renderWithProviders(
      <OverlayShell accessibilityLabel="Bộ lọc" onDismiss={onDismiss} visible>
        <Text>Nội dung</Text>
      </OverlayShell>,
    );

    fireEvent.press(scrim('overlay-scrim'));

    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it('refuses a scrim tap when dismissing would choose for the user', async () => {
    const onDismiss = jest.fn();

    await renderWithProviders(
      <OverlayShell
        accessibilityLabel="Xoá tài sản"
        dismissPolicy="explicit-only"
        onDismiss={onDismiss}
        visible
      >
        <Text>Nội dung</Text>
      </OverlayShell>,
    );

    fireEvent.press(scrim('overlay-scrim'));

    expect(onDismiss).not.toHaveBeenCalled();
  });

  it('uses the approved scrim colour and modal elevation', async () => {
    await renderWithProviders(
      <OverlayShell accessibilityLabel="Hộp thoại" onDismiss={jest.fn()} visible>
        <Text>Nội dung</Text>
      </OverlayShell>,
    );

    const scrimStyle = StyleSheet.flatten(scrim('overlay-scrim').props.style) as {
      backgroundColor?: string;
    };

    expect(scrimStyle.backgroundColor).toBe(elevation.modal.scrim);
    expect(elevation.modal.level).toBe(3);
  });

  it('uses a dialog radius inside the approved band', () => {
    expect(radius.dialog).toBeGreaterThanOrEqual(20);
    expect(radius.dialog).toBeLessThanOrEqual(24);
    expect(radius.sheet).toBeGreaterThanOrEqual(20);
    expect(radius.sheet).toBeLessThanOrEqual(24);
  });
});

describe('confirmation dialog', () => {
  const PROPS = {
    title: 'Ghi nhận định giá mới?',
    body: 'Giá trị mới sẽ được dùng cho mọi tính toán từ hôm nay.',
    confirmLabel: 'Ghi nhận',
  };

  it('renders the title, the body and both actions', async () => {
    await renderWithProviders(
      <ConfirmDialog {...PROPS} onCancel={jest.fn()} onConfirm={jest.fn()} visible />,
    );

    expect(screen.getByTestId('confirm-dialog-title')).toHaveTextContent(PROPS.title);
    expect(screen.getByTestId('confirm-dialog-body')).toHaveTextContent(PROPS.body);
    expect(screen.getByRole('button', { name: 'Ghi nhận' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Huỷ' })).toBeTruthy();
  });

  it('reports the confirmed choice', async () => {
    const onConfirm = jest.fn();

    await renderWithProviders(
      <ConfirmDialog {...PROPS} onCancel={jest.fn()} onConfirm={onConfirm} visible />,
    );

    fireEvent.press(screen.getByRole('button', { name: 'Ghi nhận' }));

    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('takes the safe route on cancel, changing nothing', async () => {
    const onCancel = jest.fn();
    const onConfirm = jest.fn();

    await renderWithProviders(
      <ConfirmDialog {...PROPS} onCancel={onCancel} onConfirm={onConfirm} visible />,
    );

    fireEvent.press(screen.getByRole('button', { name: 'Huỷ' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });
});

describe('destructive dialog', () => {
  const PROPS = {
    title: 'Xoá căn hộ Thảo Điền?',
    consequence: 'Toàn bộ 4 bản ghi định giá của tài sản này sẽ bị xoá và không khôi phục được.',
    confirmLabel: 'Xoá tài sản',
  };

  it('renders the consequence the caller named', async () => {
    await renderWithProviders(
      <DestructiveDialog {...PROPS} onCancel={jest.fn()} onConfirm={jest.fn()} visible />,
    );

    expect(screen.getByTestId('destructive-dialog-consequence')).toHaveTextContent(
      PROPS.consequence,
    );
  });

  it('separates the destructive action from the safe one rather than sitting them side by side', async () => {
    await renderWithProviders(
      <DestructiveDialog {...PROPS} onCancel={jest.fn()} onConfirm={jest.fn()} visible />,
    );

    const actions = screen.getByTestId('destructive-dialog-confirm');
    const cancel = screen.getByTestId('destructive-dialog-cancel');

    expect(actions).toBeTruthy();
    expect(cancel).toBeTruthy();
    /** A separator sits between them, so the two are not adjacent targets. */
    expect(screen.getByTestId('destructive-dialog-panel')).toBeTruthy();
  });

  it('cancels without confirming', async () => {
    const onCancel = jest.fn();
    const onConfirm = jest.fn();

    await renderWithProviders(
      <DestructiveDialog {...PROPS} onCancel={onCancel} onConfirm={onConfirm} visible />,
    );

    fireEvent.press(screen.getByRole('button', { name: 'Huỷ' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('cannot be dismissed by tapping outside it', async () => {
    const onCancel = jest.fn();

    await renderWithProviders(
      <DestructiveDialog {...PROPS} onCancel={onCancel} onConfirm={jest.fn()} visible />,
    );

    fireEvent.press(scrim('destructive-dialog-scrim'));

    expect(onCancel).not.toHaveBeenCalled();
  });
});

describe('unsaved changes dialog', () => {
  const PROPS = {
    onDiscard: jest.fn(),
    onKeepEditing: jest.fn(),
  };

  it('stays closed when the form holds nothing unsaved', async () => {
    await renderWithProviders(
      <UnsavedChangesDialog {...PROPS} isDirty={false} visible />,
    );

    expect(screen.queryByTestId('unsaved-changes-dialog')).toBeNull();
  });

  it('opens only when the form is actually dirty', async () => {
    await renderWithProviders(<UnsavedChangesDialog {...PROPS} isDirty visible />);

    expect(screen.getByTestId('unsaved-changes-dialog')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Tiếp tục chỉnh sửa' })).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Rời đi và bỏ thay đổi' })).toBeTruthy();
  });

  it('reports the discard intent without mutating anything itself', async () => {
    const onDiscard = jest.fn();

    await renderWithProviders(
      <UnsavedChangesDialog isDirty onDiscard={onDiscard} onKeepEditing={jest.fn()} visible />,
    );

    fireEvent.press(screen.getByRole('button', { name: 'Rời đi và bỏ thay đổi' }));

    expect(onDiscard).toHaveBeenCalledTimes(1);
    expect(onDiscard).toHaveBeenCalledWith();
  });

  it('cannot be dismissed by tapping outside, which would discard the work', async () => {
    const onKeepEditing = jest.fn();

    await renderWithProviders(
      <UnsavedChangesDialog isDirty onDiscard={jest.fn()} onKeepEditing={onKeepEditing} visible />,
    );

    fireEvent.press(scrim('unsaved-changes-dialog-scrim'));

    expect(onKeepEditing).not.toHaveBeenCalled();
  });
});

describe('blocking error dialog', () => {
  const PROPS = {
    title: 'Không mở được mục này',
    message: 'Không thể mở mục này. Dữ liệu bạn đã nhập vẫn được giữ lại.',
    safeReturnLabel: 'Quay lại danh sách',
  };

  it('offers exactly one way forward', async () => {
    await renderWithProviders(
      <BlockingErrorDialog {...PROPS} onSafeReturn={jest.fn()} visible />,
    );

    expect(screen.getAllByRole('button')).toHaveLength(1);
    expect(screen.getByRole('button', { name: 'Quay lại danh sách' })).toBeTruthy();
  });

  it('cannot be dismissed into an ambiguous state by tapping outside', async () => {
    const onSafeReturn = jest.fn();

    await renderWithProviders(
      <BlockingErrorDialog {...PROPS} onSafeReturn={onSafeReturn} visible />,
    );

    fireEvent.press(scrim('blocking-error-dialog-scrim'));

    expect(onSafeReturn).not.toHaveBeenCalled();
  });
});

describe('bottom sheet', () => {
  it('renders whatever content it is given, with a named dismiss control', async () => {
    const onDismiss = jest.fn();

    await renderWithProviders(
      <BottomSheet accessibilityLabel="Thêm bản ghi mới" onDismiss={onDismiss} visible>
        <Text>Chọn loại bản ghi</Text>
      </BottomSheet>,
    );

    expect(screen.getByText('Chọn loại bản ghi')).toBeTruthy();

    fireEvent.press(screen.getByRole('button', { name: 'Đóng' }));

    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it('closes on a scrim tap, because a sheet like this loses nothing', async () => {
    const onDismiss = jest.fn();

    await renderWithProviders(
      <BottomSheet accessibilityLabel="Bộ lọc" onDismiss={onDismiss} visible>
        <Text>Bộ lọc</Text>
      </BottomSheet>,
    );

    fireEvent.press(scrim('bottom-sheet-scrim'));

    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it('refuses a scrim tap when it holds entered data', async () => {
    const onDismiss = jest.fn();

    await renderWithProviders(
      <BottomSheet
        accessibilityLabel="Bộ lọc"
        dismissPolicy="explicit-only"
        onDismiss={onDismiss}
        visible
      >
        <Text>Bộ lọc</Text>
      </BottomSheet>,
    );

    fireEvent.press(scrim('bottom-sheet-scrim'));

    expect(onDismiss).not.toHaveBeenCalled();
  });
});
