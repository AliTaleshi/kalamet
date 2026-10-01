package com.kalamet.catalog.repository;

import com.kalamet.catalog.domain.ProductImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /** All images of these products with their linked variant, for cart and order lines. */
    @EntityGraph(attributePaths = {"product", "variant"})
    List<ProductImage> findByProductIdIn(Collection<Long> productIds);
}
