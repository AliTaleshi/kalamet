package com.kalamet.cart.repository;

import com.kalamet.cart.domain.Cart;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    /** Checkout locks the cart so a double-clicked "pay" cannot create two orders. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Cart> findWithLockByUserId(Long userId);
}
