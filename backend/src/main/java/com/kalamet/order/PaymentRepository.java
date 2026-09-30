package com.kalamet.order;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByAuthority(String authority);

    /** Read before locking the order, so the payment itself is loaded only after the lock. */
    @Query("select p.order.id from Payment p where p.authority = :authority")
    Optional<Long> findOrderIdByAuthority(String authority);
}
