import { ChevronLeft } from "lucide-react";
import Link from "next/link";

export function Breadcrumbs({ items }: { items: { label: string; href?: string }[] }) {
  return (
    <nav aria-label="مسیر صفحه" className="flex flex-wrap items-center gap-1 text-xs text-neutral-500">
      <Link href="/" className="hover:text-brand-700">کالامت</Link>
      {items.map((item) => (
        <span key={item.label} className="flex items-center gap-1">
          <ChevronLeft className="size-3" />
          {item.href ? <Link href={item.href} className="hover:text-brand-700">{item.label}</Link> : <span className="text-neutral-700">{item.label}</span>}
        </span>
      ))}
    </nav>
  );
}
