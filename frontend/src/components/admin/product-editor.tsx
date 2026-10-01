"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, ExternalLink, ImagePlus, Pencil, Plus, Trash2 } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/error";
import type { AdminProduct, AdminVariant, Brand, CategoryNode, Spec } from "@/lib/api/types";
import { attributesText, faNumber, jalaliDateTime, toman } from "@/lib/format";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardHeader } from "@/components/ui/card";
import { Dialog } from "@/components/ui/dialog";
import { Checkbox, FormError, SelectField, TextArea, TextField } from "@/components/ui/field";
import { ProductImage } from "@/components/ui/product-image";
import { PageSpinner } from "@/components/ui/spinner";
import { useToast } from "@/components/ui/toast";
import { fromLocalInput, rialToToman, slugify, toLocalInput, tomanToRial } from "./catalog-inputs";

type ProductFields = {
  categoryId: number | null;
  brandId: number | null;
  name: string;
  nameEn: string;
  slug: string;
  description: string;
  active: boolean;
};

type VariantFields = {
  sku: string;
  attributes: { key: string; value: string }[];
  price: string;
  compareAtPrice: string;
  discountEndsAt: string;
  stock: string;
  active: boolean;
};

const EMPTY_VARIANT: VariantFields = {
  sku: "",
  attributes: [],
  price: "",
  compareAtPrice: "",
  discountEndsAt: "",
  stock: "0",
  active: true,
};

function variantBody(fields: VariantFields, version?: number) {
  return {
    sku: fields.sku.trim(),
    attributes: Object.fromEntries(fields.attributes.filter((a) => a.key.trim()).map((a) => [a.key.trim(), a.value])),
    price: tomanToRial(fields.price),
    compareAtPrice: tomanToRial(fields.compareAtPrice),
    discountEndsAt: fromLocalInput(fields.discountEndsAt),
    stock: Number(fields.stock || 0),
    active: fields.active,
    version,
  };
}

function useCatalogOptions() {
  const categories = useQuery({ queryKey: ["categories"], queryFn: () => api<CategoryNode[]>("categories") });
  const brands = useQuery({ queryKey: ["brands"], queryFn: () => api<Brand[]>("brands") });
  const flat: { id: number; label: string }[] = [];
  const walk = (nodes: CategoryNode[], depth: number) =>
    nodes.forEach((n) => {
      flat.push({ id: n.id, label: `${"— ".repeat(depth)}${n.name}` });
      walk(n.children, depth + 1);
    });
  walk(categories.data ?? [], 0);
  return { categories: flat, brands: brands.data ?? [] };
}

// --- Create ---

export function NewProduct() {
  const router = useRouter();
  const [product, setProduct] = useState<ProductFields>({
    categoryId: null, brandId: null, name: "", nameEn: "", slug: "", description: "", active: true,
  });
  const [variants, setVariants] = useState<VariantFields[]>([EMPTY_VARIANT]);
  const [error, setError] = useState<ApiError | null>(null);
  const [busy, setBusy] = useState(false);

  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={async (event) => {
        event.preventDefault();
        setBusy(true);
        setError(null);
        try {
          const created = await api<AdminProduct>("admin/products", {
            method: "POST",
            body: { product: productBody(product), variants: variants.map((v) => variantBody(v)), specs: [], images: [] },
          });
          router.push(`/admin/products/${created.id}`);
        } catch (e) {
          setError(e instanceof ApiError ? e : null);
          setBusy(false);
        }
      }}
    >
      <BackLink />
      <h1 className="text-xl font-black text-neutral-800">کالای جدید</h1>
      <Card className="p-5">
        <ProductFieldsForm value={product} onChange={setProduct} errors={error?.errors} />
      </Card>
      <Card>
        <CardHeader
          title="تنوع‌ها"
          action={<Button size="sm" variant="outline" onClick={() => setVariants((v) => [...v, EMPTY_VARIANT])}><Plus className="size-4" />تنوع دیگر</Button>}
        />
        <div className="flex flex-col divide-y divide-neutral-100">
          {variants.map((variant, index) => (
            <div key={index} className="p-5">
              <VariantFieldsForm value={variant} onChange={(v) => setVariants((all) => all.map((x, i) => (i === index ? v : x)))} />
              {variants.length > 1 && (
                <Button size="sm" variant="ghost" className="mt-3 text-danger-600" onClick={() => setVariants((all) => all.filter((_, i) => i !== index))}>
                  <Trash2 className="size-4" />حذف این تنوع
                </Button>
              )}
            </div>
          ))}
        </div>
      </Card>
      <FormError message={error ? (Object.keys(error.errors).length ? Object.values(error.errors).join(" ") : error.message) : null} />
      <Button type="submit" size="lg" loading={busy} className="w-fit">ثبت کالا</Button>
    </form>
  );
}

