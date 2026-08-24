package com.vetsoftware.app.quote.application.port.in;

import com.vetsoftware.app.quote.application.command.AcceptQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Deja la prueba de la aceptacion: cuando, quien y desde que IP. */
public interface AcceptQuoteUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('quote.accept') "
            + "and @authz.isMyCompany(#command.companyId))")
    QuoteDto execute(AcceptQuoteCommand command);
}
