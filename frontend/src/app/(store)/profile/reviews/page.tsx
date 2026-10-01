import type { Metadata } from "next";
import { MyReviews } from "@/components/account/my-reviews";

export const metadata: Metadata = { title: "دیدگاه‌ها" };

export default function MyReviewsPage() {
  return <MyReviews />;
}
