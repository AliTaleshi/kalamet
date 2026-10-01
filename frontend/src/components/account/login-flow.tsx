"use client";

import { useQueryClient } from "@tanstack/react-query";
import { ArrowRight, KeyRound } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/error";
import type { OtpResponse, Profile, SignInResult } from "@/lib/api/types";
import { faNumber, normalizeMobile } from "@/lib/format";
import { CART_KEY, mergeGuestCart } from "@/lib/hooks/cart";
import { ME_KEY } from "@/lib/hooks/session";
import { Logo } from "@/components/store/logo";
import { Button } from "@/components/ui/button";
import { FormError, TextField } from "@/components/ui/field";

type Step = { name: "mobile" } | { name: "code"; otp: OtpResponse; issued: number } | { name: "profile"; user: Profile };

/** Only same-site paths, so ?next= cannot send people to another website. */
function safeNext(value: string | null): string {
  return value && value.startsWith("/") && !value.startsWith("//") && !value.startsWith("/login") ? value : "/";
}

export function LoginFlow() {
  const router = useRouter();
  const params = useSearchParams();
  const client = useQueryClient();
  const next = safeNext(params.get("next"));
  const [step, setStep] = useState<Step>({ name: "mobile" });

  const finish = async () => {
    await mergeGuestCart().catch(() => undefined);   // a failed merge must not block sign-in
    await client.invalidateQueries({ queryKey: ME_KEY });
    await client.invalidateQueries({ queryKey: CART_KEY });
    router.replace(next);
    router.refresh();
  };

  return (
    <div className="w-full max-w-md rounded-3xl border border-neutral-200 bg-white p-8">
      <div className="mb-8 flex justify-center">
        <Logo />
      </div>
      {step.name === "mobile" && <MobileStep onSent={(otp) => setStep({ name: "code", otp, issued: Date.now() })} />}
      {step.name === "code" && (
        <CodeStep
          key={step.issued}
          otp={step.otp}
          onBack={() => setStep({ name: "mobile" })}
          onResent={(otp) => setStep({ name: "code", otp, issued: Date.now() })}
          onSignedIn={(result) => {
            client.setQueryData(ME_KEY, result.user);   // the header shows the account at once
            if (result.user.profileComplete) finish();
            else setStep({ name: "profile", user: result.user });
          }}
        />
      )}
      {step.name === "profile" && <ProfileStep user={step.user} onDone={finish} />}
    </div>
  );
}

function MobileStep({ onSent }: { onSent: (otp: OtpResponse) => void }) {
  const [mobile, setMobile] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  return (
    <form
      noValidate
      onSubmit={async (event) => {
        event.preventDefault();
        const normalized = normalizeMobile(mobile);
        if (!normalized) {
          setError("شماره موبایل معتبر نیست؛ مثلاً ۰۹۱۲۳۴۵۶۷۸۹");
          return;
        }
        setBusy(true);
        setError(null);
        try {
          onSent(await api<OtpResponse>("auth/otp", { method: "POST", body: { mobile: normalized } }));
        } catch (e) {
          setError(errorMessage(e));
        } finally {
          setBusy(false);
        }
      }}
      className="flex flex-col gap-5"
    >
      <div>
        <h1 className="text-lg font-black text-neutral-800">ورود | ثبت‌نام</h1>
        <p className="mt-2 text-sm text-neutral-500">سلام! لطفاً شماره موبایل خود را وارد کنید.</p>
      </div>
      <TextField
        label="شماره موبایل"
        type="tel"
        inputMode="tel"
        autoComplete="tel"
        autoFocus
        ltr
        placeholder="۰۹۱۲۳۴۵۶۷۸۹"
        value={mobile}
        onChange={(e) => setMobile(e.target.value)}
        error={error ?? undefined}
      />
      <Button type="submit" size="lg" loading={busy}>ادامه</Button>
      <p className="text-center text-xs leading-6 text-neutral-400">ورود شما به معنای پذیرش شرایط کالامت و قوانین حریم‌خصوصی است.</p>
    </form>
  );
}

