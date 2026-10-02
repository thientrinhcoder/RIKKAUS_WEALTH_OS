import { TextInput } from 'react-native-paper';

import { formatGrouped, parseNumericInput } from '../formatters';
import { TextField, type TextFieldProps } from './text-field';

export interface NumberFieldProps
  extends Omit<TextFieldProps, 'keyboardType' | 'onChangeValue' | 'right' | 'autoCapitalize'> {
  value: string;
  onChangeValue?: (normalized: string) => void;
  unit?: string;
  allowNegative?: boolean;
  /** Rates and percentages need decimals; counts do not. */
  allowDecimal?: boolean;
}

export function NumberField({
  value,
  onChangeValue,
  unit,
  allowNegative = false,
  allowDecimal = true,
  ...rest
}: NumberFieldProps) {
  return (
    <TextField
      {...rest}
      autoCapitalize="none"
      keyboardType={allowDecimal ? 'decimal-pad' : 'numeric'}
      onChangeValue={(text) =>
        onChangeValue?.(parseNumericInput(text, { allowNegative, allowDecimal }))
      }
      right={unit === undefined ? undefined : <TextInput.Affix text={unit} />}
      value={formatGrouped(value)}
    />
  );
}
