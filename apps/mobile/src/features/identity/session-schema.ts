import { z } from 'zod';

/**
 * The `SessionResponse` every sign-in and renewal returns. `refreshToken` is present for a native
 * client and omitted for the web client, whose refresh token travels in the HttpOnly cookie.
 */
export const SessionUserSchema = z.object({
  id: z.string(),
  email: z.string(),
  displayName: z.string(),
});

export type SessionUser = z.infer<typeof SessionUserSchema>;

export const SessionResponseSchema = z.object({
  accessToken: z.string().min(1),
  tokenType: z.string(),
  expiresInSeconds: z.number(),
  refreshToken: z.string().min(1).optional(),
  user: SessionUserSchema,
});

export type SessionResponse = z.infer<typeof SessionResponseSchema>;
