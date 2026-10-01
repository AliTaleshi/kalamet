import { Headphones, ShieldCheck, Truck, Undo2 } from "lucide-react";
import Link from "next/link";
import { Logo } from "./logo";

const PROMISES = [
  { icon: Truck, title: "ارسال سریع", text: "تحویل اکسپرس در تهران" },
  { icon: ShieldCheck, title: "پرداخت امن", text: "درگاه‌های معتبر بانکی" },
  { icon: Undo2, title: "۷ روز ضمانت بازگشت", text: "بدون پرسش" },
  { icon: Headphones, title: "پشتیبانی", text: "هر روز هفته" },
];

export function Footer() {
  return (
    <footer className="mt-16 border-t border-neutral-200 bg-white">
      <div className="mx-auto max-w-7xl px-4 py-10">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <Logo />
          <p className="text-sm text-neutral-500">کالامت؛ خرید آنلاین با خیال راحت</p>
        </div>
        <ul className="mt-8 grid grid-cols-2 gap-4 md:grid-cols-4">
          {PROMISES.map(({ icon: Icon, title, text }) => (
            <li key={title} className="flex items-center gap-3">
              <span className="flex size-12 items-center justify-center rounded-full bg-brand-50 text-brand-600">
                <Icon className="size-6" />
              </span>
              <div>
                <p className="text-sm font-bold text-neutral-800">{title}</p>
                <p className="text-xs text-neutral-500">{text}</p>
              </div>
            </li>
          ))}
        </ul>
        <div className="mt-10 grid gap-8 text-sm text-neutral-600 sm:grid-cols-3">
          <FooterLinks title="با کالامت" links={[["درباره ما", "/"], ["فرصت‌های شغلی", "/"], ["تماس با ما", "/"]]} />
          <FooterLinks title="خدمات مشتریان" links={[["پیگیری سفارش", "/profile/orders"], ["رویه بازگرداندن کالا", "/"], ["پرسش‌های متداول", "/"]]} />
          <FooterLinks title="راهنمای خرید" links={[["نحوه ثبت سفارش", "/"], ["شیوه‌های پرداخت", "/"], ["شیوه‌های ارسال", "/"]]} />
        </div>
      </div>
      <p className="border-t border-neutral-100 py-4 text-center text-xs text-neutral-400">
        پروژه نمونه کار کالامت؛ همه نام‌ها و برندها ساختگی هستند.
      </p>
    </footer>
  );
}

function FooterLinks({ title, links }: { title: string; links: [string, string][] }) {
  return (
    <div>
      <h3 className="mb-3 font-bold text-neutral-800">{title}</h3>
      <ul className="space-y-2">
        {links.map(([label, href]) => (
          <li key={label}>
            <Link href={href} className="hover:text-brand-700">{label}</Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
