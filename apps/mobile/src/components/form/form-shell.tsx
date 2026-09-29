import { useEffect, useRef, useState, type ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';

import { ActionButton } from '@/components/action';
import { useSnackbar } from '@/components/feedback';
import { spacing } from '@/ui/tokens';
import { ErrorSummary, type FieldError } from './error-summary';
import { singleFlight } from './single-flight';

export type ValidationResult = readonly FieldError[];

export interface FormShellProps {
  children: ReactNode;
  /**
   * The caller's validation. The kit never encodes what a valid asset valuation is; it only
   * orchestrates when validation runs and what happens to the result.
   */
  validate: () => ValidationResult | Promise<ValidationResult>;
  onSubmit: () => void | Promise<void>;
  /**
   * Raised as a snackbar when the submission succeeds, so the confirmation appears over the
   * screen and clears itself. A success message parked at the foot of the form is easy to miss
   * and stays there long after it stopped being true.
   */
  onCancel?: () => void;
  onFocusField?: (name: string) => void;
  submitLabel: string;
  cancelLabel?: string;
  successMessage?: string;
  /** Optional undo on the success snackbar. The form stores nothing on the caller's behalf. */
  onUndoSubmit?: () => void;
  testID?: string;
}

/**
 * Submit orchestration for every form in the product, per docs/design-guidelines.md section 8:
 * validate on submit, surface an error summary when several fields fail, move focus to the first
 * invalid field, show loading on the submitting action, prevent a duplicate submission, and
 * confirm success.
 *
 * Submit and cancel use the shared button primitive; no button is defined here.
 */
export function FormShell({
  children,
  validate,
  onSubmit,
  onCancel,
  onFocusField,
  submitLabel,
  cancelLabel = 'Huỷ',
  successMessage,
  onUndoSubmit,
  testID = 'form',
}: FormShellProps) {
  const snackbar = useSnackbar();
  const [errors, setErrors] = useState<ValidationResult>([]);
  const [submitting, setSubmitting] = useState(false);

  /**
   * The callers' latest callbacks, read at submit time.
   *
   * A form is normally given inline callbacks, so they are new on every render. Rebuilding the
   * guard from them would hand out a fresh guard each render and defeat it entirely: the second
   * activation would meet a guard that had never seen the first.
   */
  const latest = useRef({ validate, onSubmit, onFocusField, successMessage, onUndoSubmit, snackbar });

  /**
   * Refreshed after each render rather than during it. Effects run before any interaction can
   * reach the button, so a submit never reads a stale callback.
   */
  useEffect(() => {
    latest.current = { validate, onSubmit, onFocusField, successMessage, onUndoSubmit, snackbar };
  });

  /**
   * One guard for the component's lifetime. Duplicate-submission prevention lives here rather
   * than in state because two activations in the same tick would both read a state flag's
   * pre-update value.
   */
  const submit = useRef(
    singleFlight(async () => {
      setSubmitting(true);

      try {
        const found = await latest.current.validate();

        setErrors(found);

        if (found.length > 0) {
          /** Section 8 moves focus to the first invalid field in visual order. */
          latest.current.onFocusField?.(found[0].name);
          return;
        }

        await latest.current.onSubmit();

        if (latest.current.successMessage !== undefined) {
          latest.current.snackbar.show({
            message: latest.current.successMessage,
            actionLabel: latest.current.onUndoSubmit === undefined ? undefined : 'Hoàn tác',
            onAction: latest.current.onUndoSubmit,
          });
        }
      } finally {
        setSubmitting(false);
      }
    }),
  ).current;

  return (
    <View style={styles.form} testID={testID}>
      <ErrorSummary
        errors={errors}
        onFocusField={(name) => onFocusField?.(name)}
        testID={`${testID}-error-summary`}
      />

      {children}

      <View style={styles.actions}>
        {onCancel === undefined ? null : (
          <ActionButton
            disabled={submitting}
            onPress={onCancel}
            testID={`${testID}-cancel`}
            variant="text"
          >
            {cancelLabel}
          </ActionButton>
        )}

        <ActionButton
          loading={submitting}
          onPress={() => {
            void submit();
          }}
          testID={`${testID}-submit`}
          variant="primary"
        >
          {submitLabel}
        </ActionButton>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  form: {
    gap: spacing.md,
  },
  actions: {
    flexDirection: 'row',
    gap: spacing.sm,
    justifyContent: 'flex-end',
  },
});
