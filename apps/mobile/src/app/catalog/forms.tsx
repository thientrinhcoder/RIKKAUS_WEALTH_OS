import { useState } from 'react';
import { View } from 'react-native';

import {
  CurrencyField,
  DateField,
  FormShell,
  SelectField,
  TextField,
  TextareaField,
  useUnsavedChanges,
  type ValidationResult,
} from '@/components/form';
import { ScreenHeading } from '@/components/shell/screen-heading';
import { spacing } from '@/ui/tokens';

const ASSET_KINDS = [
  { value: 'cash', label: 'Tiền mặt' },
  { value: 'deposit', label: 'Tiền gửi tiết kiệm' },
  { value: 'securities', label: 'Chứng khoán' },
  { value: 'real-estate', label: 'Bất động sản' },
];

/**
 * A realistic add-asset form composed only from the form kit.
 *
 * The validation below is the example's own, supplied the way a feature would supply it. The kit
 * orchestrates when it runs and what happens to the result; it never learns what a valid asset
 * valuation is.
 */
export default function FormsCatalog() {
  const [name, setName] = useState('');
  const [kind, setKind] = useState('');
  const [value, setValue] = useState('');
  const [valuedOn, setValuedOn] = useState('');
  const [note, setNote] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const unsaved = useUnsavedChanges();

  const validate = (): ValidationResult => {
    const errors = [];

    if (name.trim() === '') {
      errors.push({ name: 'name', label: 'Tên tài sản', message: 'Nhập tên tài sản.' });
    }

    if (kind === '') {
      errors.push({ name: 'kind', label: 'Loại tài sản', message: 'Chọn loại tài sản.' });
    }

    if (value === '' || Number(value) <= 0) {
      errors.push({
        name: 'value',
        label: 'Giá trị hiện tại',
        message: 'Giá trị phải lớn hơn 0.',
      });
    }

    return errors;
  };

  return (
    <View style={{ gap: spacing.lg }} testID="catalog-forms">
      <ScreenHeading>Biểu mẫu và kiểm tra hợp lệ</ScreenHeading>

      <FormShell
        onCancel={() => {
          unsaved.reset();
        }}
        onSubmit={() => {
          setSubmitted(true);
          unsaved.markSaved();
        }}
        submitLabel="Lưu tài sản"
        successMessage="Đã lưu tài sản."
        testID="catalog-form"
        validate={validate}
      >
        <TextField
          label="Tên tài sản"
          onChangeValue={(next) => {
            setName(next);
            unsaved.markChanged();
          }}
          required
          testID="catalog-name"
          value={name}
        />

        <SelectField
          label="Loại tài sản"
          onChangeValue={(next) => {
            setKind(next);
            unsaved.markChanged();
          }}
          options={ASSET_KINDS}
          required
          testID="catalog-kind"
          value={kind}
        />

        <CurrencyField
          helperText="Nhập giá trị hiện tại theo đồng Việt Nam."
          label="Giá trị hiện tại"
          onChangeValue={(next) => {
            setValue(next);
            unsaved.markChanged();
          }}
          required
          testID="catalog-value"
          value={value}
        />

        <DateField
          helperText="Ngày bạn thực sự kiểm tra giá trị này."
          label="Ngày định giá"
          onChangeValue={setValuedOn}
          testID="catalog-valued-on"
          value={valuedOn}
        />

        <TextareaField
          label="Ghi chú"
          onChangeValue={setNote}
          testID="catalog-note"
          value={note}
        />
      </FormShell>

      <TextField
        label="Trạng thái thay đổi chưa lưu"
        readOnly
        testID="catalog-dirty"
        value={unsaved.isDirty ? 'Có thay đổi chưa lưu' : 'Không có thay đổi chưa lưu'}
      />

      <TextField
        label="Kết quả gửi biểu mẫu"
        readOnly
        testID="catalog-submitted"
        value={submitted ? 'Đã gửi thành công' : 'Chưa gửi'}
      />
    </View>
  );
}
