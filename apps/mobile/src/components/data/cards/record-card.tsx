import { View } from 'react-native';
import { Text } from 'react-native-paper';

import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { CardShell } from './card-shell';
import { UNAVAILABLE_LABEL } from './kpi-card';

export interface RecordCardProps {
  title: string;
  category?: string;
  /** Already formatted. Absent renders the explicit unavailable state. */
  primaryValue?: string;
  /** For example the original currency amount beside its converted value. */
  secondaryValue?: string;
  date?: string;
  onPress?: () => void;
  testID?: string;
}

/**
 * The summary card for one record. Domain-neutral: it takes a title, a category, values and a
 * date, and does not know whether it is showing an asset, a liability or a cash-flow entry.
 */
export function RecordCard({
  title,
  category,
  primaryValue,
  secondaryValue,
  date,
  onPress,
  testID,
}: RecordCardProps) {
  const theme = useAppTheme();
  const available = primaryValue !== undefined && primaryValue !== '';

  return (
    <CardShell
      accessibilityLabel={`${title}: ${available ? primaryValue : UNAVAILABLE_LABEL}`}
      onPress={onPress}
      testID={testID}
    >
      <Text testID={testID === undefined ? undefined : `${testID}-title`} variant="titleMedium">
        {title}
      </Text>

      {category === undefined ? null : (
        <Text
          style={{ color: theme.colors.onSurfaceVariant }}
          testID={testID === undefined ? undefined : `${testID}-category`}
          variant="bodyMedium"
        >
          {category}
        </Text>
      )}

      <View style={{ gap: spacing.xs }}>
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

      {date === undefined ? null : (
        <Text
          style={{ color: theme.colors.onSurfaceVariant }}
          testID={testID === undefined ? undefined : `${testID}-date`}
          variant="bodyMedium"
        >
          {date}
        </Text>
      )}
    </CardShell>
  );
}
