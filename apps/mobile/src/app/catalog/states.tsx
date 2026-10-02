import { View } from 'react-native';

import {
  Banner,
  EmptyState,
  ErrorState,
  LoadingState,
  PartialState,
  StaleState,
  StatusIndicator,
  SuccessState,
  useSnackbar,
} from '@/components/feedback';
import { ActionButton } from '@/components/action';
import { ScreenHeading } from '@/components/shell/screen-heading';
import { spacing } from '@/ui/tokens';
import { EXCHANGE_RATE, STALE_ASSET, formatVnd } from '@/catalog/fixture';

/**
 * Every async and data-quality state, composed only from the feedback kit and filled with the
 * catalog fixture so the copy reads the way the product will.
 */
export default function StatesCatalog() {
  const snackbar = useSnackbar();

  return (
    <View style={{ gap: spacing.lg }} testID="catalog-states">
      <ScreenHeading>Trạng thái và phản hồi</ScreenHeading>

      <LoadingState
        accessibilityLabel="Đang tải danh sách tài sản"
        reservedHeight={184}
        testID="catalog-loading"
      />

      <EmptyState
        actionLabel="Thêm tài sản đầu tiên"
        detail="Thêm tài sản để bắt đầu theo dõi giá trị ròng."
        message="Chưa có tài sản nào"
        onAction={() => {}}
        testID="catalog-empty"
      />

      <PartialState
        available="Đã tính giá trị ròng trên 4 trong 5 tài sản"
        completionLabel="Cập nhật tài sản còn lại"
        impact="Giá trị ròng hiển thị đang thấp hơn thực tế."
        missing="1 tài sản chưa có định giá trong kỳ này."
        onComplete={() => {}}
        testID="catalog-partial"
      />

      <StaleState
        affectedValue={`${STALE_ASSET.title}: ${formatVnd(STALE_ASSET.amount)}`}
        asOfDate="Định giá gần nhất 15/03/2026"
        onUpdate={() => {}}
        statusLabel="Cần cập nhật"
        testID="catalog-stale"
        updateLabel="Cập nhật định giá"
      />

      <ErrorState
        message="Không lưu được định giá mới."
        onRetry={() => {}}
        onSafeReturn={() => {}}
        preserved="Giá trị bạn vừa nhập vẫn được giữ lại."
        retryLabel="Thử lại"
        safeReturnLabel="Quay lại danh sách"
        testID="catalog-error"
        variant="request"
      />

      <ErrorState
        message="Thiết bị đang ngoại tuyến."
        onRetry={() => {}}
        preserved="Thay đổi của bạn vẫn nằm trên máy và sẽ gửi lại khi có mạng."
        retryLabel="Thử lại"
        testID="catalog-offline"
        variant="offline"
      />

      <SuccessState
        message={`Đã lưu định giá mới cho ${STALE_ASSET.title}.`}
        nextActionLabel="Xem lịch sử định giá"
        onNextAction={() => {}}
        testID="catalog-success"
      />

      <View style={{ gap: spacing.sm }} testID="catalog-indicators">
        <StatusIndicator label="Đang ổn" testID="catalog-indicator-success" tone="success" />
        <StatusIndicator label="Cần chú ý" testID="catalog-indicator-warning" tone="warning" />
        <StatusIndicator label="Không tính được" testID="catalog-indicator-error" tone="error" />
        <StatusIndicator label="Đang tham chiếu" testID="catalog-indicator-info" tone="info" />
        <StatusIndicator label="Dữ liệu đã cũ" testID="catalog-indicator-stale" tone="stale" />
      </View>

      <Banner
        actionLabel="Cập nhật tỷ giá"
        message={`Đang dùng tỷ giá ${EXCHANGE_RATE.pair}, cập nhật ${EXCHANGE_RATE.updatedOn}.`}
        onAction={() => {}}
        onDismiss={() => {}}
        testID="catalog-banner"
      />

      <ActionButton
        onPress={() =>
          snackbar.show({ message: 'Đã lưu định giá mới.', actionLabel: 'Hoàn tác' })
        }
        testID="catalog-raise-snackbar"
        variant="secondary"
      >
        Hiện thông báo nhanh
      </ActionButton>
    </View>
  );
}
