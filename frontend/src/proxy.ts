import { NextResponse, type NextRequest } from "next/server";
import { ACCESS_COOKIE, REFRESH_COOKIE, accessCookie, refreshCookie, type Tokens } from "@/lib/auth/cookies";
import { clientIp } from "@/lib/auth/forwarding";
import { refreshSession } from "@/lib/auth/refresh";

/**
 * Runs before page requests: renews an expired access token (so server components always see a
 * valid one) and sends signed-out visitors of private pages to the login page. The pages and the
 * API still check permissions themselves; this is only the fast path.
 */
const PRIVATE_PREFIXES = ["/profile", "/checkout", "/admin"];

export async function proxy(request: NextRequest) {
  let accessToken = request.cookies.get(ACCESS_COOKIE)?.value;
  const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;
  let renewed: Tokens | null | undefined;

  if (!accessToken && refreshToken) {
    try {
      renewed = await refreshSession(refreshToken, clientIp(request));
    } catch {
      renewed = undefined;   // API unreachable: keep the session, try again on the next request
    }
    accessToken = renewed?.accessToken;
  }

  const { pathname, search } = request.nextUrl;
  const isPrivate = PRIVATE_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(prefix + "/"))
    && !pathname.startsWith("/checkout/result");
  let response: NextResponse;
  if (isPrivate && !accessToken && !(refreshToken && renewed === undefined)) {
    const login = new URL("/login", request.url);
    login.searchParams.set("next", pathname + search);
    response = NextResponse.redirect(login);
  } else if (renewed) {
    // Let this request's server components read the new token too.
    request.cookies.set(ACCESS_COOKIE, renewed.accessToken);
    request.cookies.set(REFRESH_COOKIE, renewed.refreshToken);
    response = NextResponse.next({ request: { headers: request.headers } });
  } else {
    response = NextResponse.next();
  }

  if (renewed) {
    response.cookies.set(...accessCookie(renewed));
    response.cookies.set(...refreshCookie(renewed));
  } else if (renewed === null) {
    response.cookies.delete(ACCESS_COOKIE);
    response.cookies.delete(REFRESH_COOKIE);
  }
  return response;
}

export const config = {
  // Pages only: not the BFF and payment handlers (they manage the session themselves) or assets.
  matcher: ["/((?!bff|api|_next/static|_next/image|fonts|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|webp|ico|woff2)$).*)"],
};
