package com.kalamet.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cancels unpaid orders after the payment timeout and puts their items back in stock. */
@Slf4j
@Component
@ConditionalOnProperty(name = "kalamet.jobs.enabled", havingValue = "true", matchIfMissing = true)
class OrderExpiryJob {

    private final OrderService orderService;

    OrderExpiryJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
    void cancelUnpaidOrders() {
        for (Long orderId : orderService.expiredOrderIds()) {
            try {
                orderService.expire(orderId);
            } catch (RuntimeException ex) {
                // For example a stock update racing a checkout; the next run retries.
                log.warn("Could not expire order {}: {}", orderId, ex.getMessage());
            }
        }
    }
}
