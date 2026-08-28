package com.vetsoftware.app.billingdocumentstatushistory.application.port.in;

import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Barrido cross-tenant para la consola de plataforma: que documentos se han
 * movido, a que estado y por que, en todas las clinicas.
 *
 * <p>
 * Vive en un puerto aparte y no como un parametro opcional del de tenant porque
 * son dos autorizaciones distintas: mezclar admin global y tenant en el mismo
 * caso de uso es el anti-patron que {@code CLAUDE.md} nombra, y aqui bastaria
 * un {@code companyId} nulo para que el listado del cliente devolviera la
 * cartera de todos.
 */
public interface ListAllBillingDocumentStatusHistoryUseCase {

    /**
     * <strong>{@code hasRole('SYSTEM')} a secas y sin alternativa por
     * permiso.</strong> Devuelve filas de todas las empresas cuando
     * {@code companyId} viene vacio, y abrirlo por {@code hasAuthority} seria
     * exactamente la fuga que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} persigue.
     *
     * @param companyId
     *            filtro opcional de la consola. Cuando viene, acota; cuando no, el
     *            barrido es completo
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<BillingDocumentStatusHistoryDto> listAll(Long companyId, int page, int pageSize);
}
