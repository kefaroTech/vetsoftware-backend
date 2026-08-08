package com.vetsoftware.app.companytaxprofile.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CompanyTaxProfileJpaRepository
        extends
            JpaRepository<CompanyTaxProfileJpaEntity, Long> {

    @EntityGraph(attributePaths = {"company", "economicActivity", "responsibilities"})
    Optional<CompanyTaxProfileJpaEntity> findByCompany_Id(Long companyId);

    boolean existsByCompany_Id(Long companyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE company_tax_profiles
            SET enabled = false
            WHERE company_id = :companyId
            """, nativeQuery = true)
    int deleteByCompanyId(@Param("companyId") Long companyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE company_tax_profiles
            SET enabled = true
            WHERE company_id = :companyId
            """, nativeQuery = true)
    int reactivate(@Param("companyId") Long companyId);
}
