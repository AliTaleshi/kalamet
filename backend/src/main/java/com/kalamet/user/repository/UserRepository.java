package com.kalamet.user.repository;

import com.kalamet.user.domain.Role;
import com.kalamet.user.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMobile(String mobile);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("""
            select u from User u
            where (:query is null or u.mobile like concat('%', cast(:query as string), '%')
                   or lower(u.firstName) like lower(concat('%', cast(:query as string), '%'))
                   or lower(u.lastName) like lower(concat('%', cast(:query as string), '%')))
              and (:role is null or u.role = :role)
            """)
    Page<User> search(@Param("query") String query, @Param("role") Role role, Pageable pageable);
}
