import { Footer } from "@/components/store/footer";
import { Header } from "@/components/store/header";
import { apiGet } from "@/lib/api/server";
import type { CategoryNode } from "@/lib/api/types";

export const dynamic = "force-dynamic";

export default async function StoreLayout({ children }: { children: React.ReactNode }) {
  // The menu is not worth failing every page for: without the API it is simply empty.
  const categories = await apiGet<CategoryNode[]>("categories").catch(() => [] as CategoryNode[]);
  return (
    <>
      <Header categories={categories} />
      <main className="mx-auto min-h-[60vh] max-w-7xl px-4 py-6">{children}</main>
      <Footer />
    </>
  );
}
