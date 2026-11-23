const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');
// Last, so Prettier owns layout and ESLint owns correctness: no stylistic rule can disagree with
// the formatter that `npm run format:check` enforces in CI.
const prettierConfig = require('eslint-config-prettier/flat');

module.exports = defineConfig([
  expoConfig,
  prettierConfig,
  {
    ignores: ['dist/**', 'playwright-report/**', 'test-results/**', 'src/api/generated/**'],
  },
]);
