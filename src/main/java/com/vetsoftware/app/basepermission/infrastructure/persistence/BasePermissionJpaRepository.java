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

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE base_permissions
            SET enabled = true
            WHERE id = :id
            """, nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
