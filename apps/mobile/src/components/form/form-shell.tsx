import { useMemo, useState, type ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { ActionButton } from '@/components/action';
import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
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
  onCancel?: () => void;
  onFocusField?: (name: string) => void;
  submitLabel: string;
  cancelLabel?: string;
  successMessage?: string;
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
  testID = 'form',
}: FormShellProps) {
  const theme = useAppTheme();
  const [errors, setErrors] = useState<ValidationResult>([]);
  const [submitting, setSubmitting] = useState(false);
  const [succeeded, setSucceeded] = useState(false);

  /**
   * Duplicate-submission prevention lives in single-flight rather than in state, because two
   * activations in the same tick would both read a state flag's pre-update value.
   */
  const submit = useMemo(
    () =>
      singleFlight(async () => {
        setSubmitting(true);
        setSucceeded(false);

        try {
          const found = await validate();

          setErrors(found);

          if (found.length > 0) {
            /** Section 8 moves focus to the first invalid field in visual order. */
            onFocusField?.(found[0].name);
            return;
          }

          await onSubmit();
          setSucceeded(true);
        } finally {
          setSubmitting(false);
        }
      }),
    [onFocusField, onSubmit, validate],
  );

  return (
    <View style={styles.form} testID={testID}>
      <ErrorSummary
        errors={errors}
        onFocusField={(name) => onFocusField?.(name)}
        testID={`${testID}-error-summary`}
      />

      {children}

      {succeeded && successMessage !== undefined ? (
        <Text
          accessibilityLiveRegion="polite"
          style={{ color: theme.colors.success }}
          testID={`${testID}-success`}
          variant="bodyLarge"
        >
          {successMessage}
        </Text>
      ) : null}

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
