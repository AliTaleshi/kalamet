package com.kalamet.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AddressRepository extends JpaRepository<Address, Long> {

    @EntityGraph(attributePaths = "province")
    List<Address> findByUserIdOrderByDefaultAddressDescCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "province")
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    /** Runs before another address becomes default: the database allows one default per user. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Address a set a.defaultAddress = false where a.user.id = :userId and a.defaultAddress = true")
    void clearDefault(Long userId);
}
