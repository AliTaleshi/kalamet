package com.kalamet.user;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByMobileOrderByCreatedAtDesc(String mobile);

    /** Locked so that parallel guesses cannot exceed the attempt limit. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpCode> findFirstWithLockByMobileOrderByCreatedAtDesc(String mobile);

    long countByMobileAndCreatedAtAfter(String mobile, Instant since);

    @Modifying
    @Query("delete from OtpCode o where o.createdAt < :before")
    int deleteCreatedBefore(Instant before);
}
