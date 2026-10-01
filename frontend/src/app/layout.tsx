import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import { Providers } from "@/components/providers";
import "./globals.css";

const vazirmatn = localFont({
  src: "./fonts/Vazirmatn-Variable.woff2",
  variable: "--font-vazirmatn",
  weight: "100 900",
  display: "swap",
});

export const metadata: Metadata = {
  title: { default: "کالامت | فروشگاه اینترنتی", template: "%s | کالامت" },
  description: "کالامت؛ خرید آنلاین کالای دیجیتال، مد و پوشاک و لوازم خانه با ارسال سریع و پرداخت امن.",
  applicationName: "کالامت",
};

export const viewport: Viewport = {
  themeColor: "#1d776d",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="fa" dir="rtl" className={vazirmatn.variable}>
      <body className="min-h-dvh font-sans">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
