package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.subscription.application.port.out.CompanyValidationPort;
import org.springframework.stereotype.Component;

/**
 * El nombre del bean lleva el slice delante: otras features declaran su propio
 * adaptador.
 */
@Component("subscriptionJpaCompanyValidationPort")
public class JpaCompanyValidationPort implements CompanyValidationPort {

    private final CompanyJpaRepository companyJpaRepository;

    public JpaCompanyValidationPort(CompanyJpaRepository companyJpaRepository) {
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public void validateExists(Long companyId) {
        if (companyId == null || !companyJpaRepository.existsById(companyId)) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
    }
}
