package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import com.vetsoftware.app.companytaxprofile.infrastructure.persistence.CompanyTaxProfileJpaRepository;
import com.vetsoftware.app.paymentgateway.application.port.out.CompanyBillingEmailQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El otro cruce permitido de este slice: consume
 * {@code CompanyTaxProfileJpaRepository} de {@code companytaxprofile} por su
 * variante vigente, sin importar su dominio.
 */
@Component
public class JpaCompanyBillingEmailQueryPort implements CompanyBillingEmailQueryPort {

    private final CompanyTaxProfileJpaRepository companyTaxProfileJpaRepository;

    public JpaCompanyBillingEmailQueryPort(
            CompanyTaxProfileJpaRepository companyTaxProfileJpaRepository) {
        this.companyTaxProfileJpaRepository = companyTaxProfileJpaRepository;
    }

    @Override
    public Optional<String> findFiscalEmail(Long companyId) {
        return companyTaxProfileJpaRepository.findCurrentByCompanyId(companyId)
                .map(profile -> profile.getFiscalEmail());
    }
}
