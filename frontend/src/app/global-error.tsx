"use client";

/** Last-resort error page: replaces the root layout, so it brings its own <html>. */
export default function GlobalError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <html lang="fa" dir="rtl">
      <body style={{ fontFamily: "Tahoma, sans-serif", display: "grid", placeItems: "center", minHeight: "100vh", margin: 0 }}>
        <div style={{ textAlign: "center" }}>
          <h1>خطای غیرمنتظره‌ای رخ داد</h1>
          <button type="button" onClick={reset}>تلاش دوباره</button>
        </div>
      </body>
    </html>
  );
}
