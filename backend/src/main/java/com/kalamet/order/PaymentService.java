package com.kalamet.order;

import com.kalamet.common.ApiException;
import com.kalamet.config.KalametProperties;
import com.kalamet.order.OrderDtos.PayResponse;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Payment attempts. Starting a payment calls the gateway outside any transaction; the
 * callback verifies while holding the order lock, so a repeated or concurrent callback can
 * never verify twice or mark a cancelled order as paid.
 */
@Slf4j
@Service
public class PaymentService {

    /** How long a customer may stay on the gateway page; such orders are not expired meanwhile. */
    static final Duration GATEWAY_SESSION = Duration.ofMinutes(15);

    private final OrderService orderService;
    private final PaymentRepository payments;
    private final Map<PaymentGateway, PaymentGatewayClient> clients = new EnumMap<>(PaymentGateway.class);
    private final TransactionTemplate transaction;
    private final KalametProperties properties;
    private final Clock clock;

    PaymentService(OrderService orderService, PaymentRepository payments, List<PaymentGatewayClient> clients,
                   PlatformTransactionManager transactionManager, KalametProperties properties, Clock clock) {
        this.orderService = orderService;
        this.payments = payments;
        clients.forEach(client -> this.clients.put(client.gateway(), client));
        this.transaction = new TransactionTemplate(transactionManager);
        this.properties = properties;
        this.clock = clock;
    }

    public PayResponse start(Long userId, String orderNumber, PaymentGateway requested) {
        PaymentGatewayClient client = client(requested);

        // 1. Record the attempt.
        record Attempt(Long paymentId, long amount, String mobile) {
        }
        Attempt attempt = transaction.execute(status -> {
            Order order = orderService.lockOwned(userId, orderNumber);
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                throw ApiException.conflict("ORDER_NOT_PAYABLE", "این سفارش در انتظار پرداخت نیست.");
            }
            if (!clock.instant().isBefore(orderService.payableUntil(order))) {
                throw ApiException.conflict("ORDER_EXPIRED", "مهلت پرداخت این سفارش به پایان رسیده است.");
            }
            Payment payment = payments.save(new Payment(order, client.gateway(), clock.instant()));
            return new Attempt(payment.getId(), payment.getAmount(), order.getUser().getMobile());
        });

        // 2. Ask the gateway for a payment page (network call, no transaction held).
        PaymentGatewayClient.StartResult result;
        try {
            result = client.start(attempt.amount(), "پرداخت سفارش " + orderNumber + " در کالامت",
                    attempt.mobile(), callbackUrl());
        } catch (RuntimeException ex) {
            log.warn("Could not start {} payment for order {}", client.gateway(), orderNumber, ex);
            transaction.executeWithoutResult(status -> payments.findById(attempt.paymentId())
                    .ifPresent(payment -> payment.failed(ex.getMessage(), clock.instant())));
            throw new ApiException(HttpStatus.BAD_GATEWAY, "PAYMENT_GATEWAY_ERROR",
                    "اتصال به درگاه پرداخت ممکن نشد. لطفاً دوباره تلاش کنید.");
        }

