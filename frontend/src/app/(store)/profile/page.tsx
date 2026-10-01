import type { Metadata } from "next";
import { ProfileForm } from "@/components/account/profile-form";

export const metadata: Metadata = { title: "حساب کاربری" };

export default function ProfilePage() {
  return <ProfileForm />;
}
