import { SearchX } from "lucide-react";
import { ButtonLink } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";

export default function NotFound() {
  return (
    <EmptyState
      icon={SearchX}
      title="صفحه‌ای که دنبال آن بودید پیدا نشد!"
      description="ممکن است این کالا یا صفحه حذف شده باشد یا نشانی را اشتباه وارد کرده باشید."
      action={<ButtonLink href="/">بازگشت به صفحه اصلی</ButtonLink>}
    />
  );
}
