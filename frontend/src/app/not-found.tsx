import Link from "next/link";

/** For URLs outside every route group (the store has its own, richer page). */
export default function NotFound() {
  return (
    <main className="flex min-h-dvh flex-col items-center justify-center gap-4 p-6 text-center">
      <h1 className="text-2xl font-black text-neutral-800">صفحه پیدا نشد</h1>
      <Link href="/" className="text-brand-700">بازگشت به کالامت</Link>
    </main>
  );
}
