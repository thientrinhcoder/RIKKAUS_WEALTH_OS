import { useRef, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { Portal, Surface, Text, TouchableRipple } from 'react-native-paper';

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

interface AnchorRect {
  x: number;
  y: number;
  width: number;
  height: number;
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
  const [anchor, setAnchor] = useState<AnchorRect | null>(null);
  const anchorRef = useRef<View>(null);
  const selected = options.find((option) => option.value === value);
  const accessibility = fieldAccessibility(state);

  /**
   * The list floats over whatever is below it rather than expanding in place, so opening it does
   * not push the rest of the form down and move the control out from under the user's finger.
   * Floating needs the anchor's position in window coordinates, which is only knowable at the
   * moment of opening.
   */
  const openList = () => {
    anchorRef.current?.measureInWindow((x, y, width, height) => {
      setAnchor({ x, y, width, height });
    });

    setOpen(true);
  };

  const close = () => {
    setOpen(false);
    onBlur?.();
  };

  const disabled = state.disabled === true || state.readOnly === true;

  return (
    <FieldShell {...state} testID={testID}>
      <View ref={anchorRef} collapsable={false}>
        <TouchableRipple
          {...accessibility}
          accessibilityRole="button"
          accessibilityState={{ ...accessibility.accessibilityState, expanded: open }}
          accessibilityValue={{ text: selected?.label ?? emptyLabel }}
          disabled={disabled}
          onPress={openList}
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
                color:
                  selected === undefined ? theme.colors.onSurfaceVariant : theme.colors.onSurface,
              }}
              variant="bodyLarge"
            >
              {selected?.label ?? emptyLabel}
            </Text>
          </View>
        </TouchableRipple>
      </View>

      {open ? (
        <Portal>
          {/**
            * A transparent full-screen layer so a tap anywhere outside closes the list. Choosing
            * nothing costs nothing here, so every dismissal route is allowed.
            */}
          <Pressable
            accessibilityElementsHidden
            aria-hidden
            importantForAccessibility="no-hide-descendants"
            onPress={close}
            style={StyleSheet.absoluteFill}
            testID={testID === undefined ? undefined : `${testID}-backdrop`}
          />

          <Surface
            elevation={elevation.modal.level}
            style={[
              styles.options,
              {
                backgroundColor: theme.colors.surface,
                left: anchor?.x ?? 0,
                top: (anchor?.y ?? 0) + (anchor?.height ?? 0) + spacing.xs,
                width: anchor?.width,
              },
            ]}
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
        </Portal>
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
    position: 'absolute',
  },
  option: {
    justifyContent: 'center',
    minHeight: sizing.minControlHeight,
    paddingHorizontal: spacing.md,
  },
});
