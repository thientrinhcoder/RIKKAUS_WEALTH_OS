import { z } from 'zod';

import type { ContractSchemas, KnownProblemType } from './contract-types';

const PROBLEM_TYPE_PREFIX = 'urn:rikkaus:problem:';

/**
 * Every `type` URN the contract publishes. `satisfies Record<KnownProblemType, true>` makes a
 * URN added to or removed from the contract a compile error here until it is handled.
 */
const KNOWN_PROBLEM_TYPE_SET = {
  'urn:rikkaus:problem:validation-failed': true,
  'urn:rikkaus:problem:malformed-request': true,
  'urn:rikkaus:problem:not-found': true,
  'urn:rikkaus:problem:method-not-allowed': true,
  'urn:rikkaus:problem:not-acceptable': true,
  'urn:rikkaus:problem:unsupported-media-type': true,
  'urn:rikkaus:problem:linked-records': true,
  'urn:rikkaus:problem:unauthorized': true,
  'urn:rikkaus:problem:forbidden': true,
  'urn:rikkaus:problem:internal-error': true,
} as const satisfies Record<KnownProblemType, true>;

export const KNOWN_PROBLEM_TYPES = Object.keys(KNOWN_PROBLEM_TYPE_SET) as KnownProblemType[];

export function isKnownProblemType(type: string): type is KnownProblemType {
  return Object.hasOwn(KNOWN_PROBLEM_TYPE_SET, type);
}

/**
 * RFC 9457 problem body. `type` is accepted as any string rather than the contract's enum so a
 * URN added by a newer server degrades to a status-based message instead of failing to parse.
 */
export const ProblemDetailSchema = z.object({
  type: z.string(),
  title: z.string(),
  status: z.number().int(),
  detail: z.string().optional(),
  instance: z.string().optional(),
  correlationId: z.string().optional(),
  errors: z
    .array(z.object({ field: z.string().optional(), message: z.string().optional() }))
    .optional(),
});

export type ProblemDetail = z.infer<typeof ProblemDetailSchema>;

/** The contract's own shape must stay assignable to what the client parses. */
export type ContractProblemDetailIsParseable = ContractSchemas['ProblemDetail'] extends Omit<
  ProblemDetail,
  'type'
> & { type: string }
  ? true
  : never;
const contractProblemDetailIsParseable: ContractProblemDetailIsParseable = true;
void contractProblemDetailIsParseable;

export function isProblemContentType(contentType: string | null): boolean {
  return Boolean(contentType && /application\/(problem\+)?json/i.test(contentType));
}

export function problemCode(type: string): string {
  return type.startsWith(PROBLEM_TYPE_PREFIX) ? type.slice(PROBLEM_TYPE_PREFIX.length) : type;
}
