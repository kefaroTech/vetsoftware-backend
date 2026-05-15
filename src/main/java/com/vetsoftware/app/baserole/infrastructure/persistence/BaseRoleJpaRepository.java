package com.vetsoftware.app.baserole.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseRoleJpaRepository extends JpaRepository<BaseRoleJpaEntity, Long> {
    List<BaseRoleJpaEntity> findByMandatoryTrue();

    Optional<BaseRoleJpaEntity> findByCode(String code);
}
