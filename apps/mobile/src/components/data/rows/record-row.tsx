import { StyleSheet, View } from 'react-native';
import { Text, TouchableRipple } from 'react-native-paper';

import { sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { UNAVAILABLE_LABEL } from '../cards/kpi-card';
import { RowMetadata } from './row-metadata';

export interface RecordRowProps {
  title: string;
  category?: string;
  /** Already formatted. Absent renders the explicit unavailable state, never a zero. */
  primaryValue?: string;
  /** For example an original USD amount beside its converted value. */
  secondaryValue?: string;
  date?: string;
  onPress?: () => void;
  testID?: string;
}

/**
 * One row primitive, configured by props.
 *
 * The five row shapes in section 16.2 — asset, liability, cash flow, obligation and valuation
 * history — differ in which metadata they carry, not in structure, so five near-identical
 * components would be five places to fix the same bug. The row is domain-neutral: it takes a
 * title, metadata and values, and does not know what an asset is.
 *
 * The layout is a column rather than a row with the value pushed right, so a long Vietnamese
 * title at a large text size reflows downward instead of squeezing the amount.
 */
export function RecordRow({
  title,
  category,
  primaryValue,
  secondaryValue,
  date,
  onPress,
  testID,
}: RecordRowProps) {
  const theme = useAppTheme();
  const available = primaryValue !== undefined && primaryValue !== '';

  const content = (
    <View style={styles.content}>
      <Text testID={testID === undefined ? undefined : `${testID}-title`} variant="bodyLarge">
        {title}
      </Text>

      <RowMetadata
        items={[category, date]}
        testID={testID === undefined ? undefined : `${testID}-metadata`}
      />

      <Text
        style={{ color: available ? theme.colors.onSurface : theme.colors.onSurfaceVariant }}
        testID={testID === undefined ? undefined : `${testID}-primary-value`}
        variant="labelLarge"
      >
        {available ? primaryValue : UNAVAILABLE_LABEL}
      </Text>

      {secondaryValue === undefined ? null : (
        <Text
          style={{ color: theme.colors.onSurfaceVariant }}
          testID={testID === undefined ? undefined : `${testID}-secondary-value`}
          variant="bodyMedium"
        >
          {secondaryValue}
        </Text>
      )}
    </View>
  );

  if (onPress === undefined) {
    return (
      <View style={styles.row} testID={testID}>
        {content}
      </View>
    );
  }

  return (
    <TouchableRipple
      accessibilityLabel={`${title}: ${available ? primaryValue : UNAVAILABLE_LABEL}`}
      accessibilityRole="button"
      onPress={onPress}
      style={styles.row}
      testID={testID}
    >
      {content}
    </TouchableRipple>
  );
}

const styles = StyleSheet.create({
  row: {
    justifyContent: 'center',
    minHeight: sizing.minTouchTarget.android,
    paddingVertical: spacing.sm,
  },
  content: {
    gap: spacing.xs,
  },
});
