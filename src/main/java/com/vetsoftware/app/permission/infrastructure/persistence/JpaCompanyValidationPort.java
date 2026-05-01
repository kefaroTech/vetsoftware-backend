package com.vetsoftware.app.permission.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.permission.application.port.out.CompanyValidationPort;
import org.springframework.stereotype.Component;

@Component("permissionJpaCompanyValidationPort")
public class JpaCompanyValidationPort implements CompanyValidationPort {
    private final CompanyJpaRepository companyJpaRepository;

    public JpaCompanyValidationPort(CompanyJpaRepository companyJpaRepository) {
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public void validateExists(Long companyId) {
        if (!companyJpaRepository.existsById(companyId)) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
    }
}
