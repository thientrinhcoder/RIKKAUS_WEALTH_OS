import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Surface, TouchableRipple, Text } from 'react-native-paper';

import { elevation } from '@/ui/elevation';
import { radius, sizing, spacing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';
import { FieldShell, fieldAccessibility, type FieldStateProps } from './field-shell';

export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectFieldProps extends FieldStateProps {
  options: readonly SelectOption[];
  /**
   * Empty means nothing is chosen. Section 8 forbids preselecting a financially meaningful
   * answer merely to shorten the form, so this control never defaults to an option.
   */
  value: string;
  onChangeValue?: (value: string) => void;
  onBlur?: () => void;
  /** Shown while nothing is selected. It is a prompt, not a chosen value. */
  emptyLabel?: string;
  testID?: string;
}

export function SelectField({
  options,
  value,
  onChangeValue,
  onBlur,
  emptyLabel = 'Chưa chọn',
  testID,
  ...state
}: SelectFieldProps) {
  const theme = useAppTheme();
  const [open, setOpen] = useState(false);
  const selected = options.find((option) => option.value === value);
  const accessibility = fieldAccessibility(state);

  return (
    <FieldShell {...state} testID={testID}>
      <TouchableRipple
        {...accessibility}
        accessibilityRole="button"
        accessibilityState={{ ...accessibility.accessibilityState, expanded: open }}
        accessibilityValue={{ text: selected?.label ?? emptyLabel }}
        disabled={state.disabled === true || state.readOnly === true}
        onPress={() => setOpen((wasOpen) => !wasOpen)}
        style={[
          styles.anchor,
          {
            backgroundColor: theme.colors.surface,
            borderColor: state.errorText ? theme.colors.error : theme.colors.outline,
          },
        ]}
        testID={testID === undefined ? undefined : `${testID}-anchor`}
      >
        <View style={styles.anchorInner}>
          <Text
            style={{
              color: selected === undefined ? theme.colors.onSurfaceVariant : theme.colors.onSurface,
            }}
            variant="bodyLarge"
          >
            {selected?.label ?? emptyLabel}
          </Text>
        </View>
      </TouchableRipple>

      {/**
        * The option list expands in place rather than in a portal overlay. The overlay slice
        * owns portalled presentations; until it lands, an inline list keeps the control fully
        * operable and verifiable, and the selection contract does not change when it moves.
        */}
      {open ? (
        <Surface
          elevation={elevation.raisedCard.level}
          style={[styles.options, { backgroundColor: theme.colors.surface }]}
          testID={testID === undefined ? undefined : `${testID}-options`}
        >
          {options.map((option) => (
            <TouchableRipple
              accessibilityRole="button"
              accessibilityState={{ selected: option.value === value }}
              key={option.value}
              onPress={() => {
                setOpen(false);
                onChangeValue?.(option.value);
                onBlur?.();
              }}
              style={styles.option}
              testID={testID === undefined ? undefined : `${testID}-option-${option.value}`}
            >
              <Text variant="bodyLarge">{option.label}</Text>
            </TouchableRipple>
          ))}
        </Surface>
      ) : null}
    </FieldShell>
  );
}

const styles = StyleSheet.create({
  anchor: {
    borderRadius: radius.input,
    borderWidth: 1,
    justifyContent: 'center',
    minHeight: sizing.minControlHeight,
    paddingHorizontal: spacing.md,
  },
  anchorInner: {
    justifyContent: 'center',
  },
  options: {
    borderRadius: radius.input,
    overflow: 'hidden',
  },
  option: {
    justifyContent: 'center',
    minHeight: sizing.minControlHeight,
    paddingHorizontal: spacing.md,
  },
});
