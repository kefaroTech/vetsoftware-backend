package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.quote.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.quote.domain.CompanyRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Unico archivo del slice que conoce la persistencia de company. */
@Component("quoteJpaCompanyQueryPort")
public class JpaCompanyQueryPort implements CompanyQueryPort {

    private final CompanyJpaRepository companyJpaRepository;

    public JpaCompanyQueryPort(CompanyJpaRepository companyJpaRepository) {
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Optional<CompanyRef> findById(Long companyId) {
        return companyJpaRepository.findById(companyId)
                .map(c -> new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }
}
