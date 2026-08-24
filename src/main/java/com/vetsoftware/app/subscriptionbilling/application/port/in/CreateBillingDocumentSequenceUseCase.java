package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.command.CreateBillingDocumentSequenceCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentSequenceDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Declarar una serie del consecutivo interno.
 *
 * <p>
 * {@code billing_document_sequences} es un contador global de plataforma sin
 * tenant, así que aquí no hay ninguna empresa que validar y el gate es
 * {@code hasRole("SYSTEM")} a secas.
 */
public interface CreateBillingDocumentSequenceUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    BillingDocumentSequenceDto execute(CreateBillingDocumentSequenceCommand command);
}
