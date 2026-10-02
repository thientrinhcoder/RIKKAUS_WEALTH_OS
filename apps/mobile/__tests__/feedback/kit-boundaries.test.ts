/// <reference types="node" />
// This spec reads the source tree, so it needs Node's globals.
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const FEEDBACK = join(__dirname, '..', '..', 'src', 'components', 'feedback');

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

const files = sourceFiles(FEEDBACK).map((full) => ({
  path: relative(FEEDBACK, full).replace(/\\/g, '/'),
  contents: withoutComments(readFileSync(full, 'utf8')),
}));

describe('the feedback kit stays generic', () => {
  it('finds the kit files to scan', () => {
    expect(files.length).toBeGreaterThan(8);
  });

  it('never imports from the feature layer', () => {
    const offenders = files
      .filter(({ contents }) => /from '@\/features|from '\.\.\/\.\.\/features/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('hard-codes no goal vocabulary, which the goals feature supplies instead', () => {
    const offenders = files
      .filter(({ contents }) =>
        /Đúng kế hoạch|Đúng tiến độ|Có rủi ro|Chậm tiến độ|on-track|at-risk|behind/i.test(
          contents,
        ),
      )
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('renders no currency amount of its own, so no fake business data reaches a screen', () => {
    const offenders = files
      .filter(({ contents }) => /₫|\bVND\b|\bUSD\b|\d{1,3}(?:\.\d{3}){2,}/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('reads spacing and radius from the shared tokens rather than hard-coding them', () => {
    const offenders = files
      .filter(({ contents }) => /(?:padding|gap|borderRadius):\s*\d+/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });

  it('depends on no animation to convey a state, so reduced motion loses nothing', () => {
    const offenders = files
      .filter(({ contents }) => /Animated|useNativeDriver|LayoutAnimation/.test(contents))
      .map(({ path }) => path);

    expect(offenders).toEqual([]);
  });
});
