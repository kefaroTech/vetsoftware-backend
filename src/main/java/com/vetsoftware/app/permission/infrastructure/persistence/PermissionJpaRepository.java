package com.vetsoftware.app.permission.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionJpaRepository extends JpaRepository<PermissionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"company", "subModule"})
    List<PermissionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"company", "subModule"})
    Optional<PermissionJpaEntity> findById(Long id);

    Optional<PermissionJpaEntity> findByCompanyIdAndCode(Long companyId, String code);
}
