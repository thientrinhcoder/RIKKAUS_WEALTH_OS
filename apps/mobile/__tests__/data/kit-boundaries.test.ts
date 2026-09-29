/// <reference types="node" />
// This spec reads the source tree, so it needs Node's globals.
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const DATA = join(__dirname, '..', '..', 'src', 'components', 'data');

function sourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const full = join(dir, entry);
    return statSync(full).isDirectory() ? sourceFiles(full) : /\.tsx?$/.test(entry) ? [full] : [];
  });
}

/**
 * Comments are stripped before scanning. A doc comment naming USD as an example of an original
 * currency is documentation, not a rule encoded in the kit, and failing on it would push useful
 * explanation out of the code.
 */
function withoutComments(source: string): string {
  return source.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '');
}

const files = sourceFiles(DATA).map((full) => ({
  path: relative(DATA, full).replace(/\\/g, '/'),
  contents: withoutComments(readFileSync(full, 'utf8')),
}));

describe('the data kit stays domain-neutral', () => {
  it('finds the kit files to scan', () => {
    expect(files.length).toBeGreaterThan(10);
  });

  it('never imports from the feature layer', () => {
    const offenders = files
      .filter(({ contents }) => /from '@\/features|from '\.\.\/\.\.\/features/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('defines no search input of its own, so the shared field stays the only one', () => {
    const offenders = files
      .filter(({ path }) => path !== 'filter-bar.tsx')
      .filter(({ contents }) => /TextInput|SearchField/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('hard-codes no financial category, currency or threshold', () => {
    const offenders = files
      .filter(({ contents }) => /\b(?:VND|USD|Bất động sản|Cổ phiếu|Tiền gửi)\b/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('hard-codes no goal vocabulary, which the goals feature supplies instead', () => {
    const offenders = files
      .filter(({ contents }) => /Đúng kế hoạch|Có rủi ro|Chậm tiến độ/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('reads sizing from the shared tokens rather than hard-coding heights', () => {
    const offenders = files
      .filter(({ contents }) => /minHeight:\s*\d+/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('never falls back to zero for a value it does not have', () => {
    const offenders = files
      .filter(({ contents }) => /\?\?\s*['"]?0['"]?[,;\s)]/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });
});
