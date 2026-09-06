package com.vetsoftware.app.paymentgateway.application.port.out;

import java.util.Optional;

/**
 * El correo fiscal de la empresa ({@code company_tax_profiles.fiscal_email}),
 * que es de otra feature. Solo eso: nada más del perfil fiscal le interesa a la
 * pasarela.
 */
public interface CompanyBillingEmailQueryPort {
    Optional<String> findFiscalEmail(Long companyId);
}
