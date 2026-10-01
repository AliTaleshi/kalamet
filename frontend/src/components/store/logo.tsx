import Link from "next/link";

/** Kalamet's wordmark: a teal tile with a stylised "ک" and an amber spark. */
export function Logo({ className }: { className?: string }) {
  return (
    <Link href="/" aria-label="کالامت، صفحه اصلی" className={`flex items-center gap-2 ${className ?? ""}`}>
      <span className="relative flex size-9 items-center justify-center rounded-xl bg-brand-600 text-xl font-black text-white">
        ک
        <span className="absolute -top-1 -left-1 size-3 rounded-full border-2 border-white bg-accent-400" />
      </span>
      <span className="text-2xl font-black tracking-tight text-brand-700">کالامت</span>
    </Link>
  );
}
