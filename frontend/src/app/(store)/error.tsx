"use client";

import { ServerCrash } from "lucide-react";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";

export default function StoreError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <EmptyState
      icon={ServerCrash}
      title="مشکلی پیش آمد"
      description="ارتباط با فروشگاه برقرار نشد. چند لحظه دیگر دوباره تلاش کنید."
      action={<Button onClick={reset}>تلاش دوباره</Button>}
    />
  );
}
