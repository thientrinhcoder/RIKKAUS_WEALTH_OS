import { TextInput } from 'react-native-paper';

import { sizing } from '@/ui/tokens';
import { FieldShell, fieldAccessibility, type FieldStateProps } from './field-shell';

export interface TextFieldProps extends FieldStateProps {
  value: string;
  onChangeValue?: (value: string) => void;
  onBlur?: () => void;
  placeholder?: string;
  testID?: string;
  multiline?: boolean;
  numberOfLines?: number;
  keyboardType?: 'default' | 'numeric' | 'decimal-pad' | 'email-address';
  autoCapitalize?: 'none' | 'sentences';
  right?: React.ReactNode;
}

/**
 * The plain text control, and the shared input body every other typed control reuses.
 *
 * Section 8 keeps the label permanently visible, so the Paper input is rendered without its own
 * floating label and never receives a placeholder that would substitute for one.
 */
export function TextField({
  value,
  onChangeValue,
  onBlur,
  placeholder,
  testID,
  multiline,
  numberOfLines,
  keyboardType = 'default',
  autoCapitalize = 'sentences',
  right,
  ...state
}: TextFieldProps) {
  const accessibility = fieldAccessibility(state);

  return (
    <FieldShell {...state} testID={testID}>
      <TextInput
        {...accessibility}
        autoCapitalize={autoCapitalize}
        disabled={state.disabled}
        editable={state.readOnly !== true}
        error={state.errorText !== undefined && state.errorText !== ''}
        keyboardType={keyboardType}
        mode="outlined"
        multiline={multiline}
        numberOfLines={numberOfLines}
        onBlur={onBlur}
        onChangeText={onChangeValue}
        placeholder={placeholder}
        right={right}
        style={{ minHeight: sizing.minControlHeight }}
        testID={testID === undefined ? undefined : `${testID}-input`}
        value={value}
      />
    </FieldShell>
  );
}
