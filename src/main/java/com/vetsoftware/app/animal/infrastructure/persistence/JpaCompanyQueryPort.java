package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.animal.domain.CompanyRef;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("animalJpaCompanyQueryPort")
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
