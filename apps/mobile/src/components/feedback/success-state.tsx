import { ActionButton } from '@/components/action';
import { StateView } from './state-view';

/**
 * Section 11.1 requires a success state to identify what changed and offer the logical next
 * action, so both are required props.
 */
export interface SuccessStateProps {
  /** What changed. */
  message: string;
  nextActionLabel: string;
  onNextAction: () => void;
  testID?: string;
}

export function SuccessState({
  message,
  nextActionLabel,
  onNextAction,
  testID = 'success-state',
}: SuccessStateProps) {
  return (
    <StateView
      action={
        <ActionButton
          onPress={onNextAction}
          testID={`${testID}-action-button`}
          variant="secondary"
        >
          {nextActionLabel}
        </ActionButton>
      }
      icon="check-circle-outline"
      message={message}
      testID={testID}
      tone="success"
    />
  );
}
