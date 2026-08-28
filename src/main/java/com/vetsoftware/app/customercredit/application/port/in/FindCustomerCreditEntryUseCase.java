package com.vetsoftware.app.customercredit.application.port.in;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindCustomerCreditEntryUseCase {

    /**
     * Un asiento del libro. <strong>El cliente ve lo suyo</strong>: el bloque
     * <em>Cobro y saldos</em> lo reparte como «escribe plataforma, leen ambos», y
     * la mitad del valor de un libro de saldo a favor es que el titular pueda
     * auditarlo.
     *
     * <p>
     * Un {@code id} lo escribe el cliente en la URL, asi que el {@code companyId}
     * viaja siempre y la carga va acotada por el en el puerto de salida. No existe
     * la variante ancha a proposito (BE-COV,
     * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}).
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('customerCredit.read')"
            + " and @authz.isMyCompany(#companyId))")
    CustomerCreditEntryDto findById(Long id, Long companyId);
}
