package com.kalamet.review.repository;

import com.kalamet.review.domain.Review;
import com.kalamet.review.domain.ReviewStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<Review> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "user")
    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "product")
    Page<Review> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "product"})
    @Query("select r from Review r where (:status is null or r.status = :status)")
    Page<Review> adminSearch(@Param("status") ReviewStatus status, Pageable pageable);
}
