/// <reference types="node" />
// This spec reads the source tree, so it needs Node's globals.
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const FORM = join(__dirname, '..', '..', 'src', 'components', 'form');

function sourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const full = join(dir, entry);
    return statSync(full).isDirectory() ? sourceFiles(full) : /\.tsx?$/.test(entry) ? [full] : [];
  });
}

const files = sourceFiles(FORM).map((full) => ({
  path: relative(FORM, full).replace(/\\/g, '/'),
  contents: readFileSync(full, 'utf8'),
}));

describe('the form kit carries no domain rules', () => {
  it('finds the kit files to scan', () => {
    expect(files.length).toBeGreaterThan(8);
  });

  it('never imports from the feature layer', () => {
    const offenders = files
      .filter(({ contents }) => /from '@\/features|from '\.\.\/\.\.\/features/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('defines no button of its own, so submit and cancel stay on the shared primitive', () => {
    const offenders = files
      .filter(({ path }) => path !== 'form-shell.tsx')
      .filter(({ contents }) => /export function \w*Button/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('reads sizing from the shared tokens rather than hard-coding control heights', () => {
    const offenders = files
      .filter(({ contents }) => /minHeight:\s*\d+/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('encodes no financial threshold, category or currency rule', () => {
    const offenders = files
      .filter(({ path }) => path !== 'formatters.ts')
      .filter(({ contents }) => /\b(?:VND|USD)\b|\btỷ giá\b/i.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });
});
