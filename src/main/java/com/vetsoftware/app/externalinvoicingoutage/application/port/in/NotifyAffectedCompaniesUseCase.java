package com.vetsoftware.app.externalinvoicingoutage.application.port.in;

import com.vetsoftware.app.externalinvoicingoutage.application.command.NotifyAffectedCompaniesCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NotifyAffectedCompaniesUseCase {

    /**
     * Anota que ya se aviso a las clinicas alcanzadas, y con que alcance.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Es la marca de que la
     * plataforma cumplio con avisar; que la escribiera el propio avisado la
     * vaciaria de sentido.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    ExternalInvoicingOutageDto execute(NotifyAffectedCompaniesCommand command);
}
