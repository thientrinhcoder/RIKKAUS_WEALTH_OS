import { useState } from 'react';
import { TextInput } from 'react-native-paper';

import { formatVietnameseDate, parseVietnameseDate } from '../formatters';
import { DatePicker } from './date-picker';
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
  /** Names the control that opens the picker. */
  pickerAccessibilityLabel?: string;
}

/**
 * A date field with two ways in, as section 16.2 requires: the platform picker, and typing.
 *
 * Typing is the alternative rather than the only route. Someone entering a valuation from a
 * document usually knows the date and types it faster than they can scroll to it; someone
 * choosing a due date usually wants the calendar.
 */
export function DateField({
  value,
  onChangeValue,
  onInvalid,
  pickerAccessibilityLabel = 'Mở bộ chọn ngày',
  testID,
  ...rest
}: DateFieldProps) {
  const [pickerOpen, setPickerOpen] = useState(false);

  return (
    <>
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
        right={
          <TextInput.Icon
            accessibilityLabel={pickerAccessibilityLabel}
            disabled={rest.disabled === true || rest.readOnly === true}
            icon="calendar"
            onPress={() => setPickerOpen(true)}
            testID={testID === undefined ? undefined : `${testID}-open-picker`}
          />
        }
        testID={testID}
        value={formatVietnameseDate(value)}
      />

      {pickerOpen ? (
        <DatePicker
          onChange={(isoDate) => {
            setPickerOpen(false);
            onChangeValue?.(isoDate);
          }}
          onDismiss={() => setPickerOpen(false)}
          testID={testID === undefined ? undefined : `${testID}-picker`}
          value={value}
        />
      ) : null}
    </>
  );
}
