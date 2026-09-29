import DateTimePicker from '@react-native-community/datetimepicker';

export interface DatePickerProps {
  /** The currently selected date in ISO form, or empty when nothing is chosen. */
  value: string;
  onChange: (isoDate: string) => void;
  onDismiss: () => void;
  testID?: string;
}

function toDate(isoDate: string): Date {
  if (isoDate === '') {
    return new Date();
  }

  const [year, month, day] = isoDate.split('-').map(Number);

  return new Date(year, month - 1, day);
}

function toIso(date: Date): string {
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');

  return `${date.getFullYear()}-${month}-${day}`;
}

/**
 * The platform date picker on iOS and Android.
 *
 * Web has its own implementation in date-picker.web.tsx, because this package ships no web
 * build. Metro picks the right one per platform.
 */
export function DatePicker({ value, onChange, onDismiss, testID }: DatePickerProps) {
  return (
    <DateTimePicker
      display="spinner"
      mode="date"
      onChange={(event, selected) => {
        if (event.type === 'dismissed' || selected === undefined) {
          onDismiss();
          return;
        }

        onChange(toIso(selected));
      }}
      testID={testID}
      value={toDate(value)}
    />
  );
}
