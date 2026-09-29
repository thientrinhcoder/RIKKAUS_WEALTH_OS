import { ScrollView, StyleSheet, View } from 'react-native';
import { Card, Text } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';

import { EmptyState } from '@/components/feedback/empty-state';
import { ErrorState } from '@/components/feedback/error-state';
import { LoadingState } from '@/components/feedback/loading-state';
import { SuccessState } from '@/components/feedback/success-state';
import { isApiBaseUrlConfigured } from '@/features/health/health-client';
import { useHealthQuery } from '@/features/health/use-health-query';
import { CONTENT_MAX_WIDTH, useResponsiveLayout } from '@/ui/responsive';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * The foundation diagnostic screen. It is deliberately the first consumer of the shared
 * feedback kit: if these four states cannot be expressed with the primitives, the kit is not
 * finished.
 */
const HEALTH_CARD_HEIGHT = 184;

function HealthStatus() {
  const apiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL;
  const configured = isApiBaseUrlConfigured(apiBaseUrl);
  const healthQuery = useHealthQuery(apiBaseUrl);

  if (!configured) {
    return (
      <View testID="health-state-empty">
        <EmptyState
          actionLabel="Xem hướng dẫn cấu hình"
          detail="Đặt EXPO_PUBLIC_API_BASE_URL rồi khởi động lại Expo."
          message="Chưa cấu hình địa chỉ API."
          onAction={() => {}}
          testID="health-empty"
        />
      </View>
    );
  }

  if (healthQuery.isPending) {
    return (
      <View testID="health-state-loading">
        <LoadingState
          accessibilityLabel="Đang kiểm tra tình trạng API"
          reservedHeight={HEALTH_CARD_HEIGHT}
          testID="health-loading"
        />
      </View>
    );
  }

  if (healthQuery.isError) {
    const reason =
      healthQuery.error instanceof Error
        ? healthQuery.error.message
        : 'Không hoàn tất được lượt kiểm tra.';

    return (
      <View testID="health-state-error">
        <ErrorState
          message="Kiểm tra tình trạng API thất bại."
          onRetry={() => {
            void healthQuery.refetch();
          }}
          preserved={reason}
          retryLabel="Thử lại"
          testID="health-error"
          variant="request"
        />
      </View>
    );
  }

  return (
    <View testID="health-state-success">
      <SuccessState
        message={`API phản hồi bình thường. Trạng thái: ${healthQuery.data.status}`}
        nextActionLabel="Kiểm tra lại"
        onNextAction={() => {
          void healthQuery.refetch();
        }}
        testID="health-success"
      />
    </View>
  );
}

export default function DiagnosticsScreen() {
  const { colors } = useAppTheme();
  const { horizontalPadding } = useResponsiveLayout();

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: colors.background }]}>
      <ScrollView
        contentContainerStyle={[styles.scrollContent, { paddingHorizontal: horizontalPadding }]}
        testID="screen-content"
      >
        <View style={styles.content}>
          <Text accessibilityRole="header" variant="headlineSmall">
            Rikkaus Wealth OS
          </Text>
          <Text variant="bodyLarge">Chẩn đoán nền tảng cho web và di động.</Text>

          <Card accessible accessibilityLabel="Tình trạng API" mode="contained">
            <Card.Content style={styles.cardContent}>
              <Text variant="titleLarge">Tình trạng API</Text>
              <HealthStatus />
            </Card.Content>
          </Card>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  scrollContent: {
    flexGrow: 1,
    paddingVertical: spacing.lg,
  },
  content: {
    alignSelf: 'center',
    gap: spacing.md,
    maxWidth: CONTENT_MAX_WIDTH,
    width: '100%',
  },
  cardContent: {
    gap: spacing.md,
    minHeight: HEALTH_CARD_HEIGHT,
  },
});
