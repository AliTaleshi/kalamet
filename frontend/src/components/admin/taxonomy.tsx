"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/error";
import type { Brand, CategoryNode } from "@/lib/api/types";
import { faNumber } from "@/lib/format";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { FormError, SelectField, TextField } from "@/components/ui/field";
import { PageSpinner } from "@/components/ui/spinner";
import { useToast } from "@/components/ui/toast";
import { AdminPageHeader, AdminTable } from "./admin-shell";

// --- Categories ---

type FlatCategory = CategoryNode & { depth: number; parentId: number | null };

function flatten(nodes: CategoryNode[], depth = 0, parentId: number | null = null): FlatCategory[] {
  return nodes.flatMap((n) => [{ ...n, depth, parentId }, ...flatten(n.children, depth + 1, n.id)]);
}

export function AdminCategories() {
  const client = useQueryClient();
  const notify = useToast();
  const categories = useQuery({ queryKey: ["categories"], queryFn: () => api<CategoryNode[]>("categories") });
  const [editing, setEditing] = useState<FlatCategory | "new" | null>(null);
  const remove = useMutation({
    mutationFn: (id: number) => api<void>(`admin/categories/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      notify("دسته‌بندی حذف شد");
      client.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (e) => notify(errorMessage(e), "error"),
  });
  const flat = flatten(categories.data ?? []);

  return (
    <>
      <AdminPageHeader title="دسته‌بندی‌ها" action={<Button onClick={() => setEditing("new")}><Plus className="size-4" />دسته‌بندی جدید</Button>} />
      {categories.isLoading ? <PageSpinner /> : (
        <Card>
          <ul className="divide-y divide-neutral-100">
            {flat.map((category) => (
              <li key={category.id} className="flex items-center justify-between gap-3 px-5 py-3 text-sm" style={{ paddingInlineStart: `${1.25 + category.depth * 1.5}rem` }}>
                <span>
                  <span className={category.depth === 0 ? "font-bold text-neutral-800" : "text-neutral-700"}>{category.name}</span>
                  <span className="ms-2 text-xs text-neutral-400" dir="ltr">{category.slug}</span>
                </span>
                <span className="flex gap-1">
                  <button type="button" aria-label="ویرایش" onClick={() => setEditing(category)} className="rounded-lg p-1.5 text-neutral-500 hover:bg-neutral-100"><Pencil className="size-4" /></button>
                  <button type="button" aria-label="حذف" onClick={() => window.confirm(`«${category.name}» حذف شود؟`) && remove.mutate(category.id)}
                    className="rounded-lg p-1.5 text-danger-600 hover:bg-red-50"><Trash2 className="size-4" /></button>
                </span>
              </li>
            ))}
          </ul>
        </Card>
      )}
      <Dialog open={editing !== null} onClose={() => setEditing(null)} title={editing === "new" ? "دسته‌بندی جدید" : "ویرایش دسته‌بندی"}>
        {editing && (
          <CategoryForm
            key={editing === "new" ? "new" : editing.id}
            category={editing === "new" ? null : editing}
            options={flat}
            onSaved={() => {
              setEditing(null);
              notify("دسته‌بندی ذخیره شد");
              client.invalidateQueries({ queryKey: ["categories"] });
            }}
          />
        )}
      </Dialog>
    </>
  );
}

function CategoryForm({ category, options, onSaved }: { category: FlatCategory | null; options: FlatCategory[]; onSaved: () => void }) {
  const [name, setName] = useState(category?.name ?? "");
  const [slug, setSlug] = useState(category?.slug ?? "");
  const [parentId, setParentId] = useState(category?.parentId ? String(category.parentId) : "");
  const [imageUrl, setImageUrl] = useState(category?.imageUrl ?? "");
  const save = useMutation({
    mutationFn: () => {
      const body = { name, slug, parentId: parentId ? Number(parentId) : null, imageUrl: imageUrl || null, sortOrder: 0 };
      return category
        ? api(`admin/categories/${category.id}`, { method: "PUT", body })
        : api("admin/categories", { method: "POST", body });
    },
    onSuccess: onSaved,
  });
  const error = save.error instanceof ApiError ? save.error : null;
  return (
    <form className="flex flex-col gap-4" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <TextField label="نام" required value={name} onChange={(e) => setName(e.target.value)} error={error?.errors.name} />
      <TextField label="نامک" ltr required value={slug} onChange={(e) => setSlug(e.target.value)} error={error?.errors.slug} hint="مثلاً mobile-phones" />
      <SelectField label="والد" value={parentId} onChange={(e) => setParentId(e.target.value)}>
        <option value="">— بدون والد (دسته اصلی) —</option>
        {options.filter((o) => o.id !== category?.id).map((o) => (
          <option key={o.id} value={o.id}>{"— ".repeat(o.depth)}{o.name}</option>
        ))}
      </SelectField>
      <TextField label="نشانی آیکون (اختیاری)" ltr type="url" value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} />
      <FormError message={error && !Object.keys(error.errors).length ? error.message : null} />
      <Button type="submit" loading={save.isPending}>ذخیره</Button>
    </form>
  );
}

// --- Brands ---

export function AdminBrands() {
  const client = useQueryClient();
  const notify = useToast();
  const brands = useQuery({ queryKey: ["brands"], queryFn: () => api<Brand[]>("brands") });
  const [editing, setEditing] = useState<Brand | "new" | null>(null);
  const remove = useMutation({
    mutationFn: (id: number) => api<void>(`admin/brands/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      notify("برند حذف شد");
      client.invalidateQueries({ queryKey: ["brands"] });
    },
    onError: (e) => notify(errorMessage(e), "error"),
  });
  return (
    <>
      <AdminPageHeader title="برندها" action={<Button onClick={() => setEditing("new")}><Plus className="size-4" />برند جدید</Button>} />
      {brands.isLoading ? <PageSpinner /> : (
        <AdminTable head={["نام", "نام لاتین", "نامک", ""]}>
          {brands.data?.map((brand) => (
            <tr key={brand.id}>
              <td className="px-4 py-3 font-medium">{brand.name}</td>
              <td className="px-4 py-3" dir="ltr">{brand.nameEn ?? "—"}</td>
              <td className="px-4 py-3 text-neutral-500" dir="ltr">{brand.slug}</td>
              <td className="px-4 py-3">
                <span className="flex justify-end gap-1">
                  <button type="button" aria-label="ویرایش" onClick={() => setEditing(brand)} className="rounded-lg p-1.5 text-neutral-500 hover:bg-neutral-100"><Pencil className="size-4" /></button>
                  <button type="button" aria-label="حذف"
                    onClick={() => window.confirm(`برند «${brand.name}» حذف شود؟ کالاهای آن بدون برند می‌مانند.`) && remove.mutate(brand.id)}
                    className="rounded-lg p-1.5 text-danger-600 hover:bg-red-50"><Trash2 className="size-4" /></button>
                </span>
              </td>
            </tr>
          ))}
        </AdminTable>
      )}
      <p className="mt-3 text-xs text-neutral-400">{faNumber(brands.data?.length ?? 0)} برند</p>
      <Dialog open={editing !== null} onClose={() => setEditing(null)} title={editing === "new" ? "برند جدید" : "ویرایش برند"}>
        {editing && (
          <BrandForm
            key={editing === "new" ? "new" : editing.id}
            brand={editing === "new" ? null : editing}
            onSaved={() => {
              setEditing(null);
              notify("برند ذخیره شد");
              client.invalidateQueries({ queryKey: ["brands"] });
            }}
          />
        )}
      </Dialog>
    </>
  );
}

