"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { clsx } from "clsx";
import { BadgeCheck, Check, X } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { api, query } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/error";
import type { AdminReview, AdminUser, Page, ReviewStatus, Role } from "@/lib/api/types";
import { faNumber, jalaliDate, jalaliDateTime, REVIEW_STATUS_LABELS } from "@/lib/format";
import { useDebounced } from "@/lib/hooks/debounce";
import { useMe } from "@/lib/hooks/session";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Pagination } from "@/components/ui/pagination";
import { Stars } from "@/components/ui/rating";
import { PageSpinner } from "@/components/ui/spinner";
import { ReviewStatusBadge } from "@/components/ui/status-badges";
import { useToast } from "@/components/ui/toast";
import { AdminPageHeader, AdminTable } from "./admin-shell";

// --- Reviews ---

export function AdminReviews() {
  const client = useQueryClient();
  const notify = useToast();
  const [status, setStatus] = useState<ReviewStatus | null>("PENDING");
  const [page, setPage] = useState(0);
  const reviews = useQuery({
    queryKey: ["admin", "reviews", status, page],
    queryFn: () => api<Page<AdminReview>>(`admin/reviews${query({ status, page, size: 20 })}`),
    placeholderData: keepPreviousData,
  });
  const moderate = useMutation({
    mutationFn: ({ id, decision }: { id: number; decision: ReviewStatus }) =>
      api<AdminReview>(`admin/reviews/${id}`, { method: "PATCH", body: { status: decision } }),
    onSuccess: (review) => {
      notify(review.status === "APPROVED" ? "دیدگاه تأیید شد" : "دیدگاه رد شد");
      client.invalidateQueries({ queryKey: ["admin", "reviews"] });
    },
    onError: (e) => notify(errorMessage(e), "error"),
  });

  return (
    <>
      <AdminPageHeader title="دیدگاه‌ها" />
      <div className="mb-4 flex gap-2">
        {([null, "PENDING", "APPROVED", "REJECTED"] as const).map((value) => (
          <button
            key={value ?? "all"}
            type="button"
            onClick={() => {
              setStatus(value);
              setPage(0);
            }}
            className={clsx("rounded-full border px-3 py-1.5 text-sm", value === status ? "border-brand-600 bg-brand-600 text-white" : "border-neutral-300 bg-white text-neutral-600")}
          >
            {value ? REVIEW_STATUS_LABELS[value] : "همه"}
          </button>
        ))}
      </div>
      {reviews.isLoading ? <PageSpinner /> : !reviews.data?.items.length ? (
        <p className="p-6 text-center text-sm text-neutral-500">دیدگاهی در این بخش نیست.</p>
      ) : (
        <>
          <ul className="flex flex-col gap-3">
            {reviews.data.items.map((review) => (
              <Card key={review.id} className="flex flex-col gap-2 p-5 text-sm">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <Link href={`/product/${review.product.slug}`} target="_blank" className="font-bold text-brand-700">{review.product.name}</Link>
                  <ReviewStatusBadge status={review.status} />
                </div>
                <div className="flex flex-wrap items-center gap-3 text-xs text-neutral-500">
                  <Stars value={review.rating} />
                  <span>{review.authorName} ({faNumber(review.userMobile)})</span>
                  <span>{jalaliDateTime(review.createdAt)}</span>
                  {review.verifiedPurchase && <span className="flex items-center gap-1 text-emerald-700"><BadgeCheck className="size-4" />خریدار</span>}
                </div>
                {review.title && <p className="font-medium text-neutral-800">{review.title}</p>}
                {review.comment && <p className="whitespace-pre-line leading-7 text-neutral-700">{review.comment}</p>}
                <div className="flex gap-2 pt-1">
                  {review.status !== "APPROVED" && (
                    <Button size="sm" loading={moderate.isPending && moderate.variables?.id === review.id && moderate.variables.decision === "APPROVED"}
                      onClick={() => moderate.mutate({ id: review.id, decision: "APPROVED" })}>
                      <Check className="size-4" />تأیید
                    </Button>
                  )}
                  {review.status !== "REJECTED" && (
                    <Button size="sm" variant="outline" loading={moderate.isPending && moderate.variables?.id === review.id && moderate.variables.decision === "REJECTED"}
                      onClick={() => moderate.mutate({ id: review.id, decision: "REJECTED" })}>
                      <X className="size-4" />رد
                    </Button>
                  )}
                </div>
              </Card>
            ))}
          </ul>
          <Pagination page={page} totalPages={reviews.data.totalPages} onChange={setPage} />
        </>
      )}
    </>
  );
}

