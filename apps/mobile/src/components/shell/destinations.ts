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
  label: string;
  icon: string;
  route: string;
}

/** Exactly five. A sixth entry is a typecheck failure, not a review comment. */
type FiveDestinations = readonly [Destination, Destination, Destination, Destination, Destination];

export const DESTINATIONS = [
  { key: 'overview', label: 'Tổng quan', icon: 'view-dashboard-outline', route: '/' },
  { key: 'holdings', label: 'Tài sản & Nợ', icon: 'wallet-outline', route: '/holdings' },
  { key: 'cash-flow', label: 'Dòng tiền', icon: 'swap-vertical', route: '/cash-flow' },
  { key: 'goals', label: 'Mục tiêu', icon: 'flag-outline', route: '/goals' },
  { key: 'insights', label: 'Nhận định', icon: 'lightbulb-outline', route: '/insights' },
] as const satisfies FiveDestinations;