function BrandForm({ brand, onSaved }: { brand: Brand | null; onSaved: () => void }) {
  const [name, setName] = useState(brand?.name ?? "");
  const [nameEn, setNameEn] = useState(brand?.nameEn ?? "");
  const [slug, setSlug] = useState(brand?.slug ?? "");
  const [logoUrl, setLogoUrl] = useState(brand?.logoUrl ?? "");
  const save = useMutation({
    mutationFn: () => {
      const body = { name, nameEn: nameEn || null, slug, logoUrl: logoUrl || null };
      return brand ? api(`admin/brands/${brand.id}`, { method: "PUT", body }) : api("admin/brands", { method: "POST", body });
    },
    onSuccess: onSaved,
  });
  const error = save.error instanceof ApiError ? save.error : null;
  return (
    <form className="flex flex-col gap-4" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <TextField label="نام" required value={name} onChange={(e) => setName(e.target.value)} error={error?.errors.name} />
      <TextField label="نام لاتین" ltr value={nameEn} onChange={(e) => setNameEn(e.target.value)} />
      <TextField label="نامک" ltr required value={slug} onChange={(e) => setSlug(e.target.value)} error={error?.errors.slug} />
      <TextField label="نشانی لوگو (اختیاری)" ltr type="url" value={logoUrl} onChange={(e) => setLogoUrl(e.target.value)} />
      <FormError message={error && !Object.keys(error.errors).length ? error.message : null} />
      <Button type="submit" loading={save.isPending}>ذخیره</Button>
    </form>
  );
}
