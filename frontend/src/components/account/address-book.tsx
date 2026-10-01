"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { MapPin, Pencil, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/error";
import type { Address } from "@/lib/api/types";
import { faNumber } from "@/lib/format";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardHeader } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { EmptyState } from "@/components/ui/empty-state";
import { PageSpinner } from "@/components/ui/spinner";
import { useToast } from "@/components/ui/toast";
import { AddressForm } from "./address-form";

const KEY = ["addresses"];

export function AddressBook() {
  const client = useQueryClient();
  const notify = useToast();
  const addresses = useQuery({ queryKey: KEY, queryFn: () => api<Address[]>("me/addresses") });
  const [editing, setEditing] = useState<Address | "new" | null>(null);
  const refresh = () => client.invalidateQueries({ queryKey: KEY });

  const makeDefault = useMutation({
    mutationFn: (id: number) => api<Address>(`me/addresses/${id}/default`, { method: "POST" }),
    onSuccess: refresh,
    onError: (e) => notify(errorMessage(e), "error"),
  });
  const remove = useMutation({
    mutationFn: (id: number) => api<void>(`me/addresses/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      notify("نشانی حذف شد");
      refresh();
    },
    onError: (e) => notify(errorMessage(e), "error"),
  });

  return (
    <Card>
      <CardHeader
        title="نشانی‌ها"
        action={<Button size="sm" variant="outline" onClick={() => setEditing("new")}><Plus className="size-4" />ثبت نشانی جدید</Button>}
      />
      {addresses.isLoading ? (
        <PageSpinner />
      ) : !addresses.data?.length ? (
        <EmptyState icon={MapPin} title="هنوز نشانی ثبت نکرده‌اید" />
      ) : (
        <ul className="divide-y divide-neutral-100">
          {addresses.data.map((address) => (
            <li key={address.id} className="flex flex-col gap-2 p-5 text-sm">
              <div className="flex items-start justify-between gap-3">
                <p className="font-medium leading-7 text-neutral-800">
                  {address.province.name}، {address.city}، {address.addressLine}، پلاک {faNumber(address.plaque)}
                  {address.unit ? `، واحد ${faNumber(address.unit)}` : ""}
                </p>
                {address.isDefault && <Badge tone="brand">پیش‌فرض</Badge>}
              </div>
              <p className="text-neutral-500">
                {address.recipientName} · {faNumber(address.recipientMobile)} · کد پستی {faNumber(address.postalCode)}
              </p>
              <div className="flex gap-2">
                <Button size="sm" variant="ghost" onClick={() => setEditing(address)}><Pencil className="size-4" />ویرایش</Button>
                {!address.isDefault && (
                  <Button size="sm" variant="ghost" loading={makeDefault.isPending && makeDefault.variables === address.id}
                    onClick={() => makeDefault.mutate(address.id)}>
                    انتخاب به‌عنوان پیش‌فرض
                  </Button>
                )}
                <Button size="sm" variant="ghost" className="text-danger-600" loading={remove.isPending && remove.variables === address.id}
                  onClick={() => window.confirm("این نشانی حذف شود؟") && remove.mutate(address.id)}>
                  <Trash2 className="size-4" />حذف
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}
      <Dialog open={editing !== null} onClose={() => setEditing(null)} title={editing === "new" ? "ثبت نشانی جدید" : "ویرایش نشانی"}>
        <AddressForm
          key={editing === "new" || editing === null ? "new" : editing.id}
          address={editing && editing !== "new" ? editing : undefined}
          onSaved={() => {
            setEditing(null);
            notify("نشانی ذخیره شد");
            refresh();
          }}
        />
      </Dialog>
    </Card>
  );
}