function productBody(p: ProductFields) {
  return {
    categoryId: p.categoryId,
    brandId: p.brandId,
    name: p.name,
    nameEn: p.nameEn || null,
    slug: p.slug,
    description: p.description || null,
    active: p.active,
  };
}

// --- Edit ---

export function EditProduct({ id }: { id: number }) {
  const key = ["admin", "product", id];
  const product = useQuery({ queryKey: key, queryFn: () => api<AdminProduct>(`admin/products/${id}`) });
  if (product.isLoading) return <PageSpinner />;
  if (!product.data) return <p className="text-neutral-500">کالا پیدا نشد.</p>;
  return <EditProductLoaded product={product.data} />;
}

function EditProductLoaded({ product }: { product: AdminProduct }) {
  const client = useQueryClient();
  const notify = useToast();
  const key = ["admin", "product", product.id];
  const [fields, setFields] = useState<ProductFields>({
    categoryId: product.category.id,
    brandId: product.brand?.id ?? null,
    name: product.name,
    nameEn: product.nameEn ?? "",
    slug: product.slug,
    description: product.description ?? "",
    active: product.active,
  });
  const save = useMutation({
    mutationFn: () => api<AdminProduct>(`admin/products/${product.id}`, { method: "PUT", body: productBody(fields) }),
    onSuccess: (updated) => {
      client.setQueryData(key, updated);
      client.invalidateQueries({ queryKey: ["admin", "products"] });
      notify("اطلاعات کالا ذخیره شد");
    },
  });
  const saveError = save.error instanceof ApiError ? save.error : null;

  return (
    <div className="flex flex-col gap-4">
      <BackLink />
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-xl font-black text-neutral-800">{product.name}</h1>
        {product.active ? <Badge tone="success">فعال</Badge> : <Badge>غیرفعال</Badge>}
        {product.active && (
          <Link href={`/product/${product.slug}`} target="_blank" className="flex items-center gap-1 text-sm text-brand-700">
            مشاهده در فروشگاه <ExternalLink className="size-4" />
          </Link>
        )}
        <span className="ms-auto text-xs text-neutral-400">آخرین تغییر: {jalaliDateTime(product.updatedAt)}</span>
      </div>
      <Card className="p-5">
        <form
          onSubmit={(event) => {
            event.preventDefault();
            save.mutate();
          }}
          className="flex flex-col gap-4"
        >
          <ProductFieldsForm value={fields} onChange={setFields} errors={saveError?.errors} />
          <FormError message={saveError && !Object.keys(saveError.errors).length ? saveError.message : null} />
          <Button type="submit" loading={save.isPending} className="w-fit">ذخیره اطلاعات کالا</Button>
        </form>
      </Card>
      <Variants product={product} />
      <Specs product={product} />
      <Images product={product} />
    </div>
  );
}

