// Session cookies. Both are httpOnly, so tokens never reach browser JavaScript.

export const ACCESS_COOKIE = "kl_at";
export const REFRESH_COOKIE = "kl_rt";

export type Tokens = { accessToken: string; expiresInSeconds: number; refreshToken: string };

type CookieOptions = {
  httpOnly: boolean;
  secure: boolean;
  sameSite: "lax";
  path: string;
  maxAge: number;
};

const secure = process.env.NODE_ENV === "production" && process.env.COOKIE_SECURE !== "false";

/** The access cookie expires a little before the token, so an expired token is never sent. */
export function accessCookie(tokens: Tokens): [string, string, CookieOptions] {
  return [ACCESS_COOKIE, tokens.accessToken,
    { httpOnly: true, secure, sameSite: "lax", path: "/", maxAge: Math.max(tokens.expiresInSeconds - 30, 30) }];
}

export function refreshCookie(tokens: Tokens): [string, string, CookieOptions] {
  return [REFRESH_COOKIE, tokens.refreshToken,
    { httpOnly: true, secure, sameSite: "lax", path: "/", maxAge: 30 * 24 * 3600 }];
}
