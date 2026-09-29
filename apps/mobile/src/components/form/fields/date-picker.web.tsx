import { createElement } from 'react';

import type { DatePickerProps } from './date-picker';

/**
 * The platform date picker on web.
 *
 * `@react-native-community/datetimepicker` ships no web build, and on a phone browser a native
 * date input is the platform picker anyway: iOS Safari and Android Chrome both open their own
 * date wheel for it. So the web target renders a real date input and asks the browser to show
 * its picker.
 *
 * The input is present but visually collapsed rather than absent, because a display:none input
 * cannot be focused and showPicker needs a focusable, connected element.
 */
export function DatePicker({ value, onChange, onDismiss, testID }: DatePickerProps) {
  /**
   * A callback ref rather than an effect: the work has to happen the moment the node exists, and
   * doing it here keeps the open-on-mount behaviour in one place.
   */
  const openOnMount = (node: HTMLInputElement | null) => {
    if (node === null) {
      return;
    }

    node.focus();

    if (typeof node.showPicker !== 'function') {
      /** Older browsers: the focused input still opens on tap, so nothing is lost. */
      return;
    }

    try {
      node.showPicker();
    } catch {
      /** Some browsers refuse outside a user gesture; the focused input remains usable. */
    }
  };

  return createElement('input', {
    'aria-label': 'Chọn ngày',
    'data-testid': testID,
    max: '2100-12-31',
    min: '1900-01-01',
    onBlur: onDismiss,
    onChange: (event: { target: { value: string } }) => {
      if (event.target.value !== '') {
        onChange(event.target.value);
      }
    },
    ref: openOnMount,
    style: {
      border: 0,
      height: 1,
      opacity: 0,
      padding: 0,
      position: 'absolute',
      width: 1,
    },
    type: 'date',
    value,
  });
}
