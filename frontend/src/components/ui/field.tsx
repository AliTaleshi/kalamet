import { clsx } from "clsx";
import { useId } from "react";

const CONTROL =
  "w-full rounded-lg border bg-white px-3 text-sm text-neutral-800 placeholder:text-neutral-400 transition-colors focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-100 disabled:bg-neutral-100";

type FieldProps = { label?: string; error?: string; hint?: string; className?: string };

function Wrapper({ id, label, error, hint, className, children }: FieldProps & { id: string; children: React.ReactNode }) {
  return (
    <div className={clsx("flex flex-col gap-1.5", className)}>
      {label && (
        <label htmlFor={id} className="text-sm font-medium text-neutral-700">
          {label}
        </label>
      )}
      {children}
      {error ? (
        <p id={`${id}-error`} className="text-xs text-danger-600">
          {error}
        </p>
      ) : (
        hint && <p className="text-xs text-neutral-500">{hint}</p>
      )}
    </div>
  );
}

export function TextField({
  label,
  error,
  hint,
  className,
  ltr = false,
  ...input
}: FieldProps & { ltr?: boolean } & React.InputHTMLAttributes<HTMLInputElement>) {
  const id = useId();
  return (
    <Wrapper id={id} label={label} error={error} hint={hint} className={className}>
      <input
        id={id}
        dir={ltr ? "ltr" : undefined}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        className={clsx(CONTROL, "h-11", ltr && "text-left", error ? "border-danger-500" : "border-neutral-300")}
        {...input}
      />
    </Wrapper>
  );
}

export function TextArea({ label, error, hint, className, ...input }: FieldProps & React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const id = useId();
  return (
    <Wrapper id={id} label={label} error={error} hint={hint} className={className}>
      <textarea
        id={id}
        aria-invalid={error ? true : undefined}
        className={clsx(CONTROL, "min-h-24 py-2.5", error ? "border-danger-500" : "border-neutral-300")}
        {...input}
      />
    </Wrapper>
  );
}

export function SelectField({
  label,
  error,
  hint,
  className,
  children,
  ...select
}: FieldProps & React.SelectHTMLAttributes<HTMLSelectElement>) {
  const id = useId();
  return (
    <Wrapper id={id} label={label} error={error} hint={hint} className={className}>
      <select
        id={id}
        aria-invalid={error ? true : undefined}
        className={clsx(CONTROL, "h-11", error ? "border-danger-500" : "border-neutral-300")}
        {...select}
      >
        {children}
      </select>
    </Wrapper>
  );
}

export function Checkbox({ label, ...input }: { label: string } & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="inline-flex cursor-pointer items-center gap-2 text-sm text-neutral-700">
      <input type="checkbox" className="size-4 rounded border-neutral-300 accent-brand-600" {...input} />
      {label}
    </label>
  );
}

/** Error banner for failures that are not about one field. */
export function FormError({ message }: { message?: string | null }) {
  if (!message) return null;
  return (
    <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-danger-600">
      {message}
    </p>
  );
}
