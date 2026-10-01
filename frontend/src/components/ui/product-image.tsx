/* eslint-disable @next/next/no-img-element -- product images are arbitrary admin-provided URLs, so
   next/image would have to proxy any host; a lazy <img> avoids turning the server into an open proxy. */
import { clsx } from "clsx";
import { ImageOff } from "lucide-react";

export function ProductImage({ src, alt, className }: { src: string | null; alt: string; className?: string }) {
  if (!src) {
    return (
      <div className={clsx("flex items-center justify-center bg-neutral-100 text-neutral-300", className)}>
        <ImageOff className="size-1/4" />
      </div>
    );
  }
  return <img src={src} alt={alt} loading="lazy" decoding="async" className={clsx("object-cover", className)} />;
}
