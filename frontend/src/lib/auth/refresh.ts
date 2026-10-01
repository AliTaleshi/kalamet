import "server-only";

import { API_URL } from "@/lib/api/server";
import type { Tokens } from "./cookies";

/**
 * Exchanges a refresh token for a new pair. Parallel requests carrying the same refresh token
 * (a page and its prefetches, two API calls) share one call: the backend rotates refresh tokens,
 * so a second exchange of the same token would fail.
 */
const inFlight = new Map<string, Promise<Tokens | null>>();

export function refreshSession(refreshToken: string, clientIp?: string | null): Promise<Tokens | null> {
  let pending = inFlight.get(refreshToken);
  if (!pending) {
    pending = exchange(refreshToken, clientIp);
    inFlight.set(refreshToken, pending);
    // Keep the result briefly for requests that arrive just after it.
    pending.finally(() => setTimeout(() => inFlight.delete(refreshToken), 10_000));
  }
  return pending;
}

/**
 * Resolves to null when the backend rejects the token (signed out for good) and rejects when the
 * backend could not be reached, so a short outage does not sign anyone out.
 */
async function exchange(refreshToken: string, clientIp?: string | null): Promise<Tokens | null> {
  const response = await fetch(`${API_URL}/api/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(clientIp ? { "X-Forwarded-For": clientIp } : {}),
    },
    body: JSON.stringify({ refreshToken }),
    cache: "no-store",
  });
  if (response.status === 400 || response.status === 401) return null;
  if (!response.ok) throw new Error(`Refresh failed with HTTP ${response.status}`);
  const body = (await response.json()) as Tokens;
  return { accessToken: body.accessToken, expiresInSeconds: body.expiresInSeconds, refreshToken: body.refreshToken };
}
