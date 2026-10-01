import { existsSync } from "node:fs";
import { request, type FullConfig } from "@playwright/test";
import { ADMIN_MOBILE, ADMIN_STATE, signInWithApi } from "./helpers";

/**
 * Signs the admin in once for the whole run and saves the cookies. The API allows one login code
 * per number every two minutes, so a still-valid session from an earlier run is reused.
 */
export default async function globalSetup(config: FullConfig) {
  const baseURL = config.projects[0].use.baseURL;
  if (existsSync(ADMIN_STATE)) {
    const saved = await request.newContext({ baseURL, storageState: ADMIN_STATE });
    const session = await (await saved.get("/bff/session")).json().catch(() => ({ user: null }));
    if (session.user?.role === "ADMIN") {
      await saved.storageState({ path: ADMIN_STATE });   // keep cookies a refresh may have renewed
      await saved.dispose();
      return;
    }
    await saved.dispose();
  }
  const context = await request.newContext({ baseURL });
  await signInWithApi(context, ADMIN_MOBILE);
  await context.storageState({ path: ADMIN_STATE });
  await context.dispose();
}
