import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { EditProduct } from "@/components/admin/product-editor";

export const metadata: Metadata = { title: "ویرایش کالا" };

export default async function EditProductPage({ params }: { params: Promise<{ id: string }> }) {
  const id = Number((await params).id);
  if (!Number.isInteger(id) || id <= 0) notFound();
  return <EditProduct id={id} />;
}
