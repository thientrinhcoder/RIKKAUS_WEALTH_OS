import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import {
  PRODUCT_TIME_ZONE,
  formatDate,
  formatRelative,
  formatTimestamp,
  formatUtcSource,
  type IsoDate,
} from '@/time';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { UnavailableValue } from './unavailable-value';

export interface DateTextProps {
  /** A business date (valuation, effective date). `null` renders as unavailable. */
  date: IsoDate | null;
  /** Names the date, for example "Ngày định giá" or "hiệu lực". */
  prefix?: string;
  unavailableReason?: string;
  testID?: string;
}

/** A date-only business fact: `Ngày định giá 12/09/2026`. It never shifts with the device zone. */
export function DateText({
  date,
  prefix,
  unavailableReason = 'Chưa có ngày.',
  testID = 'date-text',
}: DateTextProps) {
  const theme = useAppTheme();

  if (date === null) {
    return <UnavailableValue reason={unavailableReason} testID={testID} />;
  }

  const text = prefix === undefined ? formatDate(date) : `${prefix} ${formatDate(date)}`;

  return (
    <Text style={{ color: theme.colors.onSurface }} testID={testID} variant="bodyLarge">
      {text}
    </Text>
  );
}

export interface TimestampTextProps {
  /** A system instant such as `updatedAt`. `null` renders as unavailable. */
  instant: Date | null;
  /** For example "Cập nhật lúc". */
  prefix?: string;
  /** Adds "Khoảng 2 giờ trước" under the absolute time; it never replaces it. */
  showRelative?: boolean;
  /** Shows the UTC source and the zone, for audit-relevant values. */
  showSource?: boolean;
  /** Injected for a deterministic catalog; defaults to the current time. */
  now?: Date;
  unavailableReason?: string;
  testID?: string;
}

export function TimestampText({
  instant,
  prefix,
  showRelative = false,
  showSource = false,
  now,
  unavailableReason = 'Chưa có thời điểm cập nhật.',
  testID = 'timestamp-text',
}: TimestampTextProps) {
  const theme = useAppTheme();

  if (instant === null) {
    return <UnavailableValue reason={unavailableReason} testID={testID} />;
  }

  const absolute = formatTimestamp(instant);
  const relative = showRelative ? formatRelative(instant, now) : null;

  return (
    <View style={styles.stack} testID={testID}>
      <Text
        style={{ color: theme.colors.onSurface }}
        testID={`${testID}-absolute`}
        variant="bodyLarge"
      >
        {prefix === undefined ? absolute : `${prefix} ${absolute}`}
      </Text>
      {relative === null ? null : (
        <Text
          style={{ color: theme.colors.onSurfaceVariant }}
          testID={`${testID}-relative`}
          variant="bodyMedium"
        >
          {relative}
        </Text>
      )}
      {showSource ? (
        <Text
          style={{ color: theme.colors.onSurfaceVariant }}
          testID={`${testID}-source`}
          variant="bodyMedium"
        >
          {`UTC: ${formatUtcSource(instant)} · Múi giờ: ${PRODUCT_TIME_ZONE}`}
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  stack: {
    gap: spacing.xs,
  },
});
