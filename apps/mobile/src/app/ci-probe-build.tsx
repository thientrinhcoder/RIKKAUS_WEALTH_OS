// eslint-disable-next-line @typescript-eslint/no-require-imports
const missing = require('./module-that-does-not-exist');

export default function CiProbeBuild() {
  return missing;
}
