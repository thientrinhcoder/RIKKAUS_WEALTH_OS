import { act, renderHook } from '@testing-library/react-native';

import { useConfirm } from '@/components/overlay/use-confirm';

describe('useConfirm', () => {
  it('shows no dialog until one is asked for', async () => {
    const { result } = await renderHook(() => useConfirm());

    expect(result.current.dialog).toBeNull();
  });

  it('resolves true when the user confirms', async () => {
    const { result } = await renderHook(() => useConfirm());

    let outcome: Promise<boolean>;

    await act(async () => {
      outcome = result.current.confirm({
        title: 'Ghi nhận định giá mới?',
        body: 'Giá trị mới sẽ được dùng cho mọi tính toán từ hôm nay.',
        confirmLabel: 'Ghi nhận',
      });
    });

    expect(result.current.dialog).not.toBeNull();

    await act(async () => {
      const dialog = result.current.dialog as { props: { onConfirm: () => void } };
      dialog.props.onConfirm();
    });

    await expect(outcome!).resolves.toBe(true);
    expect(result.current.dialog).toBeNull();
  });

  it('resolves false when the user cancels', async () => {
    const { result } = await renderHook(() => useConfirm());

    let outcome: Promise<boolean>;

    await act(async () => {
      outcome = result.current.confirm({
        title: 'Ghi nhận định giá mới?',
        body: 'Giá trị mới sẽ được dùng cho mọi tính toán từ hôm nay.',
        confirmLabel: 'Ghi nhận',
      });
    });

    await act(async () => {
      const dialog = result.current.dialog as { props: { onCancel: () => void } };
      dialog.props.onCancel();
    });

    await expect(outcome!).resolves.toBe(false);
  });

  it('raises the destructive dialog when a consequence is named', async () => {
    const { result } = await renderHook(() => useConfirm());

    await act(async () => {
      void result.current.confirm({
        title: 'Xoá căn hộ Thảo Điền?',
        consequence: 'Toàn bộ 4 bản ghi định giá sẽ bị xoá và không khôi phục được.',
        confirmLabel: 'Xoá tài sản',
      });
    });

    const dialog = result.current.dialog as { props: { consequence?: string } };

    expect(dialog.props.consequence).toBe(
      'Toàn bộ 4 bản ghi định giá sẽ bị xoá và không khôi phục được.',
    );
  });

  it('never opens a second overlay while one is still open', async () => {
    const { result } = await renderHook(() => useConfirm());

    let first: Promise<boolean>;
    let second: Promise<boolean>;

    await act(async () => {
      first = result.current.confirm({ title: 'Câu hỏi thứ nhất', confirmLabel: 'Đồng ý' });
      second = result.current.confirm({ title: 'Câu hỏi thứ hai', confirmLabel: 'Đồng ý' });
    });

    /** The second resolves immediately as declined rather than stacking a dialog on top. */
    await expect(second!).resolves.toBe(false);

    const dialog = result.current.dialog as { props: { title: string } };

    expect(dialog.props.title).toBe('Câu hỏi thứ nhất');

    await act(async () => {
      (result.current.dialog as { props: { onCancel: () => void } }).props.onCancel();
    });

    await expect(first!).resolves.toBe(false);
  });

  it('accepts a new request once the previous one has settled', async () => {
    const { result } = await renderHook(() => useConfirm());

    await act(async () => {
      void result.current.confirm({ title: 'Câu hỏi thứ nhất', confirmLabel: 'Đồng ý' });
    });

    await act(async () => {
      (result.current.dialog as { props: { onCancel: () => void } }).props.onCancel();
    });

    await act(async () => {
      void result.current.confirm({ title: 'Câu hỏi thứ hai', confirmLabel: 'Đồng ý' });
    });

    expect((result.current.dialog as { props: { title: string } }).props.title).toBe(
      'Câu hỏi thứ hai',
    );
  });
});
