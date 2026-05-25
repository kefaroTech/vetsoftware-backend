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

    @EntityGraph(attributePaths = {"company", "subModule"})
    List<PermissionJpaEntity> findAllByCompanyId(Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE permissions SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByCompany_Id(Long companyId);

    boolean existsBySubModule_Id(Long subModuleId);
}
