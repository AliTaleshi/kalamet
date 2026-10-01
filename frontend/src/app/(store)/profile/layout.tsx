import { ProfileNav } from "@/components/account/profile-nav";

export default function ProfileLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid items-start gap-4 lg:grid-cols-[16rem_1fr]">
      <ProfileNav />
      <div className="min-w-0">{children}</div>
    </div>
  );
}
