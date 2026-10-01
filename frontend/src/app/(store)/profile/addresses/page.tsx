import type { Metadata } from "next";
import { AddressBook } from "@/components/account/address-book";

export const metadata: Metadata = { title: "نشانی‌ها" };

export default function AddressesPage() {
  return <AddressBook />;
}
