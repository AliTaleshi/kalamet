import type { Metadata } from "next";
import { Suspense } from "react";
import { LoginFlow } from "@/components/account/login-flow";
import { PageSpinner } from "@/components/ui/spinner";

export const metadata: Metadata = { title: "ورود | ثبت‌نام" };

export default function LoginPage() {
  return (
    <div className="flex justify-center py-6">
      <Suspense fallback={<PageSpinner />}>
        <LoginFlow />
      </Suspense>
    </div>
  );
}
