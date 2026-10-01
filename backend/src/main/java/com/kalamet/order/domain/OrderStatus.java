package com.kalamet.order.domain;

import java.util.Set;

/**
 * Order lifecycle. PENDING_PAYMENT -> PAID happens only through a verified payment; the
 * other moves are made by admins (or by the customer / expiry job for CANCELLED).
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    /** Transitions an admin may request. */
    public Set<OrderStatus> adminTargets() {
        return switch (this) {
            case PENDING_PAYMENT -> Set.of(CANCELLED);
            case PAID -> Set.of(SHIPPED, REFUNDED);
            case SHIPPED -> Set.of(DELIVERED, REFUNDED);
            case DELIVERED -> Set.of(REFUNDED);
            case CANCELLED, REFUNDED -> Set.of();
        };
    }

    /** The goods are still in the warehouse. */
    public boolean beforeShipping() {
        return this == PENDING_PAYMENT || this == PAID;
    }

    public boolean closed() {
        return this == CANCELLED || this == REFUNDED;
    }

    /** Cancelling or refunding an order that has not shipped puts its items back into stock. */
    public boolean releasesStockTo(OrderStatus target) {
        return beforeShipping() && target.closed();
    }
}
