import { ScreenHeading } from '@/components/shell/screen-heading';

/**
 * Destination placeholder. Domain content arrives with its own MVP slice; this phase only
 * proves the shell routes to it and moves focus to its heading.
 */
export default function OverviewScreen() {
  return <ScreenHeading>Tổng quan</ScreenHeading>;
}
