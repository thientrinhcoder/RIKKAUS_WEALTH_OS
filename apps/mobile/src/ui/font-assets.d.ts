/**
 * Metro resolves a font file to an asset reference at build time, and this project has no
 * expo-env.d.ts, so TypeScript needs to be told what a .ttf import evaluates to.
 *
 * Native returns a numeric asset id and web returns a URL string; expo-font's loader accepts
 * either, so the union is the honest type rather than picking one platform.
 */
declare module '*.ttf' {
  const asset: number | string;
  export default asset;
}
