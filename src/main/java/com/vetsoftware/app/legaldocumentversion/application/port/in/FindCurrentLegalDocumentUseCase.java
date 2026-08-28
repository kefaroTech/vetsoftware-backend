package com.vetsoftware.app.legaldocumentversion.application.port.in;

import com.vetsoftware.app.legaldocumentversion.application.dto.LegalDocumentVersionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** El texto vigente de un documento. Lo leen los dos lados. */
public interface FindCurrentLegalDocumentUseCase {

    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('legaldocument.read') and @authz.isMyCompany(#companyId))")
    LegalDocumentVersionDto findCurrentByCode(String code, Long companyId);
}
