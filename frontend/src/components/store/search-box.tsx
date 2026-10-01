"use client";

import { Search } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

export function SearchBox() {
  const router = useRouter();
  const params = useSearchParams();
  const [text, setText] = useState(params.get("q") ?? "");
  return (
    <form
      role="search"
      onSubmit={(event) => {
        event.preventDefault();
        const q = text.trim();
        router.push(q ? `/search?q=${encodeURIComponent(q)}` : "/search");
      }}
      className="flex h-11 w-full items-center gap-2 rounded-xl bg-neutral-100 px-3 focus-within:bg-white focus-within:ring-2 focus-within:ring-brand-200"
    >
      <Search className="size-5 shrink-0 text-neutral-400" />
      <input
        type="search"
        value={text}
        onChange={(event) => setText(event.target.value)}
        placeholder="جستجو در کالامت"
        aria-label="جستجو"
        maxLength={200}
        className="h-full w-full bg-transparent text-sm outline-none placeholder:text-neutral-400"
      />
    </form>
  );
}
