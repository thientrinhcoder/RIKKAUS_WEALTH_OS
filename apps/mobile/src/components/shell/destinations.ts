/**
 * The five primary destinations from docs/design-guidelines.md section 6.1, in the approved
 * order. Declared once and consumed by all three navigation chromes, so name and order
 * consistency across breakpoints is structural rather than a review checklist item.
 *
 * Profile and settings are deliberately absent: section 6.1 puts them behind the top app bar
 * avatar and forbids a hamburger menu for primary destinations.
 */

export interface Destination {
  key: string;
  /** The full name, used by the rail, the sidebar and the top app bar title. */
  label: string;
  /**
   * The bottom bar gives each of five destinations a fifth of the width, which on a 393pt phone
   * is about 78pt. "Tài sản & Nợ" does not fit on one line there and wrapped onto two, which
   * pushed the bar taller than its neighbours. The short form is what the bottom bar shows; it
   * is never the accessible name, so a screen reader still hears the full label.
   */
  shortLabel: string;
  icon: string;
  route: string;
}

/** Exactly five. A sixth entry is a typecheck failure, not a review comment. */
type FiveDestinations = readonly [Destination, Destination, Destination, Destination, Destination];

export const DESTINATIONS = [
  {
    key: 'overview',
    label: 'Tổng quan',
    shortLabel: 'Tổng quan',
    icon: 'view-dashboard-outline',
    route: '/',
  },
  {
    key: 'holdings',
    label: 'Tài sản & Nợ',
    shortLabel: 'TS & Nợ',
    icon: 'wallet-outline',
    route: '/holdings',
  },
  {
    key: 'cash-flow',
    label: 'Dòng tiền',
    shortLabel: 'Dòng tiền',
    icon: 'swap-vertical',
    route: '/cash-flow',
  },
  {
    key: 'goals',
    label: 'Mục tiêu',
    shortLabel: 'Mục tiêu',
    icon: 'flag-outline',
    route: '/goals',
  },
  {
    key: 'insights',
    label: 'Nhận định',
    shortLabel: 'Nhận định',
    icon: 'lightbulb-outline',
    route: '/insights',
  },
] as const satisfies FiveDestinations;
