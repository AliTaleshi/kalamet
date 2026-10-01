import { clsx } from "clsx";
import Link from "next/link";
import { Spinner } from "./spinner";

type Variant = "primary" | "secondary" | "outline" | "ghost" | "danger";
type Size = "sm" | "md" | "lg";

const VARIANTS: Record<Variant, string> = {
  primary: "bg-brand-600 text-white hover:bg-brand-700 disabled:bg-brand-300",
  secondary: "bg-accent-500 text-neutral-900 hover:bg-accent-400 disabled:bg-accent-200",
  outline: "border border-brand-600 text-brand-700 hover:bg-brand-50 disabled:border-neutral-300 disabled:text-neutral-400",
  ghost: "text-neutral-700 hover:bg-neutral-100 disabled:text-neutral-400",
  danger: "bg-danger-500 text-white hover:bg-danger-600 disabled:opacity-50",
};
const SIZES: Record<Size, string> = {
  sm: "h-8 px-3 text-sm gap-1.5",
  md: "h-10 px-4 text-sm gap-2",
  lg: "h-12 px-6 text-base gap-2",
};

type Common = { variant?: Variant; size?: Size; className?: string; children: React.ReactNode };

export function buttonClass({ variant = "primary", size = "md", className }: Omit<Common, "children">) {
  return clsx(
    "inline-flex items-center justify-center rounded-lg font-medium transition-colors disabled:cursor-not-allowed",
    VARIANTS[variant],
    SIZES[size],
    className,
  );
}

export function Button({
  variant,
  size,
  className,
  loading = false,
  disabled,
  children,
  type = "button",
  ...rest
}: Common & { loading?: boolean } & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button type={type} disabled={disabled || loading} className={buttonClass({ variant, size, className })} {...rest}>
      {loading && <Spinner className="size-4" />}
      {children}
    </button>
  );
}

export function ButtonLink({ variant, size, className, href, children }: Common & { href: string }) {
  return (
    <Link href={href} className={buttonClass({ variant, size, className })}>
      {children}
    </Link>
  );
}
