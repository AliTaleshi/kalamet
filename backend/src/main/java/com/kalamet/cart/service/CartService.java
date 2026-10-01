package com.kalamet.cart.service;

import com.kalamet.cart.domain.Cart;
import com.kalamet.cart.domain.CartItem;
import com.kalamet.cart.dto.CartDtos.AddItemRequest;
import com.kalamet.cart.dto.CartDtos.CartLine;
import com.kalamet.cart.dto.CartDtos.CartView;
import com.kalamet.cart.dto.CartDtos.LineIssue;
import com.kalamet.cart.repository.CartRepository;
import com.kalamet.catalog.domain.PriceQuote;
import com.kalamet.catalog.domain.ProductVariant;
import com.kalamet.catalog.service.CatalogService;
import com.kalamet.common.error.ApiException;
import com.kalamet.common.text.PersianText;
import com.kalamet.config.KalametProperties;
import com.kalamet.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartService {

    /** Distinct variants per cart. */
    static final int MAX_LINES = 50;

    private final CartRepository carts;
    private final CatalogService catalog;
    private final UserService userService;
    private final ShippingPolicy shipping;
    private final int maxPerItem;
    private final Clock clock;

    CartService(CartRepository carts, CatalogService catalog, UserService userService, ShippingPolicy shipping,
                KalametProperties properties, Clock clock) {
        this.carts = carts;
        this.catalog = catalog;
        this.userService = userService;
        this.shipping = shipping;
        this.maxPerItem = properties.cart().maxQuantityPerItem();
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CartView view(Long userId) {
        return carts.findByUserId(userId).map(this::view).orElseGet(() -> view(List.of()));
    }

    public CartView add(Long userId, AddItemRequest request) {
        ProductVariant variant = sellableVariant(request.variantId());
        Cart cart = cartOf(userId);
        int current = cart.item(variant.getId()).map(CartItem::getQuantity).orElse(0);
        setQuantity(cart, variant, current + request.quantity(), true);
        return view(carts.saveAndFlush(cart));
    }

    public CartView updateQuantity(Long userId, Long variantId, int quantity) {
        Cart cart = carts.findByUserId(userId).orElseThrow(CartService::itemNotFound);
        CartItem item = cart.item(variantId).orElseThrow(CartService::itemNotFound);
        if (quantity == 0) {
            cart.remove(item, clock.instant());
        } else {
            setQuantity(cart, item.getVariant(), quantity, true);
        }
        return view(carts.saveAndFlush(cart));
    }

    public CartView remove(Long userId, Long variantId) {
        Cart cart = carts.findByUserId(userId).orElseThrow(CartService::itemNotFound);
        cart.remove(cart.item(variantId).orElseThrow(CartService::itemNotFound), clock.instant());
        return view(carts.saveAndFlush(cart));
    }

    public CartView clear(Long userId) {
        carts.findByUserId(userId).ifPresent(cart -> cart.clear(clock.instant()));
        return view(userId);
    }

    /**
     * Adds a guest cart after sign-in. Unavailable items are skipped and quantities capped, never
     * rejected. A variant already in the cart keeps the larger quantity, so merging twice is harmless.
     */
    public CartView merge(Long userId, List<AddItemRequest> items) {
        if (items.isEmpty()) {
            return view(userId);
        }
        Cart cart = cartOf(userId);
        Map<Long, ProductVariant> found = catalog.variants(items.stream().map(AddItemRequest::variantId).toList());
        for (AddItemRequest item : items) {
            ProductVariant variant = found.get(item.variantId());
            if (variant == null || !variant.sellable() || variant.getStock() <= 0) {
                continue;
            }
            int current = cart.item(variant.getId()).map(CartItem::getQuantity).orElse(0);
            setQuantity(cart, variant, Math.max(current, item.quantity()), false);
        }
        return view(carts.saveAndFlush(cart));
    }

    /** For checkout: the user's cart, locked for the rest of the transaction. */
    public Cart lockedCart(Long userId) {
        return carts.findWithLockByUserId(userId)
                .filter(cart -> !cart.getItems().isEmpty())
                .orElseThrow(() -> ApiException.badRequest("CART_EMPTY", "سبد خرید شما خالی است."));
    }

    public CartView view(Cart cart) {
        return view(cart.getItems());
    }

    private CartView view(List<CartItem> items) {
        if (items.isEmpty()) {
            return new CartView(List.of(), 0, 0, 0, 0, 0, 0, false);
        }
        Instant now = clock.instant();
        Map<Long, String> mainImages = catalog.mainImageUrls(items.stream()
                .map(item -> item.getVariant().getProduct().getId()).distinct().toList());

        List<CartLine> lines = new ArrayList<>();
        int itemCount = 0;
        long itemsTotal = 0;
        long discountTotal = 0;
        boolean hasIssues = false;
        for (CartItem item : items) {
            ProductVariant variant = item.getVariant();
            PriceQuote quote = PriceQuote.of(variant, now);
            LineIssue issue = issue(variant, item.getQuantity());
            if (issue == null) {
                itemCount += item.getQuantity();
                itemsTotal += quote.price() * item.getQuantity();
                discountTotal += quote.saving() * item.getQuantity();
            } else {
                hasIssues = true;
            }
            lines.add(new CartLine(variant.getId(), variant.getSku(), variant.getProduct().getSlug(),
                    variant.getProduct().getName(), mainImages.get(variant.getProduct().getId()),
                    variant.getAttributes(), item.getQuantity(), quote.price(), quote.originalPrice(),
                    quote.discountPercent(), quote.offerEndsAt(), quote.price() * item.getQuantity(),
                    maxQuantity(variant), issue));
        }
        long shippingFee = shipping.fee(itemsTotal);
        return new CartView(lines, itemCount, itemsTotal, discountTotal, shippingFee,
                itemsTotal == 0 ? 0 : shipping.remainingForFreeShipping(itemsTotal),
                itemsTotal + shippingFee, hasIssues);
    }

    static LineIssue issue(ProductVariant variant, int quantity) {
        if (!variant.sellable()) {
            return LineIssue.UNAVAILABLE;
        }
        if (variant.getStock() <= 0) {
            return LineIssue.OUT_OF_STOCK;
        }
        return quantity > variant.getStock() ? LineIssue.INSUFFICIENT_STOCK : null;
    }

    private int maxQuantity(ProductVariant variant) {
        return variant.sellable() ? Math.max(0, Math.min(variant.getStock(), maxPerItem)) : 0;
    }

    /** {@code strict}: reject a quantity above the limit instead of capping it. */
    private void setQuantity(Cart cart, ProductVariant variant, int quantity, boolean strict) {
        int max = maxQuantity(variant);
        if (quantity > max) {
            if (strict) {
                throw ApiException.conflict("QUANTITY_LIMIT", max == 0
                        ? "این کالا در حال حاضر موجود نیست."
                        : "حداکثر " + PersianText.digits(max) + " عدد از این کالا را می‌توانید سفارش دهید.");
            }
            quantity = max;
        }
        Instant now = clock.instant();
        int finalQuantity = quantity;
        CartItem existing = cart.item(variant.getId()).orElse(null);
        if (existing != null) {
            existing.setQuantity(finalQuantity);
            cart.touch(now);
            return;
        }
        if (cart.getItems().size() >= MAX_LINES) {
            if (!strict) {
                return;
            }
            throw ApiException.conflict("CART_FULL",
                    "سبد خرید حداکثر " + PersianText.digits(MAX_LINES) + " کالای مختلف می‌پذیرد.");
        }
        cart.add(variant, finalQuantity, now);
    }

    private ProductVariant sellableVariant(Long variantId) {
        ProductVariant variant = catalog.variant(variantId)
                .orElseThrow(() -> ApiException.notFound("VARIANT_NOT_FOUND", "کالا پیدا نشد."));
        if (!variant.sellable()) {
            throw ApiException.conflict("VARIANT_UNAVAILABLE", "این کالا در حال حاضر فروخته نمی‌شود.");
        }
        return variant;
    }

    private Cart cartOf(Long userId) {
        return carts.findByUserId(userId)
                .orElseGet(() -> carts.save(new Cart(userService.require(userId), clock.instant())));
    }

    private static ApiException itemNotFound() {
        return ApiException.notFound("CART_ITEM_NOT_FOUND", "این کالا در سبد خرید شما نیست.");
    }
}
