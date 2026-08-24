package com.vetsoftware.app.quote.application.port.in;

import com.vetsoftware.app.quote.application.command.RejectQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RejectQuoteUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('quote.reject') "
            + "and @authz.isMyCompany(#command.companyId))")
    QuoteDto execute(RejectQuoteCommand command);
}
