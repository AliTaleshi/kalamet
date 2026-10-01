import { PageSpinner } from "@/components/ui/spinner";

/** Search results stream in (only here: elsewhere streaming would turn 404s into 200s). */
export default function Loading() {
  return <PageSpinner />;
}
