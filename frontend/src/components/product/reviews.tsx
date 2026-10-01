"use client";

import { useInfiniteQuery, useMutation } from "@tanstack/react-query";
import { BadgeCheck, MessageSquarePlus, ThumbsDown, ThumbsUp } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { api } from "@/lib/api/client";
import { ApiError } from "@/lib/api/error";
import type { MyReview, Page, RatingSummary, Review } from "@/lib/api/types";
import { faNumber, jalaliDate } from "@/lib/format";
import { useMe } from "@/lib/hooks/session";
import { Button } from "@/components/ui/button";
import { FormError, TextArea, TextField } from "@/components/ui/field";
import { Stars } from "@/components/ui/rating";

export function Reviews({ slug, summary, firstPage }: { slug: string; summary: RatingSummary; firstPage: Page<Review> }) {
  const reviews = useInfiniteQuery({
    queryKey: ["reviews", slug],
    queryFn: ({ pageParam }) => api<Page<Review>>(`products/${slug}/reviews?page=${pageParam}&size=10`),
    initialPageParam: 0,
    initialData: { pages: [firstPage], pageParams: [0] },
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
    staleTime: 60_000,
  });
  const items = reviews.data?.pages.flatMap((p) => p.items) ?? [];

  return (
    <div className="grid gap-8 lg:grid-cols-[18rem_1fr]">
      <aside className="flex flex-col gap-4">
        {summary.count > 0 ? (
          <>
            <div className="flex items-end gap-2">
              <span className="text-4xl font-black text-neutral-800">{faNumber(summary.average ?? 0)}</span>
              <span className="pb-1 text-sm text-neutral-500">از ۵</span>
            </div>
            <Stars value={summary.average ?? 0} size="md" />
            <p className="text-sm text-neutral-500">از مجموع {faNumber(summary.count)} دیدگاه</p>
            {summary.recommendedPercent !== null && (
              <p className="text-sm text-emerald-700">{faNumber(summary.recommendedPercent)}٪ خریداران این کالا را پیشنهاد کرده‌اند</p>
            )}
            <ul className="flex flex-col gap-1.5">
              {[5, 4, 3, 2, 1].map((stars) => {
                const count = summary.distribution[String(stars)] ?? 0;
                return (
                  <li key={stars} className="flex items-center gap-2 text-xs text-neutral-500">
                    <span className="w-3">{faNumber(stars)}</span>
                    <span className="h-1.5 flex-1 overflow-hidden rounded-full bg-neutral-200">
                      <span className="block h-full rounded-full bg-accent-400" style={{ width: `${(count / summary.count) * 100}%` }} />
                    </span>
                    <span className="w-6 text-end">{faNumber(count)}</span>
                  </li>
                );
              })}
            </ul>
          </>
        ) : (
          <p className="text-sm text-neutral-500">هنوز دیدگاهی برای این کالا ثبت نشده است. اولین نفر باشید!</p>
        )}
        <ReviewForm slug={slug} />
      </aside>
      <div>
        {items.length === 0 ? null : (
          <ul className="divide-y divide-neutral-100">
            {items.map((review) => (
              <li key={review.id} className="py-5">
                <div className="flex flex-wrap items-center gap-2">
                  <Stars value={review.rating} />
                  {review.title && <h3 className="font-bold text-neutral-800">{review.title}</h3>}
                </div>
                <p className="mt-1 text-xs text-neutral-400">
                  {review.authorName} · {jalaliDate(review.createdAt)}
                  {review.verifiedPurchase && (
                    <span className="ms-2 inline-flex items-center gap-1 text-emerald-700">
                      <BadgeCheck className="size-3.5" />
                      خریدار
                    </span>
                  )}
                </p>
                {review.comment && <p className="mt-3 whitespace-pre-line text-sm leading-7 text-neutral-700">{review.comment}</p>}
                {review.recommended !== null && (
                  <p className={`mt-3 flex items-center gap-1 text-xs ${review.recommended ? "text-emerald-700" : "text-danger-600"}`}>
                    {review.recommended ? <ThumbsUp className="size-4" /> : <ThumbsDown className="size-4" />}
                    {review.recommended ? "پیشنهاد می‌کنم" : "پیشنهاد نمی‌کنم"}
                  </p>
                )}
              </li>
            ))}
          </ul>
        )}
        {reviews.hasNextPage && (
          <Button variant="outline" className="mt-2" loading={reviews.isFetchingNextPage} onClick={() => reviews.fetchNextPage()}>
            دیدگاه‌های بیشتر
          </Button>
        )}
      </div>
    </div>
  );
}

