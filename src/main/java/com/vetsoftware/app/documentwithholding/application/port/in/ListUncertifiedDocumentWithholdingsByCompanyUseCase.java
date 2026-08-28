package com.vetsoftware.app.documentwithholding.application.port.in;

import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La otra mitad de la vigilancia: lo que a <em>esta</em> clinica le retuvieron
 * en un ano y aun no le han certificado.
 *
 * <p>
 * Es la lista con la que el cliente reclama. La diferencia entre lo retenido y
 * lo certificado es exactamente lo que puede exigirle a quien se lo retuvo, y
 * si el ano se cierra sin el papel, ese dinero se pierde: no hay con que
 * imputarlo.
 */
public interface ListUncertifiedDocumentWithholdingsByCompanyUseCase {

    /**
     * Acotada por empresa <strong>y con la empresa revalidada contra el
     * principal</strong>: sin {@code @authz.isMyCompany}, escribir el id de la
     * clinica vecina bastaria para ver que le deben certificar, que es informacion
     * comercial de un tercero.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('documentWithholding.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<DocumentWithholdingDto> listUncertifiedByCompany(Long companyId, int fiscalYear,
            int page, int pageSize);
}
