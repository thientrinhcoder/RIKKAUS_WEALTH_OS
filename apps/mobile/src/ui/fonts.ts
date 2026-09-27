import IBMPlexSans_400Regular from '@expo-google-fonts/ibm-plex-sans/400Regular/IBMPlexSans_400Regular.ttf';
import IBMPlexSans_500Medium from '@expo-google-fonts/ibm-plex-sans/500Medium/IBMPlexSans_500Medium.ttf';
import IBMPlexSans_600SemiBold from '@expo-google-fonts/ibm-plex-sans/600SemiBold/IBMPlexSans_600SemiBold.ttf';
import NotoSerifDisplay_600SemiBold from '@expo-google-fonts/noto-serif-display/600SemiBold/NotoSerifDisplay_600SemiBold.ttf';

/**
 * The bundled typefaces, per docs/design-guidelines.md section 5.2.
 *
 * expo-font registers each file under its own family name, so a weight is selected by naming
 * the family rather than by setting fontWeight. Only the three weights the eight approved
 * typography roles actually use are bundled; adding more would grow the app for nothing.
 *
 * The imports reach each .ttf directly rather than going through the package index. Importing
 * the index pulls every weight and italic of both families into the bundle: 10.5MB of fonts for
 * the four files actually used, which was 83% of the exported web app.
 */

export type SansWeight = '400' | '500' | '600';

export const SANS_FAMILY_BY_WEIGHT = {
  '400': 'IBMPlexSans_400Regular',
  '500': 'IBMPlexSans_500Medium',
  '600': 'IBMPlexSans_600SemiBold',
} as const satisfies Record<SansWeight, string>;

/**
 * Restricted by section 5.2 to the wordmark, display titles at 24pt or larger, printable cover
 * titles and the net-worth hero label. No typography role defaults to it.
 */
export const SERIF_FAMILY = 'NotoSerifDisplay_600SemiBold';

/** Passed to expo-font's loader at app start. */
export const fontAssets = {
  [SANS_FAMILY_BY_WEIGHT['400']]: IBMPlexSans_400Regular,
  [SANS_FAMILY_BY_WEIGHT['500']]: IBMPlexSans_500Medium,
  [SANS_FAMILY_BY_WEIGHT['600']]: IBMPlexSans_600SemiBold,
  [SERIF_FAMILY]: NotoSerifDisplay_600SemiBold,
} as const;

export function sansFamilyFor(weight: SansWeight): string {
  return SANS_FAMILY_BY_WEIGHT[weight];
}
