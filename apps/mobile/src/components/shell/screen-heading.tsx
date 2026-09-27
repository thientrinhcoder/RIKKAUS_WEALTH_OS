import { useEffect, useRef } from 'react';
import { AccessibilityInfo, findNodeHandle, Platform, View } from 'react-native';
import { Text } from 'react-native-paper';

/**
 * Section 12 requires a route change to move focus to the new screen's main heading, so a
 * screen reader user learns where they landed instead of being left at the top of the chrome.
 * Every destination renders exactly one of these.
 */

interface FocusableNode {
  focus?: () => void;
}

/**
 * Web and native move focus by different mechanisms: react-native-web renders a DOM node with
 * a focus method, while native needs a node handle passed to the accessibility service.
 * Returns whether focus was actually moved, so a caller is never told it succeeded when the
 * platform gave it nothing to focus.
 */
export function focusHeading(node: FocusableNode | null): boolean {
  if (node === null) {
    return false;
  }

  if (typeof node.focus === 'function') {
    node.focus();
    return true;
  }

  try {
    const handle = findNodeHandle(node as never);

    if (handle === null) {
      return false;
    }

    AccessibilityInfo.setAccessibilityFocus(handle);
    return true;
  } catch {
    return false;
  }
}

export function ScreenHeading({ children }: { children: string }) {
  const ref = useRef<View>(null);

  useEffect(() => {
    focusHeading(ref.current);
  }, [children]);

  return (
    <View
      accessible
      accessibilityRole="header"
      focusable={Platform.OS === 'web'}
      ref={ref}
      testID="screen-heading"
    >
      <Text variant="headlineSmall">{children}</Text>
    </View>
  );
}
