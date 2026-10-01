import type { Problem } from "./types";

/** An API failure. `detail` is a Persian sentence that can be shown to the user as is. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly errors: Record<string, string>;
  readonly retryAfterSeconds?: number;

  constructor(problem: Problem) {
    super(problem.detail);
    this.status = problem.status;
    this.code = problem.code;
    this.errors = problem.errors ?? {};
    this.retryAfterSeconds = problem.retryAfterSeconds;
  }

  static async from(response: Response): Promise<ApiError> {
    try {
      const body = (await response.json()) as Partial<Problem>;
      return new ApiError({
        status: response.status,
        code: body.code ?? "REQUEST_FAILED",
        detail: body.detail ?? FALLBACK_MESSAGE,
        errors: body.errors,
        retryAfterSeconds: body.retryAfterSeconds,
      });
    } catch {
      return new ApiError({ status: response.status, code: "REQUEST_FAILED", detail: FALLBACK_MESSAGE });
    }
  }
}

export const FALLBACK_MESSAGE = "ارتباط با سرور برقرار نشد. لطفاً دوباره تلاش کنید.";

export function errorMessage(error: unknown): string {
  return error instanceof ApiError ? error.message : FALLBACK_MESSAGE;
}
