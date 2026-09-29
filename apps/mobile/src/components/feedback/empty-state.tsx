import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { useAppTheme } from '@/ui/theme';
import { StateView } from './state-view';

export interface EmptyStateProps {
  /** Names what is absent, per section 11.1. */
  message: string;
  detail?: string;
  /** The relevant next action. Required: an empty state without one is a dead end. */
  actionLabel: string;
  onAction: () => void;
  testID?: string;
}

export function EmptyState({
  message,
  detail,
  actionLabel,
  onAction,
  testID = 'empty-state',
}: EmptyStateProps) {
  const theme = useAppTheme();

  return (
    <StateView
      action={
        <ActionButton onPress={onAction} testID={`${testID}-action-button`} variant="primary">
          {actionLabel}
        </ActionButton>
      }
      detail={
        detail === undefined ? undefined : (
          <Text style={{ color: theme.colors.onSurfaceVariant }} variant="bodyLarge">
            {detail}
          </Text>
        )
      }
      icon="tray-arrow-down"
      message={message}
      testID={testID}
    />
  );
}
