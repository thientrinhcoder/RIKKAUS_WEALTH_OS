import type { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { Divider } from 'react-native-paper';

import { EmptyCard } from './cards/empty-card';

export interface ListViewProps<Item> {
  items: readonly Item[];
  keyExtractor: (item: Item) => string;
  renderItem: (item: Item) => ReactNode;
  /** Section 11 requires an empty list to offer the relevant next action, not a bare message. */
  emptyTitle: string;
  emptyDescription?: string;
  emptyActionLabel: string;
  onEmptyAction: () => void;
  testID?: string;
}

/**
 * A list with the divider and empty-state rules applied once.
 *
 * A divider separates rows, so it belongs between them and never after the last one, where it
 * would read as a missing row.
 */
export function ListView<Item>({
  items,
  keyExtractor,
  renderItem,
  emptyTitle,
  emptyDescription,
  emptyActionLabel,
  onEmptyAction,
  testID = 'list',
}: ListViewProps<Item>) {
  if (items.length === 0) {
    return (
      <EmptyCard
        actionLabel={emptyActionLabel}
        description={emptyDescription}
        onAction={onEmptyAction}
        testID={`${testID}-empty`}
        title={emptyTitle}
      />
    );
  }

  return (
    <View style={styles.list} testID={testID}>
      {items.map((item, index) => {
        const key = keyExtractor(item);

        return (
          <View key={key}>
            {renderItem(item)}
            {index < items.length - 1 ? <Divider testID={`${testID}-divider-${key}`} /> : null}
          </View>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  list: {
    width: '100%',
  },
});
