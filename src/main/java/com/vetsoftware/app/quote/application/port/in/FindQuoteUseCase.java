package com.vetsoftware.app.quote.application.port.in;

import com.vetsoftware.app.quote.application.dto.QuoteDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindQuoteUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('quote.read') "
            + "and @authz.isMyCompany(#companyId))")
    QuoteDto findById(Long id, Long companyId);
}
