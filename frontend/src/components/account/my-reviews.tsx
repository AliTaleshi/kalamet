"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { MessageSquare, Trash2 } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/error";
import type { MyReview, Page } from "@/lib/api/types";
import { jalaliDate } from "@/lib/format";
import { Button } from "@/components/ui/button";
import { Card, CardHeader } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { Pagination } from "@/components/ui/pagination";
import { Stars } from "@/components/ui/rating";
import { PageSpinner } from "@/components/ui/spinner";
import { ReviewStatusBadge } from "@/components/ui/status-badges";
import { useToast } from "@/components/ui/toast";

export function MyReviews() {
  const client = useQueryClient();
  const notify = useToast();
  const [page, setPage] = useState(0);
  const reviews = useQuery({
    queryKey: ["my-reviews", page],
    queryFn: () => api<Page<MyReview>>(`me/reviews?page=${page}&size=10`),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: (id: number) => api<void>(`me/reviews/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      notify("دیدگاه حذف شد");
      client.invalidateQueries({ queryKey: ["my-reviews"] });
    },
    onError: (e) => notify(errorMessage(e), "error"),
  });

  return (
    <Card>
      <CardHeader title="دیدگاه‌های من" />
      {reviews.isLoading ? (
        <PageSpinner />
      ) : !reviews.data?.items.length ? (
        <EmptyState icon={MessageSquare} title="هنوز دیدگاهی ننوشته‌اید" description="پس از خرید، تجربه خود را با دیگران به اشتراک بگذارید." />
      ) : (
        <>
          <ul className="divide-y divide-neutral-100">
            {reviews.data.items.map((review) => (
              <li key={review.id} className="flex flex-col gap-2 p-5 text-sm">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <Link href={`/product/${review.product.slug}`} className="font-bold text-neutral-800 hover:text-brand-700">{review.product.name}</Link>
                  <ReviewStatusBadge status={review.status} />
                </div>
                <div className="flex items-center gap-2 text-xs text-neutral-500">
                  <Stars value={review.rating} />
                  {jalaliDate(review.createdAt)}
                </div>
                {review.title && <p className="font-medium text-neutral-800">{review.title}</p>}
                {review.comment && <p className="whitespace-pre-line leading-7 text-neutral-600">{review.comment}</p>}
                <Button size="sm" variant="ghost" className="w-fit text-danger-600" loading={remove.isPending && remove.variables === review.id}
                  onClick={() => window.confirm("این دیدگاه حذف شود؟") && remove.mutate(review.id)}>
                  <Trash2 className="size-4" />حذف
                </Button>
              </li>
            ))}
          </ul>
          <Pagination page={page} totalPages={reviews.data.totalPages} onChange={setPage} />
        </>
      )}
    </Card>
  );
}
