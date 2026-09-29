import { TextInput } from 'react-native-paper';

import { sizing } from '@/ui/tokens';
import { FieldShell, fieldAccessibility, type FieldStateProps } from './field-shell';

export interface SearchFieldProps extends FieldStateProps {
  value: string;
  onChangeValue: (value: string) => void;
  onBlur?: () => void;
  placeholder?: string;
  /** Section 8.1 requires the clear control to be part of the field's accessible name. */
  clearAccessibilityLabel?: string;
  testID?: string;
}

/**
 * The search input primitive.
 *
 * Section 8.1's surrounding discovery behaviour — debounce, applied filters, the no-match empty
 * state — belongs to the data and discovery slice, which imports this field rather than forking
 * it. This component owns only the control itself.
 */
export function SearchField({
  value,
  onChangeValue,
  onBlur,
  placeholder,
  clearAccessibilityLabel = 'Xoá từ khoá tìm kiếm',
  testID,
  ...state
}: SearchFieldProps) {
  const accessibility = fieldAccessibility(state);
  const hasQuery = value !== '';

  return (
    <FieldShell {...state} testID={testID}>
      <TextInput
        {...accessibility}
        accessibilityRole="search"
        autoCapitalize="none"
        disabled={state.disabled}
        left={
          /**
           * Decorative only. Without these it renders as a pressable with no accessible name,
           * which a screen reader announces as an unlabelled button that does nothing. The
           * field already declares its search role and carries a visible label.
           */
          <TextInput.Icon
            accessibilityElementsHidden
            focusable={false}
            icon="magnify"
            importantForAccessibility="no-hide-descendants"
          />
        }
        mode="outlined"
        onBlur={onBlur}
        onChangeText={onChangeValue}
        placeholder={placeholder}
        returnKeyType="search"
        right={
          hasQuery ? (
            <TextInput.Icon
              accessibilityLabel={clearAccessibilityLabel}
              icon="close"
              onPress={() => onChangeValue('')}
              testID={testID === undefined ? undefined : `${testID}-clear`}
            />
          ) : undefined
        }
        style={{ minHeight: sizing.minControlHeight }}
        testID={testID === undefined ? undefined : `${testID}-input`}
        value={value}
      />
    </FieldShell>
  );
}
