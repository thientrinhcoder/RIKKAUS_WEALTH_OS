/**
 * The three approved elevation recipes from docs/design-guidelines.md section 5.3.
 * Borders are preferred over shadows for ordinary grouping, so `flat` carries no shadow.
 */

export interface ElevationRecipe {
  /** React Native Paper elevation level, or the platform equivalent. */
  level: 0 | 1 | 3;
  /** CSS shadow for the web target; null where no shadow is approved. */
  webShadow: string | null;
}

export interface ModalElevationRecipe extends ElevationRecipe {
  scrim: string;
}

export const elevation = {
  flat: {
    level: 0,
    webShadow: null,
  },
  raisedCard: {
    level: 1,
    webShadow: '0 2px 8px rgba(24, 43, 69, 0.12)',
  },
  modal: {
    level: 3,
    webShadow: '0 12px 32px rgba(24, 43, 69, 0.22)',
    scrim: 'rgba(23, 33, 43, 0.56)',
  },
} as const satisfies {
  flat: ElevationRecipe;
  raisedCard: ElevationRecipe;
  modal: ModalElevationRecipe;
};
