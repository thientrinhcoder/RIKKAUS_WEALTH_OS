import { defineConfig, devices } from '@playwright/test';

/**
 * Browser smoke tests against the static web export, not the dev server. CI runs
 * `npm run build:web` first and these specs then prove the artifact that would be deployed
 * actually renders, which a Metro dev bundle cannot prove.
 *
 * Specs are named `*.e2e.ts` rather than `*.spec.ts` so jest-expo's default test match does not
 * pick them up and try to run Playwright inside Jest.
 */
const PORT = 4173;

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: `http://127.0.0.1:${PORT}`,
    locale: 'vi-VN',
    timezoneId: 'Asia/Saigon',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    // Serves the existing export with Expo Router's static routing; it does not rebuild.
    command: `npx expo serve --port ${PORT}`,
    url: `http://127.0.0.1:${PORT}`,
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
  },
});
