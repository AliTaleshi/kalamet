package com.kalamet.catalog;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"brand", "category"})
    Optional<Product> findBySlugAndActiveTrue(String slug);

    @EntityGraph(attributePaths = {"brand", "category"})
    Optional<Product> findWithDetailsById(Long id);

    boolean existsByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"brand", "category"})
    @Query("""
            select p from Product p
            where (:query is null or lower(p.name) like lower(concat('%', cast(:query as string), '%'))
                   or lower(p.nameEn) like lower(concat('%', cast(:query as string), '%'))
                   or lower(p.slug) like lower(concat('%', cast(:query as string), '%')))
              and (:active is null or p.active = :active)
            """)
    Page<Product> adminSearch(@Param("query") String query, @Param("active") Boolean active, Pageable pageable);
}
