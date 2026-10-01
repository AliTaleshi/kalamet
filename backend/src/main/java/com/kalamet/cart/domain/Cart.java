package com.kalamet.cart.domain;

import com.kalamet.catalog.domain.ProductVariant;
import com.kalamet.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One persistent cart per user. Items store no price, so the cart always shows current prices. */
@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("addedAt, id")
    private List<CartItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Cart(User user, Instant now) {
        this.user = user;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Optional<CartItem> item(Long variantId) {
        return items.stream().filter(item -> item.getVariant().getId().equals(variantId)).findFirst();
    }

    public void add(ProductVariant variant, int quantity, Instant now) {
        items.add(new CartItem(this, variant, quantity, now));
        updatedAt = now;
    }

    public void remove(CartItem item, Instant now) {
        items.remove(item);
        updatedAt = now;
    }

    public void clear(Instant now) {
        items.clear();
        updatedAt = now;
    }

    public void touch(Instant now) {
        updatedAt = now;
    }
}
