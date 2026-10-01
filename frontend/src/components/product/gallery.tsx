"use client";

import { clsx } from "clsx";
import { useState } from "react";
import type { ProductImage as Image } from "@/lib/api/types";
import { ProductImage } from "@/components/ui/product-image";

export function Gallery({ images, name, initialIndex = 0 }: { images: Image[]; name: string; initialIndex?: number }) {
  const [active, setActive] = useState(initialIndex);
  const current = images[active] ?? null;
  return (
    <div className="flex flex-col gap-3">
      <ProductImage src={current?.url ?? null} alt={current?.altText ?? name} className="aspect-square w-full rounded-2xl" />
      {images.length > 1 && (
        <ul className="flex gap-2">
          {images.map((image, index) => (
            <li key={image.id}>
              <button
                type="button"
                aria-label={`تصویر ${index + 1}`}
                aria-current={index === active}
                onClick={() => setActive(index)}
                className={clsx("overflow-hidden rounded-lg border-2", index === active ? "border-brand-500" : "border-transparent")}
              >
                <ProductImage src={image.url} alt={image.altText ?? name} className="size-16" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
