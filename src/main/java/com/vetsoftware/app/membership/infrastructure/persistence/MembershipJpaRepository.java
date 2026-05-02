package com.vetsoftware.app.membership.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipJpaRepository extends JpaRepository<MembershipJpaEntity, Long> {

    Optional<MembershipJpaEntity> findFirstByNameAndStatus(String name, String status);
}
