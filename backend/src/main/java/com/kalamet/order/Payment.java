package com.kalamet.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One payment attempt. Zarinpal flow: request -> authority -> customer pays on the gateway ->
 * callback -> verify -> ref_id. A failed attempt followed by a retry is two rows.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PaymentGateway gateway;

    private String authority;

    private String refId;

    private String cardPan;

    @Column(nullable = false, updatable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    private String failureReason;

    private Instant paidAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    Payment(Order order, PaymentGateway gateway, Instant now) {
        this.order = order;
        this.gateway = gateway;
        this.amount = order.getTotal();
        this.createdAt = now;
        this.updatedAt = now;
    }

    void started(String authority, Instant now) {
        this.authority = authority;
        this.updatedAt = now;
    }

    void succeeded(String refId, String cardPan, Instant now) {
        this.status = PaymentStatus.SUCCEEDED;
        this.refId = refId;
        this.cardPan = cardPan;
        this.paidAt = now;
        this.updatedAt = now;
    }

    void failed(String reason, Instant now) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason == null || reason.length() <= 255 ? reason : reason.substring(0, 255);
        this.updatedAt = now;
    }

    void refunded(Instant now) {
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = now;
    }
}
