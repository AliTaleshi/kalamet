package com.kalamet.catalog.repository;

import com.kalamet.catalog.domain.ProductImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /** Main image of each product, for cart and order lines. */
    @Query("""
            select i from ProductImage i
            where i.product.id in :productIds
              and i.sortOrder = (select min(j.sortOrder) from ProductImage j where j.product = i.product)
            order by i.id
            """)
    List<ProductImage> findMainImages(Collection<Long> productIds);
}
