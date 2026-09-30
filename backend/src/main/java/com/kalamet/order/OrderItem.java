package com.kalamet.order;

import com.kalamet.catalog.PriceQuote;
import com.kalamet.catalog.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A purchased line. Name, SKU, attributes and prices are copies taken at checkout. */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false, updatable = false)
    private ProductVariant variant;

    @Column(nullable = false, updatable = false, length = 250)
    private String productName;

    @Column(nullable = false, updatable = false, length = 64)
    private String sku;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, String> variantAttributes = new LinkedHashMap<>();

    @Column(nullable = false, updatable = false)
    private long unitPrice;

    @Column(updatable = false)
    private Long originalPrice;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(nullable = false, updatable = false)
    private long lineTotal;

    OrderItem(Order order, ProductVariant variant, PriceQuote quote, int quantity) {
        this.order = order;
        this.variant = variant;
        this.productName = variant.getProduct().getName();
        this.sku = variant.getSku();
        this.variantAttributes = new LinkedHashMap<>(variant.getAttributes());
        this.unitPrice = quote.price();
        this.originalPrice = quote.originalPrice();
        this.quantity = quantity;
        this.lineTotal = quote.price() * quantity;
    }
}
