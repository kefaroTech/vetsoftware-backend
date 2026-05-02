package com.vetsoftware.app.basepermission.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BasePermissionJpaRepository extends JpaRepository<BasePermissionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "subModule")
    List<BasePermissionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "subModule")
    Optional<BasePermissionJpaEntity> findById(Long id);
}
