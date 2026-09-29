import { StyleSheet, View } from 'react-native';

import { ActionButton } from '@/components/action';
import { spacing } from '@/ui/tokens';
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
  /**
   * Optional undo, per the Product Owner decision of 2026-09-27.
   *
   * A caller that passes it gets an undo affordance here; one that does not still gets a
   * compliant flow, because naming the consequence with a safe cancel route is the requirement
   * and undo is the bonus. This component stores nothing: it neither snapshots what changed nor
   * schedules a deferred commit. What is restorable, and for how long, belongs to whichever
   * domain owns the consequence.
   */
  onUndo?: () => void;
  undoLabel?: string;
  testID?: string;
}

export function SuccessState({
  message,
  nextActionLabel,
  onNextAction,
  onUndo,
  undoLabel = 'Hoàn tác',
  testID = 'success-state',
}: SuccessStateProps) {
  return (
    <StateView
      action={
        <View style={styles.actions}>
          <ActionButton
            onPress={onNextAction}
            testID={`${testID}-action-button`}
            variant="secondary"
          >
            {nextActionLabel}
          </ActionButton>

          {onUndo === undefined ? null : (
            <ActionButton onPress={onUndo} testID={`${testID}-undo`} variant="text">
              {undoLabel}
            </ActionButton>
          )}
        </View>
      }
      icon="check-circle-outline"
      message={message}
      testID={testID}
      tone="success"
    />
  );
}

const styles = StyleSheet.create({
  actions: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
});
