package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <b>La lista de trabajo pendiente de cada mes</b>: los documentos atascados
 * esperando emisión externa, de todas las clínicas.
 *
 * <p>
 * <b>Cerrado a {@code hasRole("SYSTEM")} a secas porque no filtra por
 * empresa</b>, que es exactamente lo que exige
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29). No es una comodidad: sin
 * ese cierre, un empleado con el permiso de lectura vería la cartera de todos
 * los demás tenants.
 */
public interface ListBillingDocumentsAwaitingExternalUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<BillingDocumentDto> listAwaitingExternal(int page, int pageSize);
}
