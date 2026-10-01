package com.kalamet.common.error;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 9457 problem response with a stable {@code code} property.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String RETRY_MESSAGE = "اطلاعات هم‌زمان تغییر کرد. لطفاً دوباره تلاش کنید.";

    /** Database constraints that users can trip, mapped to messages they can understand. */
    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.ofEntries(
            Map.entry("uk_users_email", "این ایمیل قبلاً ثبت شده است."),
            // Two requests of the same user at once (double click, two tabs): a retry succeeds.
            Map.entry("uk_carts_user", RETRY_MESSAGE),
            Map.entry("uk_cart_items_cart_variant", RETRY_MESSAGE),
            Map.entry("uk_addresses_one_default", RETRY_MESSAGE),
            Map.entry("uk_categories_slug", "دسته‌بندی دیگری با این نامک وجود دارد."),
            Map.entry("uk_brands_slug", "برند دیگری با این نامک وجود دارد."),
            Map.entry("uk_products_slug", "کالای دیگری با این نامک وجود دارد."),
            Map.entry("uk_variants_sku", "تنوع دیگری با این کد انبار (SKU) وجود دارد."),
            Map.entry("uk_variants_product_attributes", "این کالا تنوعی با همین ویژگی‌ها دارد."),
            Map.entry("uk_reviews_user_product", "شما قبلاً برای این کالا دیدگاه ثبت کرده‌اید."),
            Map.entry("fk_categories_parent", "این دسته‌بندی زیرمجموعه دارد و حذف نمی‌شود."),
            Map.entry("fk_products_category", "این دسته‌بندی کالا دارد و حذف نمی‌شود."),
            Map.entry("fk_order_items_variant", "این تنوع در سفارش‌ها استفاده شده و حذف نمی‌شود؛ آن را غیرفعال کنید."));

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
        ProblemDetail body = problem(ex.getStatus(), ex.getCode(), ex.getMessage());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(ex.getStatus());
        if (ex instanceof TooManyRequestsException tooMany) {
            body.setProperty("retryAfterSeconds", tooMany.getRetryAfterSeconds());
            response.header(HttpHeaders.RETRY_AFTER, String.valueOf(tooMany.getRetryAfterSeconds()));
        }
        return response.body(body);
    }

    /** Optimistic version conflicts, and lock timeouts or deadlocks reported by PostgreSQL. */
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    ResponseEntity<ProblemDetail> handleConcurrentUpdate(RuntimeException ex) {
        log.info("Concurrent update conflict: {}", ex.getMessage());
        return of(HttpStatus.CONFLICT, "CONCURRENT_UPDATE", RETRY_MESSAGE);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleIntegrity(DataIntegrityViolationException ex) {
        String constraint = ex.getCause() instanceof ConstraintViolationException cve ? cve.getConstraintName() : null;
        String message = constraint == null ? null : CONSTRAINT_MESSAGES.get(constraint);
        if (message == null) {
            log.warn("Unmapped data integrity violation (constraint {})", constraint, ex);
            message = "داده‌های ارسال‌شده با قوانین فروشگاه سازگار نیست.";
        }
        return of(HttpStatus.CONFLICT, RETRY_MESSAGE.equals(message) ? "CONCURRENT_UPDATE" : "CONSTRAINT_VIOLATION",
                message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return of(HttpStatus.FORBIDDEN, "FORBIDDEN", "دسترسی به این بخش مجاز نیست.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled error", ex);
        return of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "خطای غیرمنتظره‌ای رخ داد. لطفاً بعداً تلاش کنید.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "اطلاعات ارسال‌شده معتبر نیست.");
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "INVALID_BODY",
                "بدنه درخواست قابل خواندن نیست؛ قالب JSON و نوع فیلدها را بررسی کنید."));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : ex.getPropertyName();
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "مقدار پارامتر «" + name + "» معتبر نیست."));
    }

    /**
     * Other framework errors (unknown path, wrong method, missing parameter...) keep their status
     * but get a code and a Persian message, like every other error.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail detail && detail.getProperties() == null) {
            detail.setProperty("code", "REQUEST_REJECTED");
            detail.setDetail(switch (statusCode.value()) {
                case 404 -> "آدرس درخواست‌شده وجود ندارد.";
                case 405 -> "این روش درخواست برای این آدرس پشتیبانی نمی‌شود.";
                case 406, 415 -> "قالب درخواست یا پاسخ پشتیبانی نمی‌شود؛ از JSON استفاده کنید.";
                case 413 -> "حجم درخواست بیش از حد مجاز است.";
                default -> statusCode.is5xxServerError()
                        ? "خطای غیرمنتظره‌ای رخ داد. لطفاً بعداً تلاش کنید."
                        : "درخواست نامعتبر است.";
            });
        }
        return response;
    }

    private static ResponseEntity<ProblemDetail> of(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(problem(status, code, message));
    }

    private static ProblemDetail problem(HttpStatusCode status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setProperty("code", code);
        return detail;
    }
}
