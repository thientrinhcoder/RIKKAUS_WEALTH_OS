import { useQuery } from '@tanstack/react-query';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';
import { z } from 'zod';

import { classifyApiError, useApiClient } from '@/api';
import { ActionButton } from '@/components/action';
import { ErrorState, LoadingState, StateView } from '@/components/feedback';
import { ScreenHeading } from '@/components/shell/screen-heading';
import { DateText, MoneyText } from '@/components/value';
import { fromApiValue } from '@/money';
import { parseIsoDate } from '@/time';
import { radius, sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * Opens one asset by id. This is where the ownership boundary becomes visible: an id that belongs
 * to another account answers `not-found` exactly like one that never existed, and both show the
 * same non-enumerating message. The full asset detail (history, edit, delete) arrives with the
 * asset slice; this screen shows only what identifies the record.
 */
const AssetSummarySchema = z.object({
  id: z.string(),
  name: z.string(),
  currentValue: z.number(),
  currency: z.enum(['VND', 'USD']),
  valuationDate: z.string(),
});

/** Answers that must not reveal whether the record exists or whose it is. */
const UNAVAILABLE_PROBLEMS = new Set([
  'urn:rikkaus:problem:not-found',
  'urn:rikkaus:problem:forbidden',
  'urn:rikkaus:problem:validation-failed',
  'urn:rikkaus:problem:malformed-request',
]);

export default function AssetScreen() {
  const theme = useAppTheme();
  const router = useRouter();
  const client = useApiClient();
  const { assetId } = useLocalSearchParams<{ assetId: string }>();

  const query = useQuery({
    queryKey: ['assets', assetId],
    queryFn: async ({ signal }) =>
      (
        await client.request('get', '/api/v1/assets/{assetId}', {
          pathParams: { assetId },
          schema: AssetSummarySchema,
          signal,
        })
      ).data,
  });

  if (query.isPending) {
    return <LoadingState accessibilityLabel="Đang mở tài sản" reservedHeight={160} />;
  }

  if (query.isError) {
    const failure = classifyApiError(query.error);

    if (failure.problemType !== null && UNAVAILABLE_PROBLEMS.has(failure.problemType)) {
      return (
        <View style={styles.screen}>
          <StateView
            action={
              <View style={styles.actions}>
                <ActionButton
                  onPress={() => router.replace('/')}
                  testID="asset-unavailable-overview"
                  variant="primary"
                >
                  Quay lại Tổng quan
                </ActionButton>
                <ActionButton
                  onPress={() => (router.canGoBack() ? router.back() : router.replace('/'))}
                  testID="asset-unavailable-back"
                  variant="text"
                >
                  Về trang trước an toàn
                </ActionButton>
              </View>
            }
            detail={
              <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyLarge">
                Nội dung không khả dụng. Không có thông tin nào về bản ghi hoặc chủ sở hữu được hiển
                thị.
              </Text>
            }
            icon="close-circle-outline"
            message="Không thể mở mục này"
            testID="asset-unavailable"
            tone="neutral"
          />
        </View>
      );
    }

    return (
      <ErrorState
        message={failure.title}
        onRetry={() => void query.refetch()}
        onSafeReturn={() => router.replace('/')}
        preserved={[failure.message, failure.referenceLabel].filter(Boolean).join(' ')}
        retryLabel="Thử lại"
        safeReturnLabel="Quay lại Tổng quan"
        testID="asset-error"
        variant={failure.variant}
      />
    );
  }

  const asset = query.data;

  return (
    <View style={styles.screen} testID="asset-summary">
      <ScreenHeading>{asset.name}</ScreenHeading>
      <View
        style={[
          styles.card,
          { backgroundColor: theme.colors.surface, borderColor: theme.colors.outlineVariant },
        ]}
      >
        <MoneyText
          amount={fromApiValue(asset.currentValue)}
          currency={asset.currency}
          variant="titleLarge"
        />
        <DateText date={parseIsoDate(asset.valuationDate)} prefix="Ngày định giá" />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    gap: spacing.md,
    maxWidth: sizing.contentMaxWidth,
  },
  card: {
    borderRadius: radius.card,
    borderWidth: 1,
    gap: spacing.xs,
    padding: spacing.md,
  },
  actions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
});
