package com.vetsoftware.app.companytaxprofile.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyTaxProfileJpaRepository
        extends
            JpaRepository<CompanyTaxProfileJpaEntity, Long> {

    @EntityGraph(attributePaths = {"company", "economicActivity", "responsibilities"})
    Optional<CompanyTaxProfileJpaEntity> findByCompany_Id(Long companyId);

    boolean existsByCompany_Id(Long companyId);
}
