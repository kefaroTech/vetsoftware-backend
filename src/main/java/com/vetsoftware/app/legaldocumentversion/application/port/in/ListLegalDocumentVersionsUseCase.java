package com.vetsoftware.app.legaldocumentversion.application.port.in;

import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El historial de versiones de un documento, de la mas nueva a la mas vieja.
 */
public interface ListLegalDocumentVersionsUseCase {

    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('legaldocument.read') and @authz.isMyCompany(#companyId))")
    PageResult<LegalDocumentVersionDto> listByCode(String code, Long companyId, int page,
            int pageSize);
}
