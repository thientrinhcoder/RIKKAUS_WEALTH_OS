import { useState } from 'react';

export function ciProbe(flag: boolean) {
  if (flag) {
    useState(0);
  }
}
