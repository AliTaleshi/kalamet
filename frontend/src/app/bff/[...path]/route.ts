import { NextResponse, type NextRequest } from "next/server";
import { API_URL } from "@/lib/api/server";
import { ACCESS_COOKIE, REFRESH_COOKIE, accessCookie, refreshCookie, type Tokens } from "@/lib/auth/cookies";
import { apiSubPath, clientIp, forwardingHeaders } from "@/lib/auth/forwarding";
import { refreshSession } from "@/lib/auth/refresh";

/**
 * Backend-for-frontend: the browser calls /bff/<path>, this handler calls the API at /api/<path>
 * with the access token from the httpOnly cookie, refreshes the session when the token has
 * expired, and keeps tokens out of every response body.
 */
type Context = { params: Promise<{ path: string[] }> };

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest, context: Context) {
  return handle(request, context);
}
export async function POST(request: NextRequest, context: Context) {
  return handle(request, context);
}
export async function PUT(request: NextRequest, context: Context) {
  return handle(request, context);
}
export async function PATCH(request: NextRequest, context: Context) {
  return handle(request, context);
}
export async function DELETE(request: NextRequest, context: Context) {
  return handle(request, context);
}

async function handle(request: NextRequest, context: Context): Promise<Response> {
  try {
    return await forward(request, context);
  } catch (error) {
    // The API is down or unreachable: answer like the API would, so the UI can show it.
    console.error("BFF could not reach the API", error);
    return problem(502, "API_UNAVAILABLE", "ارتباط با سرور برقرار نشد. لطفاً چند لحظه دیگر تلاش کنید.");
  }
}

async function forward(request: NextRequest, { params }: Context): Promise<Response> {
  const path = apiSubPath((await params).path);
  if (path === null) {
    return problem(404, "REQUEST_REJECTED", "آدرس درخواست‌شده وجود ندارد.");
  }
  if (request.method !== "GET" && !sameOrigin(request)) {
    return problem(403, "FORBIDDEN", "درخواست از مبدأ نامعتبر ارسال شده است.");
  }
  if (path === "auth/refresh") {
    return problem(404, "REQUEST_REJECTED", "آدرس درخواست‌شده وجود ندارد.");
  }
  if (path === "auth/logout") {
    return logout(request);
  }
  if (path === "session" && !request.cookies.has(ACCESS_COOKIE) && !request.cookies.has(REFRESH_COOKIE)) {
    return NextResponse.json({ user: null });   // signed out: no need to ask the API
  }

  const body = request.method === "GET" || request.method === "DELETE" ? undefined : await request.text();
  let accessToken = request.cookies.get(ACCESS_COOKIE)?.value;
  const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;
  let renewed: Tokens | null | undefined;

  if (!accessToken && refreshToken) {
    renewed = await tryRefresh(refreshToken, request);
    accessToken = renewed?.accessToken;
  }
  // "session" is "me" that answers 200 with user null instead of 401, so signed-out visitors
  // do not see an error in the console on every page.
  const apiPath = path === "session" ? "me" : path;
  let response = await callApi(request, apiPath, body, accessToken);
  if (response.status === 401 && refreshToken && renewed === undefined) {
    renewed = await tryRefresh(refreshToken, request);
    if (renewed) {
      response = await callApi(request, apiPath, body, renewed.accessToken);
    }
  }

  let result: NextResponse;
  if (path === "auth/verify" && response.ok) {
    result = await signedIn(response);
  } else if (path === "session") {
    result = response.ok ? NextResponse.json({ user: await response.json() })
      : response.status === 401 ? NextResponse.json({ user: null }) : passThrough(response);
  } else {
    result = passThrough(response);
  }
  if (renewed) {
    result.cookies.set(...accessCookie(renewed));
    result.cookies.set(...refreshCookie(renewed));
  } else if (renewed === null) {
    clearSession(result);
  }
  return result;
}

function callApi(request: NextRequest, path: string, body: string | undefined, accessToken?: string) {
  const headers: Record<string, string> = { Accept: "application/json", ...forwardingHeaders(request) };
  if (body !== undefined) headers["Content-Type"] = request.headers.get("content-type") ?? "application/json";
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  return fetch(`${API_URL}/api/${path}${request.nextUrl.search}`, {
    method: request.method,
    headers,
    body,
    cache: "no-store",
    redirect: "manual",
  });
}

/** Undefined: no refresh attempted or the API was unreachable; null: the session is over. */
async function tryRefresh(refreshToken: string, request: NextRequest): Promise<Tokens | null | undefined> {
  try {
    return await refreshSession(refreshToken, clientIp(request));
  } catch {
    return undefined;
  }
}

/** Moves the tokens of a successful sign-in into cookies; the browser only sees the user. */
async function signedIn(response: Response): Promise<NextResponse> {
  const body = await response.json();
  const tokens: Tokens = {
    accessToken: body.accessToken,
    expiresInSeconds: body.expiresInSeconds,
    refreshToken: body.refreshToken,
  };
  const result = NextResponse.json({ newUser: body.newUser, user: body.user });
  result.cookies.set(...accessCookie(tokens));
  result.cookies.set(...refreshCookie(tokens));
  return result;
}

async function logout(request: NextRequest): Promise<Response> {
  const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;
  if (refreshToken) {
    await fetch(`${API_URL}/api/auth/logout`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...forwardingHeaders(request) },
      body: JSON.stringify({ refreshToken }),
      cache: "no-store",
    }).catch(() => undefined);
  }
  const result = new NextResponse(null, { status: 204 });
  clearSession(result);
  return result;
}

function passThrough(response: Response): NextResponse {
  const headers = new Headers();
  for (const name of ["content-type", "retry-after", "location"]) {
    const value = response.headers.get(name);
    if (value) headers.set(name, value);
  }
  return new NextResponse(response.status === 204 ? null : response.body, { status: response.status, headers });
}

function clearSession(response: NextResponse) {
  response.cookies.delete(ACCESS_COOKIE);
  response.cookies.delete(REFRESH_COOKIE);
}

/** Cookie-authenticated writes must come from this site (defence in depth on top of SameSite=Lax). */
function sameOrigin(request: NextRequest): boolean {
  const origin = request.headers.get("origin");
  if (!origin) return true;   // same-origin fetches from older browsers, server-side calls
  const host = request.headers.get("x-forwarded-host") ?? request.headers.get("host");
  try {
    return new URL(origin).host === host;
  } catch {
    return false;
  }
}

function problem(status: number, code: string, detail: string) {
  return NextResponse.json({ status, code, detail }, { status });
}
