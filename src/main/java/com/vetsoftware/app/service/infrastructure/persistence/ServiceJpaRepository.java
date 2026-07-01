package com.vetsoftware.app.service.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServiceJpaRepository extends JpaRepository<ServiceJpaEntity, Long>,
        JpaSpecificationExecutor<ServiceJpaEntity> {

    @Override
    @EntityGraph(attributePaths = {"serviceCategory", "tax", "company"})
    List<ServiceJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"serviceCategory", "tax", "company"})
    Optional<ServiceJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"serviceCategory", "tax", "company"})
    List<ServiceJpaEntity> findAllByCompanyId(Long companyId);

    @EntityGraph(attributePaths = {"serviceCategory", "tax", "company"})
    Optional<ServiceJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE services SET enabled = true WHERE id = :id AND company_id = :companyId",
        nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
                   @org.springframework.data.repository.query.Param("companyId") Long companyId);

    boolean existsByTax_Id(Long taxId);

    boolean existsByServiceCategory_Id(Long serviceCategoryId);

    boolean existsByIdAndCompany_Id(Long id, Long companyId);
}
