import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { StatusIndicator } from '@/components/feedback';
import { assessFreshness, formatDate, type IsoDate } from '@/time';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

export interface FreshnessLabels {
  fresh: string;
  stale: string;
  missing: string;
}

const DEFAULT_LABELS: FreshnessLabels = {
  fresh: 'Còn hiệu lực',
  stale: 'Dữ liệu cũ',
  missing: 'Chưa có dữ liệu',
};

export interface FreshnessBadgeProps {
  /** The business date the value is current as of. */
  asOf: IsoDate | null;
  /**
   * The caller's threshold. The product has not fixed one, so there is no default: each domain
   * states its own rule where it uses the badge.
   */
  maxAgeDays: number;
  /** Names the date in the detail line, for example "hiệu lực" or "định giá". */
  datePrefix?: string;
  labels?: Partial<FreshnessLabels>;
  /** Section 11.1: a stale or missing value offers an update path. */
  updateLabel?: string;
  onUpdate?: () => void;
  /** Injected for a deterministic catalog; defaults to today in Asia/Ho_Chi_Minh. */
  today?: IsoDate;
  testID?: string;
}

/**
 * Icon + text + absolute date, never colour alone (section 3.4 and 11.1). The age in days is a
 * supplement; the absolute date is always shown.
 */
export function FreshnessBadge({
  asOf,
  maxAgeDays,
  datePrefix = 'ngày',
  labels,
  updateLabel,
  onUpdate,
  today,
  testID = 'freshness-badge',
}: FreshnessBadgeProps) {
  const theme = useAppTheme();
  const text = { ...DEFAULT_LABELS, ...labels };
  const freshness = assessFreshness(asOf, maxAgeDays, today);

  const tone = freshness.status === 'fresh' ? 'success' : freshness.status === 'stale' ? 'stale' : 'warning';
  const label = text[freshness.status];

  const detail =
    freshness.status === 'missing'
      ? null
      : `${datePrefix} ${formatDate(freshness.asOf)}${
          freshness.ageDays === 0 ? '' : ` · ${freshness.ageDays} ngày trước`
        }`;

  const showAction =
    freshness.status !== 'fresh' && updateLabel !== undefined && onUpdate !== undefined;

  return (
    <View
      accessibilityLiveRegion={freshness.status === 'fresh' ? 'none' : 'polite'}
      style={styles.container}
      testID={testID}
    >
      <StatusIndicator label={label} testID={`${testID}-indicator`} tone={tone} />
      {detail === null ? null : (
        <Text
          style={{ color: theme.colors.onSurfaceVariant }}
          testID={`${testID}-detail`}
          variant="bodyMedium"
        >
          {detail}
        </Text>
      )}
      {showAction ? (
        <ActionButton onPress={onUpdate} testID={`${testID}-action`} variant="secondary">
          {updateLabel}
        </ActionButton>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'flex-start',
    gap: spacing.xs,
  },
});
