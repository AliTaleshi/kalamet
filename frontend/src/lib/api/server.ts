import "server-only";

import { cookies } from "next/headers";
import { ApiError } from "./error";
import { ACCESS_COOKIE } from "@/lib/auth/cookies";
import type { Profile } from "./types";

/** Base URL of the Spring Boot API as seen from the Next.js server. */
export const API_URL = (process.env.API_URL ?? "http://localhost:8080").replace(/\/$/, "");

/** GET from the API during server rendering. Throws ApiError; 404s are for the caller to map to notFound(). */
export async function apiGet<T>(path: string, { auth = false }: { auth?: boolean } = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (auth) {
    const token = (await cookies()).get(ACCESS_COOKIE)?.value;
    if (token) headers.Authorization = `Bearer ${token}`;
  }
  const response = await fetch(`${API_URL}/api/${path}`, { headers, cache: "no-store" });
  if (!response.ok) {
    throw await ApiError.from(response);
  }
  return (await response.json()) as T;
}

/** Like apiGet, but returns null for 404 instead of throwing. */
export async function apiGetOrNull<T>(path: string): Promise<T | null> {
  try {
    return await apiGet<T>(path);
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

/** The signed-in user, or null (no cookie, expired or revoked session). */
export async function currentUser(): Promise<Profile | null> {
  if (!(await cookies()).get(ACCESS_COOKIE)) return null;
  try {
    return await apiGet<Profile>("me", { auth: true });
  } catch {
    return null;
  }
}
