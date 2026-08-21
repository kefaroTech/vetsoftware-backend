package com.vetsoftware.app.supplier.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierJpaRepository
        extends
            JpaRepository<SupplierJpaEntity, Long>,
            JpaSpecificationExecutor<SupplierJpaEntity> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<SupplierJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<SupplierJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    List<SupplierJpaEntity> findAllByCompanyId(Long companyId);

    @EntityGraph(attributePaths = "company")
    Optional<SupplierJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    boolean existsByIdAndCompany_Id(Long id, Long companyId);

    // @SQLRestriction("enabled = true") aplica: solo cuenta proveedores ACTIVOS (un
    // name desactivado
    // se reusa).
    boolean existsByCompany_IdAndName(Long companyId, String name);

    boolean existsByCompany_IdAndNameAndIdNot(Long companyId, String name, Long id);
}