function ReviewForm({ slug }: { slug: string }) {
  const { data: me } = useMe();
  const [open, setOpen] = useState(false);
  const [rating, setRating] = useState(0);
  const [title, setTitle] = useState("");
  const [comment, setComment] = useState("");
  const [recommended, setRecommended] = useState<boolean | null>(null);
  const submit = useMutation({
    mutationFn: () =>
      api<MyReview>(`products/${slug}/reviews`, {
        method: "POST",
        body: { rating, title: title || null, comment: comment || null, recommended },
      }),
  });

  if (!me) {
    return (
      <Link href={`/login?next=${encodeURIComponent(`/product/${slug}`)}`} className="text-sm font-medium text-brand-700">
        برای ثبت دیدگاه وارد شوید
      </Link>
    );
  }
  if (submit.isSuccess) {
    return <p className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-700">دیدگاه شما ثبت شد و پس از بررسی نمایش داده می‌شود.</p>;
  }
  if (!open) {
    return (
      <Button variant="outline" onClick={() => setOpen(true)}>
        <MessageSquarePlus className="size-5" />
        ثبت دیدگاه
      </Button>
    );
  }
  const errors = submit.error instanceof ApiError ? submit.error.errors : {};
  return (
    <form
      className="flex flex-col gap-3 rounded-2xl border border-neutral-200 p-4"
      onSubmit={(event) => {
        event.preventDefault();
        if (rating > 0) submit.mutate();
      }}
    >
      <fieldset>
        <legend className="mb-1 text-sm font-medium text-neutral-700">امتیاز شما</legend>
        <StarInput value={rating} onChange={setRating} />
        {errors.rating && <p className="mt-1 text-xs text-danger-600">{errors.rating}</p>}
      </fieldset>
      <TextField label="عنوان" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={150} error={errors.title} />
      <TextArea label="متن دیدگاه" value={comment} onChange={(e) => setComment(e.target.value)} maxLength={3000} error={errors.comment} />
      <div className="flex gap-2 text-sm">
        {[true, false].map((value) => (
          <button
            key={String(value)}
            type="button"
            aria-pressed={recommended === value}
            onClick={() => setRecommended(recommended === value ? null : value)}
            className={`flex items-center gap-1 rounded-full border px-3 py-1.5 ${recommended === value ? "border-brand-600 bg-brand-50 text-brand-700" : "border-neutral-300 text-neutral-600"}`}
          >
            {value ? <ThumbsUp className="size-4" /> : <ThumbsDown className="size-4" />}
            {value ? "پیشنهاد می‌کنم" : "پیشنهاد نمی‌کنم"}
          </button>
        ))}
      </div>
      <FormError message={submit.error && !Object.keys(errors).length ? submit.error.message : null} />
      <Button type="submit" disabled={rating === 0} loading={submit.isPending}>ثبت دیدگاه</Button>
    </form>
  );
}

function StarInput({ value, onChange }: { value: number; onChange: (value: number) => void }) {
  return (
    <div role="radiogroup" aria-label="امتیاز" className="flex gap-1">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          role="radio"
          aria-checked={value === star}
          aria-label={`${faNumber(star)} ستاره`}
          onClick={() => onChange(star)}
          className={`text-2xl leading-none ${star <= value ? "text-accent-400" : "text-neutral-300"}`}
        >
          ★
        </button>
      ))}
    </div>
  );
}
