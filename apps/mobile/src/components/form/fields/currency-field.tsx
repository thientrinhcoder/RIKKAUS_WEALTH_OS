import { TextInput } from 'react-native-paper';

import { formatGrouped, parseNumericInput } from '../formatters';
import { TextField, type TextFieldProps } from './text-field';

export interface CurrencyFieldProps
  extends Omit<TextFieldProps, 'keyboardType' | 'onChangeValue' | 'right' | 'autoCapitalize'> {
  /**
   * The normalized, unrounded value. Section 9 keeps calculation precision in the stored value
   * while display shows no more precision than a manual valuation supports.
   */
  value: string;
  onChangeValue?: (normalized: string) => void;
  /** Section 9 requires the unit beside the value, not hidden in a placeholder. */
  unit?: string;
  /** Section 8 accepts negatives only where the product contract permits them. */
  allowNegative?: boolean;
}

export function CurrencyField({
  value,
  onChangeValue,
  unit = '₫',
  allowNegative = false,
  ...rest
}: CurrencyFieldProps) {
  return (
    <TextField
      {...rest}
      autoCapitalize="none"
      keyboardType="numeric"
      onChangeValue={(text) => onChangeValue?.(parseNumericInput(text, { allowNegative }))}
      right={<TextInput.Affix text={unit} />}
      value={formatGrouped(value)}
    />
  );
}
