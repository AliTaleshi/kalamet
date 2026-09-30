package com.kalamet.order;

import com.kalamet.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A frozen record of a purchase. Amounts in Rial; total = itemsTotal + shippingFee (a DB check). */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(nullable = false, updatable = false, length = 20)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(nullable = false)
    private long itemsTotal;

    @Column(nullable = false)
    private long discountTotal;

    @Column(nullable = false)
    private long shippingFee;

    @Column(nullable = false)
    private long total;

    @Embedded
    private ShippingAddress shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order")
    @OrderBy("createdAt DESC, id DESC")
    private List<Payment> payments = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    Order(User user, String orderNumber, ShippingAddress shippingAddress, Instant now) {
        this.user = user;
        this.orderNumber = orderNumber;
        this.shippingAddress = shippingAddress;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void addItem(OrderItem item) {
        items.add(item);
        itemsTotal += item.getLineTotal();
        if (item.getOriginalPrice() != null) {
            discountTotal += (item.getOriginalPrice() - item.getUnitPrice()) * item.getQuantity();
        }
    }

    void setShippingFee(long shippingFee) {
        this.shippingFee = shippingFee;
        this.total = itemsTotal + shippingFee;
    }

    void moveTo(OrderStatus target, Instant now) {
        this.status = target;
        this.updatedAt = now;
    }

    public int itemCount() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }
}
