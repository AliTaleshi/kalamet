import type { Metadata } from "next";
import { NewProduct } from "@/components/admin/product-editor";

export const metadata: Metadata = { title: "کالای جدید" };

export default function NewProductPage() {
  return <NewProduct />;
}
