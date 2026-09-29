import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { useAppTheme } from '@/ui/theme';
import { CardShell } from './card-shell';

export interface EmptyCardProps {
  /** Names what is absent, per section 11.1. */
  title: string;
  description?: string;
  /**
   * Exactly one next action. Section 11 requires an empty state to offer the relevant next step
   * rather than a bare message, and more than one choice is no longer a next step.
   */
  actionLabel: string;
  onAction: () => void;
  testID?: string;
}

export function EmptyCard({
  title,
  description,
  actionLabel,
  onAction,
  testID,
}: EmptyCardProps) {
  const theme = useAppTheme();

  return (
    <CardShell testID={testID}>
      <Text testID={testID === undefined ? undefined : `${testID}-title`} variant="titleMedium">
        {title}
      </Text>

      {description === undefined ? null : (
        <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyLarge">
          {description}
        </Text>
      )}

      <ActionButton
        onPress={onAction}
        testID={testID === undefined ? undefined : `${testID}-action`}
        variant="primary"
      >
        {actionLabel}
      </ActionButton>
    </CardShell>
  );
}
