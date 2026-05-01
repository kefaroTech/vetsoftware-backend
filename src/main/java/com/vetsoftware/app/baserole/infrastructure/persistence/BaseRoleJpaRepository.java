package com.vetsoftware.app.baserole.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseRoleJpaRepository extends JpaRepository<BaseRoleJpaEntity, Long> {
    List<BaseRoleJpaEntity> findByMandatoryTrue();
}