function Variants({ product }: { product: AdminProduct }) {
  const client = useQueryClient();
  const notify = useToast();
  const [editing, setEditing] = useState<AdminVariant | "new" | null>(null);
  return (
    <Card>
      <CardHeader title="تنوع‌ها و موجودی" action={<Button size="sm" variant="outline" onClick={() => setEditing("new")}><Plus className="size-4" />تنوع جدید</Button>} />
      <div className="overflow-x-auto">
        <table className="w-full min-w-[40rem] text-sm">
          <thead className="bg-neutral-50 text-neutral-500">
            <tr>
              {["کد انبار", "ویژگی‌ها", "قیمت (تومان)", "قبل از تخفیف", "پایان پیشنهاد", "موجودی", "وضعیت", ""].map((h) => (
                <th key={h} className="px-4 py-3 text-start font-medium">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-100">
            {product.variants.map((variant) => (
              <tr key={variant.id}>
                <td className="px-4 py-3" dir="ltr">{variant.sku}</td>
                <td className="px-4 py-3">{attributesText(variant.attributes) || "—"}</td>
                <td className="px-4 py-3">{toman(variant.price)}</td>
                <td className="px-4 py-3">{variant.compareAtPrice ? toman(variant.compareAtPrice) : "—"}</td>
                <td className="px-4 py-3 text-xs">{variant.discountEndsAt ? jalaliDateTime(variant.discountEndsAt) : "—"}</td>
                <td className={`px-4 py-3 font-bold ${variant.stock <= 5 ? "text-danger-600" : ""}`}>{faNumber(variant.stock)}</td>
                <td className="px-4 py-3">{variant.active ? <Badge tone="success">فعال</Badge> : <Badge>غیرفعال</Badge>}</td>
                <td className="px-4 py-3">
                  <button type="button" aria-label="ویرایش تنوع" onClick={() => setEditing(variant)} className="rounded-lg p-1.5 text-neutral-500 hover:bg-neutral-100">
                    <Pencil className="size-4" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Dialog open={editing !== null} onClose={() => setEditing(null)} title={editing === "new" ? "تنوع جدید" : "ویرایش تنوع"}>
        {editing && (
          <VariantDialog
            key={editing === "new" ? "new" : editing.id}
            productId={product.id}
            variant={editing === "new" ? null : editing}
            onSaved={() => {
              setEditing(null);
              notify("تنوع ذخیره شد");
              client.invalidateQueries({ queryKey: ["admin", "product", product.id] });
            }}
          />
        )}
      </Dialog>
    </Card>
  );
}

function VariantDialog({ productId, variant, onSaved }: { productId: number; variant: AdminVariant | null; onSaved: () => void }) {
  const [fields, setFields] = useState<VariantFields>(
    variant
      ? {
          sku: variant.sku,
          attributes: Object.entries(variant.attributes).map(([key, value]) => ({ key, value })),
          price: rialToToman(variant.price),
          compareAtPrice: rialToToman(variant.compareAtPrice),
          discountEndsAt: toLocalInput(variant.discountEndsAt),
          stock: String(variant.stock),
          active: variant.active,
        }
      : EMPTY_VARIANT,
  );
  const save = useMutation({
    mutationFn: () =>
      variant
        ? api<AdminVariant>(`admin/variants/${variant.id}`, { method: "PUT", body: variantBody(fields, variant.version) })
        : api<AdminVariant>(`admin/products/${productId}/variants`, { method: "POST", body: variantBody(fields) }),
    onSuccess: onSaved,
  });
  const error = save.error instanceof ApiError ? save.error : null;
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
      className="flex flex-col gap-4"
    >
      <VariantFieldsForm value={fields} onChange={setFields} errors={error?.errors} />
      <FormError
        message={error ? (error.code === "STALE_VARIANT" ? `${error.message}` : Object.keys(error.errors).length ? null : error.message) : null}
      />
      <Button type="submit" loading={save.isPending}>ذخیره تنوع</Button>
    </form>
  );
}

function Specs({ product }: { product: AdminProduct }) {
  const client = useQueryClient();
  const notify = useToast();
  const [rows, setRows] = useState<Spec[]>(product.specs.length ? product.specs : [{ groupName: "مشخصات کلی", name: "", value: "" }]);
  const save = useMutation({
    mutationFn: () =>
      api<AdminProduct>(`admin/products/${product.id}/specs`, {
        method: "PUT",
        body: rows.filter((r) => r.name.trim() && r.value.trim()).map((r) => ({ ...r, groupName: r.groupName || null })),
      }),
    onSuccess: (updated) => {
      client.setQueryData(["admin", "product", product.id], updated);
      notify("مشخصات ذخیره شد");
    },
  });
  const update = (index: number, change: Partial<Spec>) => setRows((all) => all.map((r, i) => (i === index ? { ...r, ...change } : r)));
  return (
    <Card>
      <CardHeader title="مشخصات فنی" />
      <div className="flex flex-col gap-3 p-5">
        {rows.map((row, index) => (
          <div key={index} className="grid gap-2 sm:grid-cols-[10rem_12rem_1fr_auto]">
            <input aria-label="گروه" placeholder="گروه" value={row.groupName ?? ""} onChange={(e) => update(index, { groupName: e.target.value })}
              className="h-10 rounded-lg border border-neutral-300 px-3 text-sm" />
            <input aria-label="عنوان" placeholder="عنوان (مثلاً وزن)" value={row.name} onChange={(e) => update(index, { name: e.target.value })}
              className="h-10 rounded-lg border border-neutral-300 px-3 text-sm" />
            <input aria-label="مقدار" placeholder="مقدار" value={row.value} onChange={(e) => update(index, { value: e.target.value })}
              className="h-10 rounded-lg border border-neutral-300 px-3 text-sm" />
            <button type="button" aria-label="حذف ردیف" onClick={() => setRows((all) => all.filter((_, i) => i !== index))}
              className="flex size-10 items-center justify-center rounded-lg text-danger-600 hover:bg-red-50">
              <Trash2 className="size-4" />
            </button>
          </div>
        ))}
        <div className="flex gap-2">
          <Button size="sm" variant="ghost" onClick={() => setRows((all) => [...all, { groupName: all.at(-1)?.groupName ?? "", name: "", value: "" }])}>
            <Plus className="size-4" />ردیف جدید
          </Button>
          <Button size="sm" loading={save.isPending} onClick={() => save.mutate()}>ذخیره مشخصات</Button>
        </div>
        <FormError message={save.error ? errorMessage(save.error) : null} />
      </div>
    </Card>
  );
}

function Images({ product }: { product: AdminProduct }) {
  const client = useQueryClient();
  const notify = useToast();
  const [url, setUrl] = useState("");
  const [altText, setAltText] = useState("");
  const [variantId, setVariantId] = useState("");
  const refresh = () => client.invalidateQueries({ queryKey: ["admin", "product", product.id] });
  const add = useMutation({
    mutationFn: () =>
      api<AdminProduct>(`admin/products/${product.id}/images`, {
        method: "POST",
        body: { url: url.trim(), altText: altText || null, variantId: variantId ? Number(variantId) : null, sortOrder: product.images.length },
      }),
    onSuccess: (updated) => {
      client.setQueryData(["admin", "product", product.id], updated);
      setUrl("");
      setAltText("");
      notify("تصویر اضافه شد");
    },
  });
  const remove = useMutation({
    mutationFn: (imageId: number) => api<void>(`admin/products/${product.id}/images/${imageId}`, { method: "DELETE" }),
    onSuccess: refresh,
    onError: (e) => notify(errorMessage(e), "error"),
  });
  return (
    <Card>
      <CardHeader title="تصاویر" />
      <div className="flex flex-col gap-4 p-5">
        <ul className="flex flex-wrap gap-3">
          {product.images.map((image, index) => (
            <li key={image.id} className="relative">
              <ProductImage src={image.url} alt={image.altText ?? ""} className="size-28 rounded-xl border border-neutral-200" />
              {index === 0 && <span className="absolute top-1 right-1 rounded bg-brand-600 px-1.5 text-xs text-white">اصلی</span>}
              <button type="button" aria-label="حذف تصویر" onClick={() => window.confirm("این تصویر حذف شود؟") && remove.mutate(image.id)}
                className="absolute bottom-1 left-1 rounded-lg bg-white/90 p-1 text-danger-600 shadow">
                <Trash2 className="size-4" />
              </button>
            </li>
          ))}
        </ul>
        <form
          onSubmit={(event) => {
            event.preventDefault();
            if (url.trim()) add.mutate();
          }}
          className="grid gap-3 sm:grid-cols-[1fr_12rem_12rem_auto] sm:items-end"
        >
          <TextField label="نشانی تصویر (URL)" ltr type="url" required value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://..." />
          <TextField label="متن جایگزین" value={altText} onChange={(e) => setAltText(e.target.value)} />
          <SelectField label="مربوط به تنوع" value={variantId} onChange={(e) => setVariantId(e.target.value)}>
            <option value="">همه تنوع‌ها</option>
            {product.variants.map((v) => <option key={v.id} value={v.id}>{attributesText(v.attributes) || v.sku}</option>)}
          </SelectField>
          <Button type="submit" loading={add.isPending}><ImagePlus className="size-4" />افزودن</Button>
        </form>
        <FormError message={add.error ? errorMessage(add.error) : null} />
      </div>
    </Card>
  );
}

// --- Shared fields ---

function ProductFieldsForm({ value, onChange, errors = {} }: { value: ProductFields; onChange: (v: ProductFields) => void; errors?: Record<string, string> }) {
  const { categories, brands } = useCatalogOptions();
  const set = <K extends keyof ProductFields>(key: K, v: ProductFields[K]) => onChange({ ...value, [key]: v });
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <TextField label="نام کالا" required value={value.name} onChange={(e) => set("name", e.target.value)} error={errors.name ?? errors["product.name"]} />
      <TextField label="نام لاتین" ltr value={value.nameEn}
        onChange={(e) => onChange({ ...value, nameEn: e.target.value, slug: value.slug || slugify(e.target.value) })} error={errors.nameEn} />
      <TextField label="نامک (در نشانی صفحه)" ltr required value={value.slug} onChange={(e) => set("slug", e.target.value)}
        hint="حروف کوچک انگلیسی، عدد و خط تیره" error={errors.slug ?? errors["product.slug"]} />
      <SelectField label="دسته‌بندی" required value={value.categoryId ?? ""} onChange={(e) => set("categoryId", e.target.value ? Number(e.target.value) : null)}
        error={errors.categoryId ?? errors["product.categoryId"]}>
        <option value="">انتخاب کنید</option>
        {categories.map((c) => <option key={c.id} value={c.id}>{c.label}</option>)}
      </SelectField>
      <SelectField label="برند" value={value.brandId ?? ""} onChange={(e) => set("brandId", e.target.value ? Number(e.target.value) : null)}>
        <option value="">بدون برند</option>
        {brands.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
      </SelectField>
      <div className="flex items-end pb-2">
        <Checkbox label="کالا در فروشگاه نمایش داده شود" checked={value.active} onChange={(e) => set("active", e.target.checked)} />
      </div>
      <TextArea label="معرفی" className="sm:col-span-2" rows={5} value={value.description} onChange={(e) => set("description", e.target.value)} />
    </div>
  );
}

function VariantFieldsForm({ value, onChange, errors = {} }: { value: VariantFields; onChange: (v: VariantFields) => void; errors?: Record<string, string> }) {
  const set = <K extends keyof VariantFields>(key: K, v: VariantFields[K]) => onChange({ ...value, [key]: v });
  const setAttribute = (index: number, change: Partial<{ key: string; value: string }>) =>
    set("attributes", value.attributes.map((a, i) => (i === index ? { ...a, ...change } : a)));
  return (
    <div className="grid gap-4 sm:grid-cols-3">
      <TextField label="کد انبار (SKU)" ltr required value={value.sku} onChange={(e) => set("sku", e.target.value)} error={errors.sku} />
      <TextField label="قیمت فروش (تومان)" inputMode="numeric" required value={value.price} onChange={(e) => set("price", e.target.value)} error={errors.price} />
      <TextField label="قیمت قبل از تخفیف (تومان)" inputMode="numeric" value={value.compareAtPrice} onChange={(e) => set("compareAtPrice", e.target.value)}
        hint="خالی یعنی بدون تخفیف" />
      <TextField label="پایان پیشنهاد شگفت‌انگیز" type="datetime-local" ltr value={value.discountEndsAt} onChange={(e) => set("discountEndsAt", e.target.value)}
        hint="پس از این زمان، قیمت قبل از تخفیف اعمال می‌شود" />
      <TextField label="موجودی" type="number" min={0} required value={value.stock} onChange={(e) => set("stock", e.target.value)} error={errors.stock} />
      <div className="flex items-end pb-2">
        <Checkbox label="فعال" checked={value.active} onChange={(e) => set("active", e.target.checked)} />
      </div>
      <fieldset className="sm:col-span-3">
        <legend className="mb-2 text-sm font-medium text-neutral-700">ویژگی‌ها (مثلاً color: مشکی، size: M)</legend>
        <div className="flex flex-col gap-2">
          {value.attributes.map((attribute, index) => (
            <div key={index} className="flex gap-2">
              <input aria-label="کلید" dir="ltr" placeholder="color" value={attribute.key} onChange={(e) => setAttribute(index, { key: e.target.value })}
                className="h-10 w-40 rounded-lg border border-neutral-300 px-3 text-sm" />
              <input aria-label="مقدار" placeholder="مشکی" value={attribute.value} onChange={(e) => setAttribute(index, { value: e.target.value })}
                className="h-10 flex-1 rounded-lg border border-neutral-300 px-3 text-sm" />
              <button type="button" aria-label="حذف ویژگی" onClick={() => set("attributes", value.attributes.filter((_, i) => i !== index))}
                className="flex size-10 items-center justify-center rounded-lg text-danger-600 hover:bg-red-50">
                <Trash2 className="size-4" />
              </button>
            </div>
          ))}
          <Button size="sm" variant="ghost" className="w-fit" onClick={() => set("attributes", [...value.attributes, { key: "", value: "" }])}>
            <Plus className="size-4" />افزودن ویژگی
          </Button>
        </div>
      </fieldset>
    </div>
  );
}

function BackLink() {
  return (
    <Link href="/admin/products" className="flex w-fit items-center gap-1 text-sm text-neutral-500 hover:text-neutral-800">
      <ArrowRight className="size-4" />
      بازگشت به کالاها
    </Link>
  );
}
