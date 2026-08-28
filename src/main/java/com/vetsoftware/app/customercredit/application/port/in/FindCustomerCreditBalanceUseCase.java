package com.vetsoftware.app.customercredit.application.port.in;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditBalanceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindCustomerCreditBalanceUseCase {

    /**
     * El saldo a favor de una empresa. Es la lectura que el cliente necesita —«que
     * tengo a favor y cuando se me vence»— y por eso el bloque la reparte como
     * «leen ambos».
     *
     * <p>
     * Recuerda que la fila es una <strong>proyeccion</strong> y no la verdad: la
     * verdad es la suma del libro. Un cuadre que discrepe se resuelve rehaciendo
     * esta fila, nunca corrigiendo asientos.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('customerCredit.read')"
            + " and @authz.isMyCompany(#companyId))")
    CustomerCreditBalanceDto findByCompanyId(Long companyId);
}