function CodeStep({ otp, onBack, onResent, onSignedIn }: {
  otp: OtpResponse;
  onBack: () => void;
  onResent: (otp: OtpResponse) => void;
  onSignedIn: (result: SignInResult) => void;
}) {
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  // A new code remounts this step (see `key`), so the deadline starts fresh.
  const [resendAt, setResendAt] = useState(() => Date.now() + otp.resendInSeconds * 1000);
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  const resendIn = Math.max(0, Math.ceil((resendAt - now) / 1000));

  const verify = async (value: string) => {
    setBusy(true);
    setError(null);
    try {
      onSignedIn(await api<SignInResult>("auth/verify", { method: "POST", body: { mobile: otp.mobile, code: value } }));
    } catch (e) {
      setError(errorMessage(e));
      setBusy(false);
    }
  };

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        if (code.length >= 4) verify(code);
      }}
      className="flex flex-col gap-5"
    >
      <button type="button" onClick={onBack} className="flex w-fit items-center gap-1 text-sm text-neutral-500 hover:text-neutral-800">
        <ArrowRight className="size-4" />
        تغییر شماره
      </button>
      <div>
        <h1 className="text-lg font-black text-neutral-800">کد تأیید را وارد کنید</h1>
        <p className="mt-2 text-sm text-neutral-500">
          کد تأیید برای شماره <span dir="ltr" className="font-bold text-neutral-700">{faNumber(otp.mobile)}</span> پیامک شد.
        </p>
      </div>
      {otp.demoCode && (
        <p className="flex items-center gap-2 rounded-xl bg-accent-50 px-3 py-2 text-sm text-accent-700">
          <KeyRound className="size-4" />
          نسخه نمایشی: کد ورود <b dir="ltr">{faNumber(otp.demoCode)}</b> است.
        </p>
      )}
      <TextField
        label="کد تأیید"
        inputMode="numeric"
        autoComplete="one-time-code"
        autoFocus
        ltr
        maxLength={6}
        value={code}
        onChange={(e) => {
          const value = e.target.value.replace(/[۰-۹]/g, (d) => String(d.charCodeAt(0) - 0x06f0)).replace(/\D/g, "").slice(0, 6);
          setCode(value);
          if (value.length === 6 && !busy) verify(value);
        }}
        error={error ?? undefined}
        className="[&_input]:text-center [&_input]:text-xl [&_input]:tracking-[0.5em]"
      />
      <Button type="submit" size="lg" loading={busy} disabled={code.length < 4}>تأیید</Button>
      <div className="text-center text-sm">
        {resendIn > 0 ? (
          <span className="text-neutral-500">{faNumber(resendIn)} ثانیه تا ارسال مجدد کد</span>
        ) : (
          <button
            type="button"
            className="font-medium text-brand-700"
            onClick={async () => {
              try {
                onResent(await api<OtpResponse>("auth/otp", { method: "POST", body: { mobile: otp.mobile } }));
              } catch (e) {
                setError(errorMessage(e));
                if (e instanceof ApiError && e.retryAfterSeconds) setResendAt(Date.now() + e.retryAfterSeconds * 1000);
              }
            }}
          >
            ارسال مجدد کد
          </button>
        )}
      </div>
    </form>
  );
}

function ProfileStep({ user, onDone }: { user: Profile; onDone: () => void }) {
  const [firstName, setFirstName] = useState(user.firstName ?? "");
  const [lastName, setLastName] = useState(user.lastName ?? "");
  const [error, setError] = useState<ApiError | null>(null);
  const [busy, setBusy] = useState(false);
  return (
    <form
      onSubmit={async (event) => {
        event.preventDefault();
        setBusy(true);
        try {
          await api<Profile>("me", { method: "PUT", body: { firstName, lastName, email: user.email } });
          onDone();
        } catch (e) {
          setError(e instanceof ApiError ? e : null);
          setBusy(false);
        }
      }}
      className="flex flex-col gap-5"
    >
      <div>
        <h1 className="text-lg font-black text-neutral-800">به کالامت خوش آمدید!</h1>
        <p className="mt-2 text-sm text-neutral-500">برای ارسال سفارش‌ها، نام خود را وارد کنید.</p>
      </div>
      <TextField label="نام" autoComplete="given-name" autoFocus required value={firstName} onChange={(e) => setFirstName(e.target.value)} error={error?.errors.firstName} />
      <TextField label="نام خانوادگی" autoComplete="family-name" required value={lastName} onChange={(e) => setLastName(e.target.value)} error={error?.errors.lastName} />
      <FormError message={error && !Object.keys(error.errors).length ? error.message : null} />
      <div className="flex gap-3">
        <Button type="submit" size="lg" className="flex-1" loading={busy}>ذخیره و ادامه</Button>
        <Button variant="ghost" size="lg" onClick={onDone}>بعداً</Button>
      </div>
    </form>
  );
}
