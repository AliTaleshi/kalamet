"use client";

import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "@/lib/api/client";
import { ApiError } from "@/lib/api/error";
import type { Address, AddressInput, Province } from "@/lib/api/types";
import { Button } from "@/components/ui/button";
import { Checkbox, FormError, SelectField, TextArea, TextField } from "@/components/ui/field";

const EMPTY: AddressInput = {
  recipientName: "",
  recipientMobile: "",
  provinceId: null,
  city: "",
  addressLine: "",
  plaque: "",
  unit: "",
  postalCode: "",
  makeDefault: false,
};

/** Create (no `address`) or edit an address; returns the saved address. */
export function AddressForm({ address, onSaved }: { address?: Address; onSaved: (address: Address) => void }) {
  const provinces = useQuery({ queryKey: ["provinces"], queryFn: () => api<Province[]>("provinces"), staleTime: Infinity });
  const [form, setForm] = useState<AddressInput>(
    address
      ? {
          recipientName: address.recipientName,
          recipientMobile: address.recipientMobile,
          provinceId: address.province.id,
          city: address.city,
          addressLine: address.addressLine,
          plaque: address.plaque,
          unit: address.unit ?? "",
          postalCode: address.postalCode,
          makeDefault: address.isDefault,
        }
      : EMPTY,
  );
  const [error, setError] = useState<ApiError | null>(null);
  const [busy, setBusy] = useState(false);
  const set = <K extends keyof AddressInput>(key: K, value: AddressInput[K]) => setForm((f) => ({ ...f, [key]: value }));
  const errors = error?.errors ?? {};

  return (
    <form
      onSubmit={async (event) => {
        event.preventDefault();
        setBusy(true);
        setError(null);
        try {
          const saved = await api<Address>(address ? `me/addresses/${address.id}` : "me/addresses", {
            method: address ? "PUT" : "POST",
            body: { ...form, unit: form.unit || null },
          });
          onSaved(saved);
        } catch (e) {
          setError(e instanceof ApiError ? e : new ApiError({ status: 0, code: "NETWORK_ERROR", detail: "اتصال برقرار نشد." }));
        } finally {
          setBusy(false);
        }
      }}
      className="grid gap-4 sm:grid-cols-2"
    >
      <TextField label="نام و نام خانوادگی گیرنده" autoComplete="name" required value={form.recipientName}
        onChange={(e) => set("recipientName", e.target.value)} error={errors.recipientName} />
      <TextField label="شماره موبایل گیرنده" type="tel" inputMode="tel" ltr autoComplete="tel" required value={form.recipientMobile}
        onChange={(e) => set("recipientMobile", e.target.value)} error={errors.recipientMobile} />
      <SelectField label="استان" required value={form.provinceId ?? ""} onChange={(e) => set("provinceId", e.target.value ? Number(e.target.value) : null)}
        error={errors.provinceId}>
        <option value="">انتخاب استان</option>
        {provinces.data?.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
      </SelectField>
      <TextField label="شهر" required value={form.city} onChange={(e) => set("city", e.target.value)} error={errors.city} />
      <TextArea label="نشانی پستی" required className="sm:col-span-2" value={form.addressLine}
        onChange={(e) => set("addressLine", e.target.value)} error={errors.addressLine} placeholder="خیابان، کوچه، ساختمان" />
      <div className="grid grid-cols-2 gap-4">
        <TextField label="پلاک" required value={form.plaque} onChange={(e) => set("plaque", e.target.value)} error={errors.plaque} />
        <TextField label="واحد" value={form.unit} onChange={(e) => set("unit", e.target.value)} error={errors.unit} />
      </div>
      <TextField label="کد پستی" inputMode="numeric" ltr required maxLength={12} hint="۱۰ رقم، بدون خط تیره" value={form.postalCode}
        onChange={(e) => set("postalCode", e.target.value)} error={errors.postalCode} />
      <div className="sm:col-span-2">
        <Checkbox label="نشانی پیش‌فرض من باشد" checked={form.makeDefault} onChange={(e) => set("makeDefault", e.target.checked)} />
      </div>
      <div className="sm:col-span-2">
        <FormError message={error && !Object.keys(errors).length ? error.message : null} />
      </div>
      <Button type="submit" loading={busy} className="sm:col-span-2">ذخیره نشانی</Button>
    </form>
  );
}
