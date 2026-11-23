#!/usr/bin/env node
/**
 * Generates the client's view of the backend contract from the committed OpenAPI document.
 *
 *   npm run api:generate   rewrites src/api/generated/*
 *   npm run api:check      exits 1 when the committed output differs from the contract
 *
 * Two files are produced, both committed so the app builds without the backend checkout:
 *   - openapi.ts            the paths, operations and schemas as TypeScript types
 *   - contract-examples.ts  every response example in the contract, keyed by operation and status,
 *                           which src/api/fixtures.ts serves as mock responses
 */
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

import openapiTS, { astToString } from 'openapi-typescript';

const appRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const contractPath = resolve(appRoot, '../../services/api/openapi/openapi.json');
const outputDir = resolve(appRoot, 'src/api/generated');
const banner =
  '/**\n * Generated from services/api/openapi/openapi.json by scripts/generate-api-contract.mjs.\n' +
  ' * Do not edit: run `npm run api:generate` after the contract changes.\n */\n';

const HTTP_METHODS = ['get', 'put', 'post', 'delete', 'patch'];

function collectExamples(contract) {
  const examples = {};

  for (const [path, item] of Object.entries(contract.paths ?? {})) {
    for (const method of HTTP_METHODS) {
      const operation = item[method];

      if (!operation) continue;

      for (const [status, response] of Object.entries(operation.responses ?? {})) {
        for (const [contentType, media] of Object.entries(response.content ?? {})) {
          const named = media.examples
            ? Object.fromEntries(
                Object.entries(media.examples).map(([name, example]) => [name, example.value]),
              )
            : media.example === undefined
              ? {}
              : { default: media.example };

          if (Object.keys(named).length === 0) continue;

          const key = `${method.toUpperCase()} ${path}`;
          examples[key] ??= {};
          examples[key][status] = { contentType, examples: named };
        }
      }
    }
  }

  return examples;
}

async function render() {
  const contract = JSON.parse(await readFile(contractPath, 'utf8'));
  const types = banner + astToString(await openapiTS(pathToFileURL(contractPath)));
  const examples = collectExamples(contract);
  const exampleCount = Object.values(examples)
    .flatMap((byStatus) => Object.values(byStatus))
    .reduce((total, entry) => total + Object.keys(entry.examples).length, 0);
  const fixtures =
    banner +
    `\n/** ${exampleCount} examples, copied verbatim from the contract. */\n` +
    `export const contractExamples = ${JSON.stringify(examples, null, 2)} as const;\n`;

  return { 'openapi.ts': types, 'contract-examples.ts': fixtures };
}

const check = process.argv.includes('--check');
const files = await render();
const stale = [];

for (const [name, content] of Object.entries(files)) {
  const target = resolve(outputDir, name);

  if (check) {
    const current = await readFile(target, 'utf8').catch(() => null);

    if (current !== content) stale.push(name);
  } else {
    await mkdir(outputDir, { recursive: true });
    await writeFile(target, content);
  }
}

if (stale.length > 0) {
  console.error(
    `src/api/generated is out of date with the contract: ${stale.join(', ')}.\n` +
      'Run `npm run api:generate` and commit the result.',
  );
  process.exit(1);
}

console.log(check ? 'src/api/generated matches the contract.' : 'src/api/generated written.');
