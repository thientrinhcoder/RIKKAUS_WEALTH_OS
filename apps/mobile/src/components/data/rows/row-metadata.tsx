import { View } from 'react-native';
import { Text } from 'react-native-paper';

import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

export interface RowMetadataProps {
  items: readonly (string | undefined)[];
  testID?: string;
}

/**
 * The secondary line of a row: category, date, source. Section 5.2 prefers wrapping over
 * truncation, so the items wrap onto another line rather than being clipped.
 */
export function RowMetadata({ items, testID }: RowMetadataProps) {
  const theme = useAppTheme();
  const present = items.filter((item): item is string => item !== undefined && item !== '');

  if (present.length === 0) {
    return null;
  }

  return (
    <View style={styles.row} testID={testID}>
      {present.map((item, index) => (
        <Text
          key={item}
          style={{ color: theme.colors.onSurfaceVariant }}
          variant="bodyMedium"
        >
          {index === 0 ? item : `· ${item}`}
        </Text>
      ))}
    </View>
  );
}

const styles = {
  row: {
    flexDirection: 'row' as const,
    flexWrap: 'wrap' as const,
    gap: spacing.xs,
  },
};
