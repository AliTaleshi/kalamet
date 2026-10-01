import "server-only";

/**
 * Headers that tell the API who the real client is and which host it used: the client IP feeds
 * the per-IP login-code limit, and the host makes the API build payment URLs on this site.
 * A reverse proxy in front of Next.js must set X-Forwarded-For itself (never pass a client's value).
 */
export function forwardingHeaders(request: Request): Record<string, string> {
  const url = new URL(request.url);
  const host = request.headers.get("x-forwarded-host") ?? request.headers.get("host") ?? url.host;
  const proto = request.headers.get("x-forwarded-proto") ?? url.protocol.replace(":", "");
  const headers: Record<string, string> = { "X-Forwarded-Host": host, "X-Forwarded-Proto": proto };
  const ip = clientIp(request);
  if (ip) headers["X-Forwarded-For"] = ip;
  return headers;
}

/**
 * Joins route segments into an API sub-path. Refuses "." and ".." (fetch would normalise them
 * and let a caller reach API paths outside the one this handler forwards to).
 */
export function apiSubPath(segments: string[]): string | null {
  if (segments.some((s) => s === "." || s === ".." || s === "")) return null;
  return segments.map(encodeURIComponent).join("/");
}

export function clientIp(request: Request): string | null {
  const forwarded = request.headers.get("x-forwarded-for");
  if (forwarded) return forwarded.split(",")[0].trim();
  return request.headers.get("x-real-ip");
}
