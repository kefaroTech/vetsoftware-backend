package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.subscriptionbilling.application.command.IssueCreditNoteCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Emitir la nota crédito que corrige un documento ya registrado.
 *
 * <p>
 * <b>Es el único camino para corregir un documento con factura externa.</b> El
 * original no se toca: si se tocara, lo que dice VetSoftware dejaría de
 * coincidir con lo que tiene la DIAN y no habría forma de saber cuál de los dos
 * miente.
 */
public interface IssueCreditNoteUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    BillingDocumentDto execute(IssueCreditNoteCommand command);
}
