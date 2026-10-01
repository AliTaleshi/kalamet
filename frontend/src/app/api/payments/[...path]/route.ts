import { NextResponse, type NextRequest } from "next/server";
import { API_URL } from "@/lib/api/server";
import { apiSubPath, forwardingHeaders } from "@/lib/auth/forwarding";

/**
 * Serves the API's public payment endpoints on this site: the gateway callback (which redirects
 * to /checkout/result) and the mock gateway's page. The API builds both URLs from the forwarded
 * host, so customers never see the API's own address.
 */
export const dynamic = "force-dynamic";

export async function GET(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  const path = apiSubPath((await params).path);
  if (path === null) {
    return NextResponse.json({ status: 404, code: "REQUEST_REJECTED", detail: "آدرس درخواست‌شده وجود ندارد." }, { status: 404 });
  }
  let response: Response;
  try {
    response = await fetch(`${API_URL}/api/payments/${path}${request.nextUrl.search}`, {
      headers: forwardingHeaders(request),
      cache: "no-store",
      redirect: "manual",
    });
  } catch {
    return NextResponse.redirect(new URL("/checkout/result?status=failed", request.url));
  }
  const headers = new Headers();
  for (const name of ["content-type", "location"]) {
    const value = response.headers.get(name);
    if (value) headers.set(name, value);
  }
  return new NextResponse(response.body, { status: response.status, headers });
}
