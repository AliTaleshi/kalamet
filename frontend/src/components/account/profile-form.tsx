"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "@/lib/api/client";
import { ApiError } from "@/lib/api/error";
import type { Profile } from "@/lib/api/types";
import { faNumber, jalaliDate } from "@/lib/format";
import { ME_KEY, useMe } from "@/lib/hooks/session";
import { Button } from "@/components/ui/button";
import { Card, CardHeader } from "@/components/ui/card";
import { FormError, TextField } from "@/components/ui/field";
import { PageSpinner } from "@/components/ui/spinner";
import { useToast } from "@/components/ui/toast";

export function ProfileForm() {
  const { data: me, isLoading } = useMe();
  if (isLoading || !me) return <PageSpinner />;
  // Remount when the profile changes, so the inputs start from the saved values.
  return <Form key={me.id} me={me} />;
}

function Form({ me }: { me: Profile }) {
  const client = useQueryClient();
  const notify = useToast();
  const [firstName, setFirstName] = useState(me.firstName ?? "");
  const [lastName, setLastName] = useState(me.lastName ?? "");
  const [email, setEmail] = useState(me.email ?? "");
  const [error, setError] = useState<ApiError | null>(null);
  const [busy, setBusy] = useState(false);

  return (
    <Card>
      <CardHeader title="اطلاعات حساب کاربری" />
      {/* noValidate: the API's Persian messages are shown instead of the browser's own. */}
      <form
        noValidate
        className="grid gap-4 p-5 sm:grid-cols-2"
        onSubmit={async (event) => {
          event.preventDefault();
          setBusy(true);
          setError(null);
          try {
            const saved = await api<Profile>("me", { method: "PUT", body: { firstName, lastName, email: email || null } });
            client.setQueryData(ME_KEY, saved);
            notify("اطلاعات حساب ذخیره شد");
          } catch (e) {
            setError(e instanceof ApiError ? e : null);
          } finally {
            setBusy(false);
          }
        }}
      >
        <TextField label="نام" autoComplete="given-name" value={firstName} onChange={(e) => setFirstName(e.target.value)} error={error?.errors.firstName} />
        <TextField label="نام خانوادگی" autoComplete="family-name" value={lastName} onChange={(e) => setLastName(e.target.value)} error={error?.errors.lastName} />
        <TextField label="شماره موبایل" ltr value={faNumber(me.mobile)} disabled hint="شماره موبایل قابل تغییر نیست." />
        <TextField label="ایمیل" type="email" ltr autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={error?.errors.email} />
        <p className="text-xs text-neutral-500 sm:col-span-2">عضویت از {jalaliDate(me.createdAt)}</p>
        <div className="sm:col-span-2">
          <FormError message={error && !Object.keys(error.errors).length ? error.message : null} />
        </div>
        <Button type="submit" loading={busy} className="w-fit">ذخیره تغییرات</Button>
      </form>
    </Card>
  );
}
