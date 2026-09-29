/// <reference types="node" />
// This spec reads the source tree, so it needs Node's globals.
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const CATALOG = join(__dirname, '..', '..', 'src', 'app', 'catalog');

function sourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const full = join(dir, entry);
    return statSync(full).isDirectory() ? sourceFiles(full) : /\.tsx?$/.test(entry) ? [full] : [];
  });
}

function withoutComments(source: string): string {
  return source.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '');
}

const files = sourceFiles(CATALOG).map((full) => ({
  path: relative(CATALOG, full).replace(/\\/g, '/'),
  contents: withoutComments(readFileSync(full, 'utf8')),
}));

/**
 * This is the spec that makes the composability claim falsifiable. If an example needed an input,
 * a dialog or a styled control the kit does not provide, it would have to build one here, and
 * these assertions are what would catch it.
 */
describe('the catalog forks nothing', () => {
  it('finds the catalog screens to scan', () => {
    expect(files.length).toBeGreaterThanOrEqual(5);
  });

  it('defines no input of its own', () => {
    const offenders = files
      .filter(({ contents }) => /\bTextInput\b|\bSwitch\b|\bCheckbox\b/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('defines no pressable or button of its own', () => {
    const offenders = files
      .filter(({ contents }) => /\bPressable\b|\bTouchableRipple\b|\bTouchableOpacity\b/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('defines no dialog, modal or portal of its own', () => {
    const offenders = files
      .filter(({ contents }) => /\bModal\b|\bPortal\b|\bDialog\b(?!Props)/.test(contents))
      .filter(({ contents }) => !/from '@\/components\/overlay'/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('declares no colour, radius or elevation of its own', () => {
    const offenders = files
      .filter(({ contents }) =>
        /['"`]#[0-9a-fA-F]{3,8}\b|['"`]rgba?\(|borderRadius:|elevation:/.test(contents),
      )
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('takes every visual element from the shared components', () => {
    const screens = files.filter(({ path }) => path !== '_layout.tsx');

    for (const screen of screens) {
      expect({
        path: screen.path,
        importsComponents: /from '@\/components\//.test(screen.contents),
      }).toEqual({ path: screen.path, importsComponents: true });
    }
  });
});
