package com.kalamet.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * The sellable unit: its own SKU, price and stock. {@code attributes} holds the options
 * that tell variants apart, with English keys and Persian values ({"color": "مشکی"}).
 */
@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(nullable = false, length = 64)
    private String sku;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> attributes = new LinkedHashMap<>();

    /** Selling price in Rial. */
    @Column(nullable = false)
    private long price;

    /** Crossed-out price; null means no discount. */
    private Long compareAtPrice;

    /** Set means an "amazing offer" that ends at this moment. */
    private Instant discountEndsAt;

    @Column(name = "stock_quantity", nullable = false)
    private int stock;

    @Column(nullable = false)
    private boolean active = true;

    /** Optimistic lock: two checkouts cannot both take the last unit. */
    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** A product can be bought only when both it and this variant are active. */
    public boolean sellable() {
        return active && product.isActive();
    }

    public void takeStock(int quantity) {
        if (quantity > stock) {
            throw new IllegalStateException("Not enough stock for " + sku);
        }
        stock -= quantity;
    }

    public void returnStock(int quantity) {
        stock += quantity;
    }
}
