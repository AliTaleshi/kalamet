package com.kalamet.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @EntityGraph(attributePaths = "product")
    Optional<ProductVariant> findWithProductById(Long id);

    @EntityGraph(attributePaths = "product")
    List<ProductVariant> findWithProductByIdIn(Collection<Long> ids);

    /** Active variants with at most {@code threshold} units left, for the admin dashboard. */
    @EntityGraph(attributePaths = "product")
    @Query("select v from ProductVariant v where v.active = true and v.stock <= :threshold order by v.stock, v.id")
    List<ProductVariant> findLowStock(int threshold);
}
