package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El barrido de mora de la plataforma: facturas registradas, no saldadas y con
 * el vencimiento pasado, de todas las clínicas.
 *
 * <p>
 * Mismo régimen que la lista de trabajo mensual: no filtra por empresa, así que
 * solo lo sirve {@code hasRole("SYSTEM")} a secas.
 */
public interface ListOverdueBillingDocumentsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<BillingDocumentDto> listOverdue(int page, int pageSize);
}
