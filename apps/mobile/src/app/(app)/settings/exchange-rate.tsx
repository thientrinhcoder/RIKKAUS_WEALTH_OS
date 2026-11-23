import { ScreenHeading } from '@/components/shell/screen-heading';

/**
 * Destination placeholder for the first-login next step. The exchange-rate settings form arrives
 * with its own MVP slice; this route exists so the first-login panel's action lands somewhere real.
 */
export default function ExchangeRateSettingsScreen() {
  return <ScreenHeading>Thiết lập tỷ giá USD/VND</ScreenHeading>;
}
