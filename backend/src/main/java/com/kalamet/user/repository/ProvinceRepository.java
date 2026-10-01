package com.kalamet.user.repository;

import com.kalamet.user.domain.Province;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvinceRepository extends JpaRepository<Province, Short> {
}
