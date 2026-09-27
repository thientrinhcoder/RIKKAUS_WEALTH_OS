import { StyleSheet, type StyleProp, type ViewStyle } from 'react-native';
import { Button } from 'react-native-paper';
import type { IconSource } from 'react-native-paper/lib/typescript/components/Icon';

import { sizing } from '@/ui/tokens';
import { useAppTheme } from '@/ui/theme';

/**
 * The shared Button family required by docs/design-guidelines.md section 16.2.
 *
 * Owned by issue #114 rather than #116 so the shell (#115) and the overlay slice (#119) can
 * use it the moment the theme lands. No later slice may fork it.
 */
export const ACTION_BUTTON_VARIANTS = [
  'primary',
  'secondary',
  'tonal',
  'text',
  'destructive',
] as const;

export type ActionButtonVariant = (typeof ACTION_BUTTON_VARIANTS)[number];

/** Paper mode per approved variant. Destructive reuses the filled treatment, recoloured. */
const PAPER_MODE: Record<ActionButtonVariant, 'contained' | 'outlined' | 'contained-tonal' | 'text'> =
  {
    primary: 'contained',
    secondary: 'outlined',
    tonal: 'contained-tonal',
    text: 'text',
    destructive: 'contained',
  };

/**
 * Section 12 requires a destructive action to be distinguishable by more than colour, so it
 * carries a leading warning icon by default. A caller may substitute a more specific icon but
 * cannot remove the distinction.
 */
const DEFAULT_DESTRUCTIVE_ICON: IconSource = 'alert-outline';

/**
 * Section 5.3 gives the primary button a 52 to 56pt height and every touch target a 44pt
 * (iOS/web) or 48dp (Android) minimum. An icon-only button is square, so the same value also
 * becomes its minimum width.
 */
export function actionButtonMinHeight(variant: ActionButtonVariant): number {
  return variant === 'primary' || variant === 'destructive'
    ? sizing.primaryButtonHeight
    : Math.max(sizing.minTouchTarget.android, sizing.minTouchTarget.ios);
}

/**
 * Resolves the leading icon. A destructive action gets one by default so it is
 * distinguishable by more than colour; an ordinary action gets none unless asked.
 */
export function resolveActionIcon(
  variant: ActionButtonVariant,
  icon?: IconSource,
): IconSource | undefined {
  if (icon !== undefined) {
    return icon;
  }

  return variant === 'destructive' ? DEFAULT_DESTRUCTIVE_ICON : undefined;
}

interface ActionButtonBaseProps {
  variant: ActionButtonVariant;
  icon?: IconSource;
  loading?: boolean;
  disabled?: boolean;
  onPress?: () => void;
  style?: StyleProp<ViewStyle>;
  testID?: string;
}

interface LabelledActionButtonProps extends ActionButtonBaseProps {
  children: React.ReactNode;
  accessibilityLabel?: string;
}

/**
 * An icon-only button has no visible text, so section 5.4 requires an explicit accessible
 * label. Omitting it is a typecheck failure.
 */
interface IconOnlyActionButtonProps extends ActionButtonBaseProps {
  children?: undefined;
  icon: IconSource;
  accessibilityLabel: string;
}

export type ActionButtonProps = LabelledActionButtonProps | IconOnlyActionButtonProps;

export function ActionButton({
  variant,
  icon,
  loading = false,
  disabled = false,
  onPress,
  style,
  testID,
  children,
  accessibilityLabel,
}: ActionButtonProps) {
  const theme = useAppTheme();

  const isIconOnly = children === undefined;
  const resolvedIcon = resolveActionIcon(variant, icon);

  /**
   * Loading blocks activation as well as announcing busy, so a slow submit cannot be
   * double-fired. Section 8 requires duplicate-submit prevention.
   */
  const inactive = disabled || loading;

  const minHeight = actionButtonMinHeight(variant);

  /**
   * A leading icon renders a glyph inside the button, and on some platforms that glyph becomes
   * part of the computed accessible name. Naming the button explicitly from its own label keeps
   * the name the user hears identical to the text they read.
   */
  const resolvedLabel =
    accessibilityLabel ?? (typeof children === 'string' ? children : undefined);

  return (
    <Button
      accessibilityLabel={resolvedLabel}
      /**
       * Paper owns the accessibilityState of its own role="button" element, so this lands on the
       * container Paper renders around it. Activation is blocked through `disabled` regardless,
       * which is what prevents a duplicate submit.
       */
      accessibilityState={{ busy: loading, disabled }}
      buttonColor={variant === 'destructive' ? theme.colors.error : undefined}
      contentStyle={[styles.content, { minHeight }, isIconOnly && { minWidth: minHeight }]}
      disabled={inactive}
      icon={resolvedIcon}
      loading={loading}
      mode={PAPER_MODE[variant]}
      onPress={inactive ? undefined : onPress}
      style={style}
      testID={testID}
      textColor={variant === 'destructive' ? theme.colors.onError : undefined}
    >
      {children}
    </Button>
  );
}

const styles = StyleSheet.create({
  content: {
    alignItems: 'center',
    justifyContent: 'center',
  },
});
