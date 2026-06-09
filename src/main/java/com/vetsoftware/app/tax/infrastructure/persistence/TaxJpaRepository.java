package com.vetsoftware.app.tax.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxJpaRepository extends JpaRepository<TaxJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "company")
    List<TaxJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "company")
    Optional<TaxJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "company")
    List<TaxJpaEntity> findAllByCompanyId(Long companyId);

    @EntityGraph(attributePaths = "company")
    Optional<TaxJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE taxes SET enabled = true WHERE id = :id",
        nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);
}
