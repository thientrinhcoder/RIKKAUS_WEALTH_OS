/// <reference types="node" />
// This spec reads the source tree, so it needs Node's globals.
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

import { DESTINATIONS } from '@/components/shell/destinations';
import { CREATE_RECORD_TYPES } from '@/components/shell/create-action';

/**
 * The catalog exists for the team, not for the user. It must never become reachable from
 * production navigation, which is easy to do by accident when adding a route.
 */
describe('the catalog stays out of the product', () => {
  it('appears in none of the five destinations', () => {
    for (const destination of DESTINATIONS) {
      expect(destination.route).not.toContain('catalog');
      expect(destination.key).not.toContain('catalog');
      expect(destination.label.toLowerCase()).not.toContain('catalog');
    }
  });

  it('appears in none of the create-action routes', () => {
    for (const recordType of CREATE_RECORD_TYPES) {
      expect(recordType.route).not.toContain('catalog');
    }
  });

  it('is not mounted by the production group layout', () => {
    const layout = readFileSync(
      join(__dirname, '..', '..', 'src', 'app', '(app)', '_layout.tsx'),
      'utf8',
    );

    expect(layout).not.toContain('catalog');
  });

  it('lives under its own /catalog path rather than at the product root', () => {
    /**
     * A route group is transparent in the URL, so a grouped catalog would publish /forms and
     * /data at the product root, ready to collide with a real screen later. A plain directory
     * namespaces them under /catalog instead.
     */
    const screens = readdirSync(join(__dirname, '..', '..', 'src', 'app', 'catalog'));

    expect(screens.length).toBeGreaterThan(0);
    expect(existsSync(join(__dirname, '..', '..', 'src', 'app', '(catalog)'))).toBe(false);
  });

  it('keeps its own layout outside the shell, so it cannot inherit production navigation', () => {
    const layout = readFileSync(
      join(__dirname, '..', '..', 'src', 'app', 'catalog', '_layout.tsx'),
      'utf8',
    );

    expect(layout).not.toContain('AppShell');
  });
});