        // 3. Remember the gateway's token for the callback.
        transaction.executeWithoutResult(status -> payments.findById(attempt.paymentId())
                .ifPresent(payment -> payment.started(result.authority(), clock.instant())));
        return new PayResponse(orderNumber, client.gateway(), result.authority(), result.paymentUrl());
    }

    /** Handles the customer's return from the gateway and returns the frontend page to redirect to. */
    public String handleCallback(String authority, String status) {
        if (authority == null || authority.isBlank()) {
            return resultUrl(null, false, null);
        }
        return transaction.execute(tx -> {
            Long orderId = payments.findOrderIdByAuthority(authority).orElse(null);
            if (orderId == null) {
                return resultUrl(null, false, null);
            }
            Order order = orderService.lockById(orderId);
            Payment payment = payments.findByAuthority(authority).orElseThrow();
            Instant now = clock.instant();

            if (payment.getStatus() != PaymentStatus.PENDING) {
                // Repeated callback: report what already happened.
                return resultUrl(order, payment.getStatus() == PaymentStatus.SUCCEEDED, payment.getRefId());
            }
            if (!"OK".equalsIgnoreCase(status)) {
                payment.failed("پرداخت توسط مشتری لغو شد یا ناموفق بود.", now);
                return resultUrl(order, false, null);
            }
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                // Not verifying makes the gateway return the money to the customer.
                payment.failed("سفارش دیگر در انتظار پرداخت نبود (" + order.getStatus() + ").", now);
                log.warn("Payment {} for order {} arrived in status {}; left unverified",
                        payment.getId(), order.getOrderNumber(), order.getStatus());
                return resultUrl(order, false, null);
            }

            PaymentGatewayClient.VerifyResult result;
            try {
                result = clients.get(payment.getGateway()).verify(authority, payment.getAmount());
            } catch (RuntimeException ex) {
                // Outcome unknown: leave the attempt PENDING. Unverified payments are reversed by
                // the gateway, and the expiry job closes the order if nothing else happens.
                log.error("Verifying payment {} of order {} failed", payment.getId(), order.getOrderNumber(), ex);
                return resultUrl(order, false, null);
            }
            if (!result.success()) {
                payment.failed(result.failureReason(), now);
                return resultUrl(order, false, null);
            }
            payment.succeeded(result.refId(), result.cardPan(), now);
            orderService.changeStatus(order, OrderStatus.PAID);
            log.info("Order {} paid via {} (ref {})", order.getOrderNumber(), payment.getGateway(), result.refId());
            return resultUrl(order, true, result.refId());
        });
    }

    /** The mock gateway's payment page. */
    public String mockPage(String authority) {
        if (!clients.get(PaymentGateway.MOCK).enabled()) {
            throw ApiException.notFound("MOCK_DISABLED", "درگاه آزمایشی غیرفعال است.");
        }
        record PageData(String orderNumber, long amount) {
        }
        PageData data = transaction.execute(tx -> payments.findByAuthority(authority)
                .filter(p -> p.getGateway() == PaymentGateway.MOCK && p.getStatus() == PaymentStatus.PENDING)
                .map(p -> new PageData(p.getOrder().getOrderNumber(), p.getAmount()))
                .orElseThrow(() -> ApiException.notFound("PAYMENT_NOT_FOUND", "تراکنش پیدا نشد یا قبلاً انجام شده است.")));

        String base = callbackUrl();
        String pay = UriComponentsBuilder.fromUriString(base)
                .queryParam("Authority", authority).queryParam("Status", "OK").toUriString();
        String cancel = UriComponentsBuilder.fromUriString(base)
                .queryParam("Authority", authority).queryParam("Status", "NOK").toUriString();
        String toman = NumberFormat.getIntegerInstance(Locale.forLanguageTag("fa-IR")).format(data.amount() / 10);
        return """
                <!doctype html>
                <html lang="fa" dir="rtl">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>درگاه پرداخت آزمایشی</title>
                <style>
                  body { font-family: Vazirmatn, Tahoma, sans-serif; background: #f5f5f5; margin: 0;
                         display: grid; place-items: center; min-height: 100vh; color: #222; }
                  main { background: #fff; border-radius: 12px; padding: 32px; width: min(360px, 90vw);
                         box-shadow: 0 2px 12px rgba(0,0,0,.08); text-align: center; }
                  .amount { font-size: 1.6rem; font-weight: bold; margin: 16px 0; }
                  a { display: block; padding: 12px; border-radius: 8px; margin-top: 12px; text-decoration: none; }
                  .pay { background: #19bfd3; color: #fff; }
                  .cancel { border: 1px solid #ccc; color: #555; }
                  small { color: #888; }
                </style>
                </head>
                <body>
                <main>
                  <h1>درگاه پرداخت آزمایشی</h1>
                  <p>سفارش %s</p>
                  <p class="amount">%s تومان</p>
                  <a class="pay" href="%s">پرداخت موفق</a>
                  <a class="cancel" href="%s">انصراف از پرداخت</a>
                  <p><small>این صفحه فقط برای آزمایش است و پولی جابه‌جا نمی‌شود.</small></p>
                </main>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(data.orderNumber()), toman,
                HtmlUtils.htmlEscape(pay), HtmlUtils.htmlEscape(cancel));
    }

    /** Must run on a request thread when no callback URL is configured. */
    private String callbackUrl() {
        String configured = properties.payment().callbackUrl();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/payments/callback").toUriString();
    }

    private PaymentGatewayClient client(PaymentGateway requested) {
        PaymentGateway gateway = requested;
        if (gateway == null) {
            gateway = clients.get(PaymentGateway.ZARINPAL).enabled() ? PaymentGateway.ZARINPAL : PaymentGateway.MOCK;
        }
        PaymentGatewayClient client = clients.get(gateway);
        if (client == null || !client.enabled()) {
            throw ApiException.badRequest("GATEWAY_UNAVAILABLE", "این درگاه پرداخت در دسترس نیست.");
        }
        return client;
    }

    private String resultUrl(Order order, boolean success, String refId) {
        UriComponentsBuilder url = UriComponentsBuilder.fromUriString(properties.frontendUrl())
                .path("/checkout/result")
                .queryParam("status", success ? "success" : "failed");
        if (order != null) {
            url.queryParam("order", order.getOrderNumber());
        }
        if (refId != null) {
            url.queryParam("ref", refId);
        }
        return url.toUriString();
    }
}
