import { formatVietnameseDate, parseVietnameseDate } from '../formatters';
import { TextField, type TextFieldProps } from './text-field';

/** Each cause states what to correct, per section 11.1. */
export const DATE_ERROR_MESSAGES = {
  'invalid-format': 'Nhập ngày theo dạng ngày/tháng/năm, ví dụ 12/09/2026.',
  'invalid-day': 'Ngày này không có trong tháng đã chọn.',
  'invalid-month': 'Tháng phải nằm trong khoảng 01 đến 12.',
} as const;

export interface DateFieldProps
  extends Omit<TextFieldProps, 'keyboardType' | 'onChangeValue' | 'value' | 'autoCapitalize'> {
  /** The unambiguous stored value, in ISO form. */
  value: string;
  onChangeValue?: (isoDate: string) => void;
  onInvalid?: (message: string) => void;
}

export function DateField({ value, onChangeValue, onInvalid, ...rest }: DateFieldProps) {
  return (
    <TextField
      {...rest}
      autoCapitalize="none"
      keyboardType="numeric"
      onChangeValue={(text) => {
        const result = parseVietnameseDate(text);

        if (result.error !== undefined) {
          onInvalid?.(DATE_ERROR_MESSAGES[result.error]);
          return;
        }

        onChangeValue?.(result.value);
      }}
      placeholder="dd/mm/yyyy"
      value={formatVietnameseDate(value)}
    />
  );
}
