package com.kalamet.catalog.repository;

import com.kalamet.catalog.domain.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
}
