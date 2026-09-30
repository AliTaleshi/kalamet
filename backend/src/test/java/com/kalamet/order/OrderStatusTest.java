package com.kalamet.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void paidOnlyThroughPayment() {
        assertThat(OrderStatus.PENDING_PAYMENT.adminTargets()).containsExactly(OrderStatus.CANCELLED);
    }

    @Test
    void fulfilmentMovesForward() {
        assertThat(OrderStatus.PAID.adminTargets()).containsExactlyInAnyOrder(OrderStatus.SHIPPED, OrderStatus.REFUNDED);
        assertThat(OrderStatus.SHIPPED.adminTargets()).contains(OrderStatus.DELIVERED);
        assertThat(OrderStatus.DELIVERED.adminTargets()).containsExactly(OrderStatus.REFUNDED);
    }

    @Test
    void finalStatesAreFinal() {
        assertThat(OrderStatus.CANCELLED.adminTargets()).isEmpty();
        assertThat(OrderStatus.REFUNDED.adminTargets()).isEmpty();
    }

    @Test
    void stockReturnsOnlyWhenAnUnshippedOrderCloses() {
        assertThat(OrderStatus.PENDING_PAYMENT.releasesStockTo(OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatus.PAID.releasesStockTo(OrderStatus.REFUNDED)).isTrue();
        assertThat(OrderStatus.PENDING_PAYMENT.releasesStockTo(OrderStatus.PAID)).isFalse();
        assertThat(OrderStatus.PAID.releasesStockTo(OrderStatus.SHIPPED)).isFalse();
        assertThat(OrderStatus.SHIPPED.releasesStockTo(OrderStatus.REFUNDED)).isFalse();
        assertThat(OrderStatus.DELIVERED.releasesStockTo(OrderStatus.REFUNDED)).isFalse();
    }
}
