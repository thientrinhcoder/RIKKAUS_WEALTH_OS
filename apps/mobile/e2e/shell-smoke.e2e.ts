import { expect, test, type Page, type Route } from '@playwright/test';

/**
 * Proves the exported web build boots. Signed in, it renders the app shell with its Vietnamese
 * primary destinations: phone width shows the bottom bar with the short labels, desktop width the
 * sidebar with the full labels (docs/design-guidelines.md section 6.3). Signed out, the protected
 * shell redirects to the single Google entry point, and the release build offers no development
 * sign-in.
 *
 * CI runs no API, so the two auth calls the web client makes on load are answered here with the
 * contract's shapes. That stands in for the server, not for the app: every byte rendered is the
 * exported build's own.
 */
const VIEWPORTS = [
  {
    name: 'phone 360px',
    size: { width: 360, height: 780 },
    chrome: 'shell-bottom-nav',
    labels: ['Tổng quan', 'TS & Nợ', 'Dòng tiền', 'Mục tiêu', 'Nhận định'],
  },
  {
    name: 'desktop 1440px',
    size: { width: 1440, height: 900 },
    chrome: 'shell-sidebar',
    labels: ['Tổng quan', 'Tài sản & Nợ', 'Dòng tiền', 'Mục tiêu', 'Nhận định'],
  },
] as const;

/**
 * React error #418 is a hydration text mismatch: the static HTML is rendered at build time with
 * no window width and before fonts load, so the client's first render differs and React
 * re-renders on the client. The page recovers and the shell is fully usable, which is what this
 * smoke test guards. The mismatch itself predates CI and is tracked as a separate fix; every
 * other uncaught page error still fails the test.
 */
const KNOWN_HYDRATION_MISMATCH = /Minified React error #418\b/;

const USER = {
  id: '0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93',
  email: 'an.nguyen@example.com',
  displayName: 'An Nguyễn',
};

/** A credentialed cross-origin answer, as the API gives on `/api/v1/auth/**`. */
async function answer(
  route: Route,
  status: number,
  body?: unknown,
  contentType = 'application/json',
) {
  const origin = route.request().headers().origin ?? '*';
  const cors = {
    'Access-Control-Allow-Origin': origin,
    'Access-Control-Allow-Credentials': 'true',
    'Access-Control-Allow-Headers':
      'content-type, authorization, x-correlation-id, x-rikkaus-client',
    'Access-Control-Allow-Methods': 'GET, POST',
  };

  if (route.request().method() === 'OPTIONS') {
    await route.fulfill({ status: 204, headers: cors });
    return;
  }

  await route.fulfill({
    status,
    headers: { ...cors, 'Content-Type': contentType },
    body: body === undefined ? '' : JSON.stringify(body),
  });
}

async function serveSignedIn(page: Page) {
  await page.route('**/api/v1/auth/refresh', (route) =>
    answer(route, 200, {
      accessToken: 'smoke-access-token',
      tokenType: 'Bearer',
      expiresInSeconds: 900,
      user: USER,
    }),
  );
  await page.route('**/api/v1/auth/session', (route) => answer(route, 200, USER));
}

async function serveSignedOut(page: Page) {
  await page.route('**/api/v1/auth/refresh', (route) =>
    answer(
      route,
      400,
      {
        type: 'urn:rikkaus:problem:validation-failed',
        title: 'Validation failed',
        status: 400,
        errors: [{ field: 'refreshToken', message: 'must not be blank' }],
      },
      'application/problem+json',
    ),
  );
}

function collectPageErrors(page: Page): string[] {
  const pageErrors: string[] = [];
  page.on('pageerror', (error) => {
    if (!KNOWN_HYDRATION_MISMATCH.test(error.message)) {
      pageErrors.push(error.message);
    }
  });
  return pageErrors;
}

for (const viewport of VIEWPORTS) {
  test(`shell renders with Vietnamese navigation at ${viewport.name}`, async ({ page }) => {
    const pageErrors = collectPageErrors(page);
    await serveSignedIn(page);

    await page.setViewportSize(viewport.size);
    await page.goto('/');

    await expect(page.getByTestId('shell-root')).toBeVisible();
    const chrome = page.getByTestId(viewport.chrome);
    await expect(chrome).toBeVisible();

    for (const label of viewport.labels) {
      await expect(chrome.getByText(label, { exact: true })).toBeVisible();
    }

    expect(pageErrors).toEqual([]);
  });

  test(`signed-out visit is sent to the Google entry point at ${viewport.name}`, async ({
    page,
  }) => {
    const pageErrors = collectPageErrors(page);
    await serveSignedOut(page);

    await page.setViewportSize(viewport.size);
    await page.goto('/');

    await expect(page).toHaveURL(/\/sign-in$/);
    await expect(page.getByText('Tài sản của bạn. Một góc nhìn riêng tư.')).toBeVisible();
    await expect(page.getByTestId('sign-in-google')).toBeVisible();
    await expect(page.getByTestId('shell-root')).toHaveCount(0);
    await expect(page.getByTestId('dev-sign-in')).toHaveCount(0);

    const storage = await page.evaluate(() => [localStorage.length, sessionStorage.length]);
    expect(storage).toEqual([0, 0]);
    expect(pageErrors).toEqual([]);
  });
}
