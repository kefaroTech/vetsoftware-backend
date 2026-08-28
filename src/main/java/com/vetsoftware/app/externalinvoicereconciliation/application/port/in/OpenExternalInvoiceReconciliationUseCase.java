package com.vetsoftware.app.externalinvoicereconciliation.application.port.in;

import com.vetsoftware.app.externalinvoicereconciliation.application.command.OpenExternalInvoiceReconciliationCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface OpenExternalInvoiceReconciliationUseCase {

    /**
     * Abre la conciliacion de un documento de cobro devengado, en
     * {@code MISSING_EXTERNAL} y con los dos importes propios.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, sin alternativa por permiso y sin
     * comprobacion de tenant.</strong> La conciliacion es el cuadre entre
     * VetSoftware y su facturador externo, no un documento del cliente: ver el
     * parrafo completo en
     * {@link FindExternalInvoiceReconciliationUseCase#findById(Long)}, que es donde
     * vive la decision.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    ExternalInvoiceReconciliationDto execute(OpenExternalInvoiceReconciliationCommand command);
}
