package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingEntitlementQueryPort;
import com.vetsoftware.app.membershipsubmodule.infrastructure.persistence.MembershipSubModuleJpaRepository;
import org.springframework.stereotype.Component;

/**
 * Resuelve el derecho a facturación electrónica: empresa → membresía → ¿incluye
 * el submódulo BILLING (habilitado)? Único cruce permitido a la persistencia de
 * otras features (company, membershipsubmodule).
 */
@Component
public class JpaBillingEntitlementQueryPort implements BillingEntitlementQueryPort {
    private static final String BILLING_CODE = "BILLING";

    private final CompanyJpaRepository companyJpaRepository;
    private final MembershipSubModuleJpaRepository membershipSubModuleJpaRepository;

    public JpaBillingEntitlementQueryPort(CompanyJpaRepository companyJpaRepository,
            MembershipSubModuleJpaRepository membershipSubModuleJpaRepository) {
        this.companyJpaRepository = companyJpaRepository;
        this.membershipSubModuleJpaRepository = membershipSubModuleJpaRepository;
    }

    @Override
    public boolean isElectronicInvoicingEnabled(Long companyId) {
        if (companyId == null)
            return false;
        return companyJpaRepository.findById(companyId).map(c -> c.getMembership().getId())
                .map(membershipId -> membershipSubModuleJpaRepository
                        .hasEnabledSubModuleCode(membershipId, BILLING_CODE))
                .orElse(false);
    }
}
