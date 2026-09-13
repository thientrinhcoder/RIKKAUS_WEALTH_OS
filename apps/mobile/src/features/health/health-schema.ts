import { z } from 'zod';

export const HealthResponseSchema = z
  .object({
    status: z.literal('UP'),
  })
  .passthrough();

export type HealthResponse = z.infer<typeof HealthResponseSchema>;
