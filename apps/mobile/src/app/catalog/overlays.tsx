import { useState } from 'react';
import { View } from 'react-native';

import { ActionButton } from '@/components/action';
import { TextField } from '@/components/form';
import {
  BlockingErrorDialog,
  BottomSheet,
  UnsavedChangesDialog,
  useConfirm,
} from '@/components/overlay';
import { ScreenHeading } from '@/components/shell/screen-heading';
import { spacing } from '@/ui/tokens';
import { STALE_ASSET } from '@/catalog/fixture';

/**
 * The overlay flows, each ending in an observable outcome so a test can tell a safe cancel from
 * a confirmation rather than only checking that a dialog opened.
 */
export default function OverlaysCatalog() {
  const { confirm, dialog } = useConfirm();
  const [outcome, setOutcome] = useState('Chưa có thao tác nào');
  const [dirty, setDirty] = useState(true);
  const [unsavedOpen, setUnsavedOpen] = useState(false);
  const [blockingOpen, setBlockingOpen] = useState(false);
  const [sheetOpen, setSheetOpen] = useState(false);

  return (
    <View style={{ gap: spacing.md }} testID="catalog-overlays">
      <ScreenHeading>Hộp thoại và hành động an toàn</ScreenHeading>

      <ActionButton
        onPress={async () => {
          const confirmed = await confirm({
            title: 'Ghi nhận định giá mới?',
            body: 'Giá trị mới sẽ được dùng cho mọi tính toán từ hôm nay.',
            confirmLabel: 'Ghi nhận',
          });

          setOutcome(confirmed ? 'Đã ghi nhận định giá' : 'Đã huỷ, không thay đổi gì');
        }}
        testID="catalog-open-confirm"
        variant="primary"
      >
        Xác nhận thông thường
      </ActionButton>

      <ActionButton
        onPress={async () => {
          const confirmed = await confirm({
            title: `Xoá ${STALE_ASSET.title}?`,
            consequence:
              'Toàn bộ lịch sử định giá của tài sản này sẽ bị xoá và không khôi phục được.',
            confirmLabel: 'Xoá tài sản',
          });

          setOutcome(confirmed ? 'Đã xoá tài sản' : 'Đã huỷ, không thay đổi gì');
        }}
        testID="catalog-open-destructive"
        variant="destructive"
      >
        Xác nhận xoá
      </ActionButton>

      <ActionButton
        onPress={() => setUnsavedOpen(true)}
        testID="catalog-open-unsaved"
        variant="secondary"
      >
        Cảnh báo chưa lưu
      </ActionButton>

      <ActionButton
        onPress={() => setBlockingOpen(true)}
        testID="catalog-open-blocking"
        variant="secondary"
      >
        Lỗi chặn
      </ActionButton>

      <ActionButton
        onPress={() => setSheetOpen(true)}
        testID="catalog-open-sheet"
        variant="secondary"
      >
        Mở bảng chọn
      </ActionButton>

      <TextField
        label="Kết quả thao tác gần nhất"
        readOnly
        testID="catalog-outcome"
        value={outcome}
      />

      {dialog}

      <UnsavedChangesDialog
        isDirty={dirty}
        onDiscard={() => {
          setDirty(false);
          setUnsavedOpen(false);
          setOutcome('Đã rời đi và bỏ thay đổi');
        }}
        onKeepEditing={() => {
          setUnsavedOpen(false);
          setOutcome('Đã quay lại chỉnh sửa');
        }}
        testID="catalog-unsaved"
        visible={unsavedOpen}
      />

      <BlockingErrorDialog
        message="Không thể mở mục này. Dữ liệu bạn đã nhập vẫn được giữ lại."
        onSafeReturn={() => {
          setBlockingOpen(false);
          setOutcome('Đã quay lại danh sách');
        }}
        safeReturnLabel="Quay lại danh sách"
        testID="catalog-blocking"
        title="Không mở được mục này"
        visible={blockingOpen}
      />

      <BottomSheet
        accessibilityLabel="Chọn kỳ xem"
        onDismiss={() => setSheetOpen(false)}
        testID="catalog-sheet"
        visible={sheetOpen}
      >
        <ActionButton
          onPress={() => {
            setSheetOpen(false);
            setOutcome('Đã chọn kỳ tháng này');
          }}
          testID="catalog-sheet-month"
          variant="text"
        >
          Tháng này
        </ActionButton>
      </BottomSheet>
    </View>
  );
}
