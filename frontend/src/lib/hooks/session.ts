"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api/client";
import type { Profile } from "@/lib/api/types";

export const ME_KEY = ["me"] as const;

/** The signed-in user; null when signed out. */
export function useMe() {
  return useQuery({
    queryKey: ME_KEY,
    queryFn: async () => (await api<{ user: Profile | null }>("session")).user,
    staleTime: 5 * 60_000,
  });
}

export function useSignOut() {
  const client = useQueryClient();
  const router = useRouter();
  return useMutation({
    mutationFn: () => api<void>("auth/logout", { method: "POST" }),
    onSettled: () => {
      client.clear();
      client.setQueryData(ME_KEY, null);
      router.push("/");
      router.refresh();
    },
  });
}

export function displayName(user: Profile): string {
  const name = [user.firstName, user.lastName].filter(Boolean).join(" ");
  return name || "کاربر کالامت";
}
