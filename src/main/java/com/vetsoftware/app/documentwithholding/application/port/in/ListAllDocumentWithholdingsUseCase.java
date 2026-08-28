package com.vetsoftware.app.documentwithholding.application.port.in;

import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Consulta cross-tenant de tesoreria para la consola de plataforma: que se ha
 * retenido, a quien, de que impuesto y en que periodo, en todas las clinicas.
 */
public interface ListAllDocumentWithholdingsUseCase {

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
    PageResult<DocumentWithholdingDto> listAll(Long companyId, int page, int pageSize);
}
