/// <reference types="node" />
// This spec reads the source tree, so it needs Node's globals. The reference keeps them
// scoped to this file rather than widening the project tsconfig.
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const SRC = join(__dirname, '..', '..', 'src');

/** tokens.ts is the single place a raw value may appear. */
const RAW_VALUE_EXEMPT = ['ui/tokens.ts', 'ui/elevation.ts'];

/** theme.ts is the adapter that reads Paper's own theme; everything else uses useAppTheme. */
const PAPER_THEME_EXEMPT = ['ui/theme.ts'];

function sourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const full = join(dir, entry);

    if (statSync(full).isDirectory()) {
      return sourceFiles(full);
    }

    return /\.tsx?$/.test(entry) ? [full] : [];
  });
}

const files = sourceFiles(SRC).map((full) => ({
  path: relative(SRC, full).replace(/\\/g, '/'),
  contents: readFileSync(full, 'utf8'),
}));

describe('token discipline', () => {
  it('finds source files to scan', () => {
    expect(files.length).toBeGreaterThan(0);
  });

  it('keeps every raw hex colour inside the token and elevation modules', () => {
    const offenders = files
      .filter(({ path }) => !RAW_VALUE_EXEMPT.includes(path))
      .flatMap(({ path, contents }) => {
        // A colour literal is always a quoted string, which keeps issue references
        // such as the #114 in a comment out of the results.
        const matches = contents.match(/['"`]#[0-9a-fA-F]{3,8}\b/g) ?? [];
        return matches.map((match) => `${path}: ${match}`);
      });

    expect(offenders).toEqual([]);
  });

  it('keeps every rgba colour inside the token and elevation modules', () => {
    const offenders = files
      .filter(({ path }) => !RAW_VALUE_EXEMPT.includes(path))
      .flatMap(({ path, contents }) => {
        const matches = contents.match(/['"`]rgba?\(/g) ?? [];
        return matches.map((match) => `${path}: ${match}`);
      });

    expect(offenders).toEqual([]);
  });

  it("reads the theme through useAppTheme so the project roles stay typed", () => {
    const offenders = files
      .filter(({ path }) => !PAPER_THEME_EXEMPT.includes(path))
      .filter(({ contents }) => /\buseTheme\b/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });
});
