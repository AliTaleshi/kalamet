package com.kalamet.order;

import com.kalamet.cart.Cart;
import com.kalamet.cart.CartItem;
import com.kalamet.cart.CartDtos.CartView;
import com.kalamet.cart.CartService;
import com.kalamet.cart.ShippingPolicy;
import com.kalamet.catalog.PriceQuote;
import com.kalamet.catalog.ProductImage;
import com.kalamet.catalog.ProductImageRepository;
import com.kalamet.catalog.ProductVariant;
import com.kalamet.common.ApiException;
import com.kalamet.common.MobileNumber;
import com.kalamet.common.PageResponse;
import com.kalamet.common.PersianText;
import com.kalamet.config.KalametProperties;
import com.kalamet.order.OrderDtos.CustomerResponse;
import com.kalamet.order.OrderDtos.OrderLine;
import com.kalamet.order.OrderDtos.OrderResponse;
import com.kalamet.order.OrderDtos.OrderSummary;
import com.kalamet.order.OrderDtos.PaymentResponse;
import com.kalamet.order.OrderDtos.ShippingAddressResponse;
import com.kalamet.user.Address;
import com.kalamet.user.AddressService;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orders;
    private final CartService cartService;
    private final AddressService addressService;
    private final ShippingPolicy shipping;
    private final ProductImageRepository images;
    private final Duration paymentTimeout;
    private final Clock clock;

    OrderService(OrderRepository orders, CartService cartService, AddressService addressService,
                 ShippingPolicy shipping, ProductImageRepository images, KalametProperties properties, Clock clock) {
        this.orders = orders;
        this.cartService = cartService;
        this.addressService = addressService;
        this.shipping = shipping;
        this.images = images;
        this.paymentTimeout = properties.order().paymentTimeout();
        this.clock = clock;
    }

    /**
     * Turns the cart into an order at current prices and reserves the stock. The variant
     * version check stops two customers from buying the same last unit; the loser gets 409.
     */
    public OrderResponse checkout(Long userId, Long addressId, Long expectedPayable) {
        Instant now = clock.instant();
        Cart cart = cartService.lockedCart(userId);
        CartView current = cartService.view(cart);
        if (current.hasIssues()) {
            throw ApiException.conflict("CART_HAS_ISSUES",
                    "موجودی یا وضعیت برخی کالاهای سبد تغییر کرده است. لطفاً سبد خرید را بررسی کنید.");
        }
        if (expectedPayable != null && expectedPayable != current.payable()) {
            throw ApiException.conflict("PRICES_CHANGED",
                    "قیمت برخی کالاهای سبد تغییر کرده است. لطفاً مبلغ جدید را بررسی کنید.");
        }
        Address address = addressService.require(userId, addressId);

        Order order = new Order(cart.getUser(), newOrderNumber(), ShippingAddress.copyOf(address), now);
        for (CartItem item : cart.getItems()) {
            ProductVariant variant = item.getVariant();
            variant.takeStock(item.getQuantity());
            order.addItem(new OrderItem(order, variant, PriceQuote.of(variant, now), item.getQuantity()));
        }
        order.setShippingFee(shipping.fee(order.getItemsTotal()));
        if (order.getTotal() == 0) {
            // Nothing to pay (free items, free shipping): gateways and the payments table need an amount.
            order.moveTo(OrderStatus.PAID, now);
        }
        orders.saveAndFlush(order);
        cart.clear(now);
        log.info("Order {} created for user {} ({} Rial)", order.getOrderNumber(), userId, order.getTotal());
        return response(order, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> list(Long userId, Pageable pageable) {
        return summaries(orders.findByUserId(userId, pageable), false);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long userId, String orderNumber) {
        return response(require(userId, orderNumber), false);
    }

    /** Customers can cancel only while the order is unpaid. */
    public OrderResponse cancel(Long userId, String orderNumber) {
        Order order = lockOwned(userId, orderNumber);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw ApiException.conflict("ORDER_NOT_CANCELLABLE", "این سفارش دیگر قابل لغو نیست.");
        }
        failPendingPayments(order, "سفارش توسط مشتری لغو شد.");
        changeStatus(order, OrderStatus.CANCELLED);
        return response(order, false);
    }

    // --- Admin ---

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> adminSearch(OrderStatus status, String query, Pageable pageable) {
        String normalized = null;
        if (query != null && !query.isBlank()) {
            // A mobile in any format (Persian digits, +98...) or an order number.
            String mobile = MobileNumber.normalize(query);
            normalized = mobile != null ? mobile : PersianText.digitsToAscii(query.strip()).toUpperCase(Locale.ROOT);
        }
        return summaries(orders.adminSearch(status, normalized, pageable), true);
    }

    @Transactional(readOnly = true)
    public OrderResponse adminGet(String orderNumber) {
        return response(orders.findByOrderNumber(orderNumber).orElseThrow(OrderService::notFound), true);
    }

    /** Refunds are recorded only: the money itself is returned outside the system. */
    public OrderResponse adminChangeStatus(String orderNumber, OrderStatus target) {
        Order order = orders.lockByOrderNumber(orderNumber).orElseThrow(OrderService::notFound);
        if (!order.getStatus().adminTargets().contains(target)) {
            throw ApiException.conflict("INVALID_STATUS_CHANGE",
                    "تغییر وضعیت از " + order.getStatus() + " به " + target + " مجاز نیست.");
        }
        if (target == OrderStatus.CANCELLED) {
            failPendingPayments(order, "سفارش توسط مدیر لغو شد.");
        }
        if (target == OrderStatus.REFUNDED) {
            order.getPayments().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                    .forEach(p -> p.refunded(clock.instant()));
        }
        changeStatus(order, target);
        return response(order, true);
    }

    // --- Expiry ---

    /** Unpaid orders older than the payment timeout. */
    @Transactional(readOnly = true)
    public List<Long> expiredOrderIds() {
        return orders.findIdsByStatusCreatedBefore(OrderStatus.PENDING_PAYMENT, clock.instant().minus(paymentTimeout));
    }

    /**
     * Cancels one unpaid order and releases its stock, unless a payment attempt is still
     * recent (the customer may be on the gateway page right now).
     */
    public boolean expire(Long orderId) {
        Order order = orders.lockById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return false;
        }
        Instant now = clock.instant();
        boolean recentAttempt = order.getPayments().stream().anyMatch(p -> p.getStatus() == PaymentStatus.PENDING
                && p.getCreatedAt().isAfter(now.minus(PaymentService.GATEWAY_SESSION)));
        if (recentAttempt) {
            return false;
        }
        failPendingPayments(order, "مهلت پرداخت به پایان رسید.");
        changeStatus(order, OrderStatus.CANCELLED);
        log.info("Order {} cancelled: not paid within {}", order.getOrderNumber(), paymentTimeout);
        return true;
    }

    // --- Shared with PaymentService ---

    /** The user's order, locked and read fresh; see {@link OrderRepository#lockById}. */
    Order lockOwned(Long userId, String orderNumber) {
        return orders.lockByOrderNumber(orderNumber)
                .filter(order -> order.getUser().getId().equals(userId))
                .orElseThrow(OrderService::notFound);
    }

    Order lockById(Long orderId) {
        return orders.lockById(orderId).orElseThrow(OrderService::notFound);
    }

    Order require(Long userId, String orderNumber) {
        return orders.findByOrderNumberAndUserId(orderNumber, userId).orElseThrow(OrderService::notFound);
    }

    Instant payableUntil(Order order) {
        return order.getCreatedAt().plus(paymentTimeout);
    }

    void changeStatus(Order order, OrderStatus target) {
        if (order.getStatus().releasesStockTo(target)) {
            order.getItems().forEach(item -> item.getVariant().returnStock(item.getQuantity()));
        }
        order.moveTo(target, clock.instant());
    }

    OrderResponse response(Order order, boolean withCustomer) {
        Map<Long, String> mainImages = mainImages(order.getItems().stream()
                .map(item -> item.getVariant().getProduct().getId()).toList());
        List<OrderLine> lines = order.getItems().stream()
                .map(item -> new OrderLine(item.getVariant().getId(), item.getVariant().getProduct().getSlug(),
                        item.getProductName(), item.getSku(), item.getVariantAttributes(),
                        mainImages.get(item.getVariant().getProduct().getId()), item.getUnitPrice(),
                        item.getOriginalPrice(), item.getQuantity(), item.getLineTotal()))
                .toList();
        return new OrderResponse(order.getOrderNumber(), order.getStatus(), order.getCreatedAt(),
                order.getStatus() == OrderStatus.PENDING_PAYMENT ? payableUntil(order) : null,
                lines, order.getItemsTotal(), order.getDiscountTotal(), order.getShippingFee(), order.getTotal(),
                ShippingAddressResponse.of(order.getShippingAddress()),
                order.getPayments().stream().map(PaymentResponse::of).toList(),
                withCustomer ? customer(order) : null);
    }

    private void failPendingPayments(Order order, String reason) {
        order.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .forEach(p -> p.failed(reason, clock.instant()));
    }

    private PageResponse<OrderSummary> summaries(Page<Order> page, boolean withCustomer) {
        Map<Long, String> mainImages = mainImages(page.getContent().stream()
                .flatMap(order -> order.getItems().stream())
                .map(item -> item.getVariant().getProduct().getId())
                .distinct()
                .toList());
        return PageResponse.of(page, order -> new OrderSummary(order.getOrderNumber(), order.getStatus(),
                order.getTotal(), order.itemCount(),
                order.getItems().stream()
                        .map(item -> mainImages.get(item.getVariant().getProduct().getId()))
                        .filter(url -> url != null)
                        .distinct()
                        .limit(4)
                        .toList(),
                order.getCreatedAt(), withCustomer ? customer(order) : null));
    }

    private Map<Long, String> mainImages(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return images.findMainImages(productIds).stream()
                .collect(Collectors.toMap(image -> image.getProduct().getId(), ProductImage::getUrl, (a, b) -> a));
    }

    private static CustomerResponse customer(Order order) {
        return new CustomerResponse(order.getUser().getId(), order.getUser().getMobile(),
                order.getUser().displayName());
    }

    /** "KL-" and 8 random digits; retried on the rare collision. */
    private String newOrderNumber() {
        String number;
        do {
            number = "KL-" + (10_000_000 + RANDOM.nextInt(90_000_000));
        } while (orders.existsByOrderNumber(number));
        return number;
    }

    static ApiException notFound() {
        return ApiException.notFound("ORDER_NOT_FOUND", "سفارش پیدا نشد.");
    }
}
