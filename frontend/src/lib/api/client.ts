"use client";

import { ApiError } from "./error";

type Options = { method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE"; body?: unknown };

/**
 * Calls the API from the browser through the Next.js `/bff` proxy, which adds the access token
 * from an httpOnly cookie and refreshes it when needed. `path` is relative to `/api/`.
 */
export async function api<T>(path: string, { method = "GET", body }: Options = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`/bff/${path}`, {
      method,
      headers: body === undefined ? undefined : { "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
      cache: "no-store",
    });
  } catch {
    throw new ApiError({ status: 0, code: "NETWORK_ERROR", detail: "اتصال اینترنت را بررسی کنید." });
  }
  if (!response.ok) {
    throw await ApiError.from(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

/** Builds a query string, skipping empty values; arrays become repeated parameters. */
export function query(params: Record<string, string | number | boolean | string[] | null | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === null || value === undefined || value === "" || value === false) continue;
    for (const item of Array.isArray(value) ? value : [value]) search.append(key, String(item));
  }
  const text = search.toString();
  return text ? `?${text}` : "";
}
