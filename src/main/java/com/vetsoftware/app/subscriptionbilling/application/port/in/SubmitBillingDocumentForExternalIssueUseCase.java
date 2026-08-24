package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.command.SubmitBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Poner el documento en la cola de emisión externa. */
public interface SubmitBillingDocumentForExternalIssueUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    BillingDocumentDto execute(SubmitBillingDocumentCommand command);
}
