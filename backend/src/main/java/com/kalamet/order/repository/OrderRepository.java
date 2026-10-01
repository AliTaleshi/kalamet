package com.kalamet.order.repository;

import com.kalamet.order.domain.Order;
import com.kalamet.order.domain.OrderStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderNumber(String orderNumber);

    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Every state change locks the order row first (then its payments), so the payment
     * callback, the expiry job and admins never interleave.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> lockById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderNumber = :orderNumber")
    Optional<Order> lockByOrderNumber(String orderNumber);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("""
            select o from Order o
            where (:status is null or o.status = :status)
              and (:query is null or o.orderNumber = :query or o.user.mobile = :query)
            """)
    Page<Order> adminSearch(@Param("status") OrderStatus status, @Param("query") String query, Pageable pageable);

    @Query("select o.id from Order o where o.status = :status and o.createdAt < :before order by o.createdAt")
    List<Long> findIdsByStatusCreatedBefore(OrderStatus status, Instant before);

    /** Did this user receive this product? Marks reviews as verified purchases. */
    @Query("""
            select count(i) > 0 from OrderItem i
            where i.order.user.id = :userId
              and i.order.status = com.kalamet.order.domain.OrderStatus.DELIVERED
              and i.variant.product.id = :productId
            """)
    boolean existsDeliveredPurchase(Long userId, Long productId);
}
