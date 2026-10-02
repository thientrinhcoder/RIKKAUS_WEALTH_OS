import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { Text } from 'react-native-paper';

import { spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * The shared anatomy of every form control, per docs/design-guidelines.md section 8: a
 * permanently visible label, helper text, inline error, a required marker and the disabled and
 * read-only states from the section 16.2 input inventory.
 *
 * Implemented once so no control can quietly ship a different anatomy.
 */

export interface FieldStateProps {
  label: string;
  helperText?: string;
  errorText?: string;
  required?: boolean;
  disabled?: boolean;
  readOnly?: boolean;
}

export interface FieldShellProps extends FieldStateProps {
  children: ReactNode;
  testID?: string;
}

/**
 * The accessible props a control must carry. Returned rather than applied so each control can
 * put them on its own input element, and so the mapping is unit testable without rendering.
 *
 * The aria-* duplicates exist because react-native-web reads those rather than
 * accessibilityState, the same split the navigation shell needed for its selected state.
 */
export function fieldAccessibility(state: FieldStateProps) {
  const invalid = state.errorText !== undefined && state.errorText !== '';

  return {
    accessibilityLabel: state.label,
    accessibilityHint: invalid ? state.errorText : state.helperText,
    accessibilityState: {
      disabled: state.disabled === true,
      invalid,
      required: state.required === true,
    },
    'aria-invalid': invalid || undefined,
    'aria-required': state.required === true ? true : undefined,
    'aria-readonly': state.readOnly === true ? true : undefined,
  } as const;
}

export function FieldShell({
  children,
  testID,
  label,
  helperText,
  errorText,
  required,
  disabled,
}: FieldShellProps) {
  const theme = useAppTheme();
  const hasError = errorText !== undefined && errorText !== '';

  return (
    <View style={styles.field} testID={testID}>
      <View style={styles.labelRow}>
        <Text
          style={{ color: disabled === true ? theme.colors.onSurfaceVariant : theme.colors.onSurface }}
          variant="labelLarge"
        >
          {label}
        </Text>

        {required === true ? (
          <Text
            style={{ color: theme.colors.error }}
            testID={testID === undefined ? undefined : `${testID}-required-marker`}
            variant="labelLarge"
          >
            *
          </Text>
        ) : null}
      </View>

      {children}

      {hasError ? (
        /**
         * Section 11.1 announces a validation error politely and keeps the correction directly
         * below the field it belongs to.
         */
        <Text
          accessibilityLiveRegion="polite"
          style={{ color: theme.colors.error }}
          testID={testID === undefined ? undefined : `${testID}-error`}
          variant="bodyMedium"
        >
          {errorText}
        </Text>
      ) : helperText !== undefined && helperText !== '' ? (
        <Text
          style={{ color: theme.colors.onSurfaceVariant }}
          testID={testID === undefined ? undefined : `${testID}-helper`}
          variant="bodyMedium"
        >
          {helperText}
        </Text>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  field: {
    gap: spacing.xs,
  },
  labelRow: {
    flexDirection: 'row',
    gap: spacing.xs,
  },
});
