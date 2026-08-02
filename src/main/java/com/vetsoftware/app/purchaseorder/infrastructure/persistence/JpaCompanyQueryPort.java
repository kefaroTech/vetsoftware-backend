package com.vetsoftware.app.purchaseorder.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.purchaseorder.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.purchaseorder.domain.CompanyRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("purchaseOrderJpaCompanyQueryPort")
public class JpaCompanyQueryPort implements CompanyQueryPort {
    private final CompanyJpaRepository companyJpaRepository;

    public JpaCompanyQueryPort(CompanyJpaRepository companyJpaRepository) {
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Optional<CompanyRef> findById(Long companyId) {
        return companyJpaRepository.findById(companyId)
                .map(e -> new CompanyRef(e.getId(), e.getName(), e.getIdentifier()));
    }
}