// --- Users ---

export function AdminUsers() {
  const client = useQueryClient();
  const notify = useToast();
  const { data: me } = useMe();
  const [search, setSearch] = useState("");
  const [role, setRole] = useState<Role | "">("");
  const [page, setPage] = useState(0);
  const q = useDebounced(search.trim());
  const users = useQuery({
    queryKey: ["admin", "users", q, role, page],
    queryFn: () => api<Page<AdminUser>>(`admin/users${query({ q, role, page, size: 20 })}`),
    placeholderData: keepPreviousData,
  });
  const update = useMutation({
    mutationFn: ({ id, change }: { id: number; change: { role?: Role; active?: boolean } }) =>
      api<AdminUser>(`admin/users/${id}`, { method: "PATCH", body: change }),
    onSuccess: () => {
      notify("کاربر به‌روزرسانی شد");
      client.invalidateQueries({ queryKey: ["admin", "users"] });
    },
    onError: (e) => notify(errorMessage(e), "error"),
  });

  return (
    <>
      <AdminPageHeader title="کاربران" />
      <div className="mb-4 flex flex-wrap gap-2">
        <input type="search" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          placeholder="موبایل یا نام" aria-label="جستجوی کاربر" className="h-10 w-72 rounded-lg border border-neutral-300 bg-white px-3 text-sm" />
        <select value={role} aria-label="نقش" onChange={(e) => { setRole(e.target.value as Role | ""); setPage(0); }}
          className="h-10 rounded-lg border border-neutral-300 bg-white px-3 text-sm">
          <option value="">همه نقش‌ها</option>
          <option value="CUSTOMER">مشتری</option>
          <option value="ADMIN">مدیر</option>
        </select>
      </div>
      {users.isLoading ? <PageSpinner /> : (
        <>
          <AdminTable head={["کاربر", "موبایل", "ایمیل", "عضویت", "نقش", "وضعیت"]}>
            {users.data?.items.map((user) => {
              const self = user.id === me?.id;
              const busy = update.isPending && update.variables?.id === user.id;
              return (
                <tr key={user.id}>
                  <td className="px-4 py-3">{[user.firstName, user.lastName].filter(Boolean).join(" ") || "—"}</td>
                  <td className="px-4 py-3" dir="ltr">{faNumber(user.mobile)}</td>
                  <td className="px-4 py-3" dir="ltr">{user.email ?? "—"}</td>
                  <td className="px-4 py-3 text-neutral-500">{jalaliDate(user.createdAt)}</td>
                  <td className="px-4 py-3">
                    <select value={user.role} disabled={self || busy} aria-label="نقش"
                      onChange={(e) => update.mutate({ id: user.id, change: { role: e.target.value as Role } })}
                      className="h-9 rounded-lg border border-neutral-300 bg-white px-2 text-sm disabled:bg-neutral-100">
                      <option value="CUSTOMER">مشتری</option>
                      <option value="ADMIN">مدیر</option>
                    </select>
                  </td>
                  <td className="px-4 py-3">
                    {self ? <Badge tone="brand">شما</Badge> : (
                      <button type="button" disabled={busy}
                        onClick={() => (user.active ? window.confirm("حساب این کاربر غیرفعال شود؟ همه نشست‌های او بسته می‌شود.") : true)
                          && update.mutate({ id: user.id, change: { active: !user.active } })}>
                        {user.active ? <Badge tone="success">فعال</Badge> : <Badge tone="danger">غیرفعال</Badge>}
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </AdminTable>
          <Pagination page={page} totalPages={users.data?.totalPages ?? 0} onChange={setPage} />
        </>
      )}
    </>
  );
}
