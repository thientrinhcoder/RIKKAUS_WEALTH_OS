import { View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { StateView } from './state-view';

/**
 * Section 11.1 requires a partial state to carry four things. Each is a required prop, so
 * omitting one is a typecheck failure rather than something a reviewer has to notice.
 */
export interface PartialStateProps {
  /** What information is available. */
  available: string;
  /** Which inputs are missing. */
  missing: string;
  /** How the missing inputs affect the interpretation. */
  impact: string;
  /** The path to completing it. */
  completionLabel: string;
  onComplete: () => void;
  testID?: string;
}

export function PartialState({
  available,
  missing,
  impact,
  completionLabel,
  onComplete,
  testID = 'partial-state',
}: PartialStateProps) {
  const theme = useAppTheme();

  return (
    <StateView
      action={
        <ActionButton onPress={onComplete} testID={`${testID}-action-button`} variant="primary">
          {completionLabel}
        </ActionButton>
      }
      detail={
        <View style={{ gap: spacing.xs }}>
          <Text
            style={{ color: theme.colors.onSurfaceVariant }}
            testID={`${testID}-missing`}
            variant="bodyLarge"
          >
            {missing}
          </Text>
          <Text
            style={{ color: theme.colors.onSurfaceVariant }}
            testID={`${testID}-impact`}
            variant="bodyLarge"
          >
            {impact}
          </Text>
        </View>
      }
      icon="progress-alert"
      message={available}
      testID={testID}
      tone="warning"
    />
  );
}
