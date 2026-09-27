/// <reference types="node" />
// This spec reads the source tree, so it needs Node's globals.
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const OVERLAY = join(__dirname, '..', '..', 'src', 'components', 'overlay');

function sourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const full = join(dir, entry);
    return statSync(full).isDirectory() ? sourceFiles(full) : /\.tsx?$/.test(entry) ? [full] : [];
  });
}

/** Comments are documentation, not behaviour, so they are stripped before scanning. */
function withoutComments(source: string): string {
  return source.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '');
}

const files = sourceFiles(OVERLAY).map((full) => ({
  path: relative(OVERLAY, full).replace(/\\/g, '/'),
  contents: withoutComments(readFileSync(full, 'utf8')),
}));

describe('the overlay kit names no consequence of its own', () => {
  it('finds the kit files to scan', () => {
    expect(files.length).toBeGreaterThan(6);
  });

  it('never imports from the feature layer', () => {
    const offenders = files
      .filter(({ contents }) => /from '@\/features|from '\.\.\/\.\.\/features/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('hard-codes no record type, so the consequence always comes from the caller', () => {
    const offenders = files
      .filter(({ contents }) =>
        /tài sản|công nợ|thu nhập|chi phí|mục tiêu|định giá/i.test(contents),
      )
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('reimplements focus handling nowhere but the shell', () => {
    const offenders = files
      .filter(({ path }) => path !== 'overlay-shell.tsx')
      .filter(({ contents }) => /activeElement|setAccessibilityFocus|findNodeHandle/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('defines no button of its own, so the destructive variant stays the shared one', () => {
    const offenders = files
      .filter(({ contents }) => /export function \w*Button/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('retains no deleted state, per the Product Owner decision that undo is optional', () => {
    const offenders = files
      .filter(({ contents }) => /snapshot|restoreState|deletedRecord|pendingDelete/i.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('reads radius, elevation and the scrim from the shared tokens', () => {
    const offenders = files
      .filter(({ contents }) => /borderRadius:\s*\d+|rgba\(/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });
});
