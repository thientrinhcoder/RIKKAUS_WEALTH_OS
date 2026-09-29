/// <reference types="node" />
// This spec reads the source tree, so it needs Node's globals.
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const COMPONENTS = join(__dirname, '..', '..', 'src', 'components');

function sourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const full = join(dir, entry);
    return statSync(full).isDirectory() ? sourceFiles(full) : /\.tsx?$/.test(entry) ? [full] : [];
  });
}

function withoutComments(source: string): string {
  return source.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '');
}

const files = sourceFiles(COMPONENTS).map((full) => ({
  path: relative(COMPONENTS, full).replace(/\\/g, '/'),
  contents: withoutComments(readFileSync(full, 'utf8')),
}));

/**
 * One gate over the whole component kit, consolidating the per-slice boundary scans. The slice
 * scans catch a regression inside their own directory; this one catches a new directory added
 * later that nobody remembered to scan.
 */
describe('no shared component depends on the domain', () => {
  it('scans every kit directory', () => {
    const directories = new Set(files.map(({ path }) => path.split('/')[0]));

    expect([...directories].sort()).toEqual(['action', 'data', 'feedback', 'form', 'overlay', 'shell']);
  });

  it('never imports from the feature layer', () => {
    const offenders = files
      .filter(({ contents }) => /from '@\/features|from '\.\.\/\.\.\/features/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('never imports a catalog fixture', () => {
    const offenders = files
      .filter(({ contents }) => /\(catalog\)|fixture/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('never imports a route or screen', () => {
    const offenders = files
      .filter(({ contents }) => /from '@\/app\//.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });
});
