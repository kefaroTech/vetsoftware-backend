package com.vetsoftware.app.externalinvoicereconciliation.application.port.in;

import com.vetsoftware.app.externalinvoicereconciliation.application.command.ResolveExternalInvoiceReconciliationCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ResolveExternalInvoiceReconciliationUseCase {

    /**
     * Cierra el expediente con firma, nota y periodo contable. El instante lo pone
     * el reloj inyectado, nunca el llamante: una fecha de resolucion escrita por
     * quien resuelve se puede antedatar a un periodo ya cerrado.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Resolver es decidir en
     * que periodo contable de Lumbre se imputa un ajuste; no hay lectura ni
     * escritura de tenant en este bloque -ver
     * {@link FindExternalInvoiceReconciliationUseCase#findById(Long)}-.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    ExternalInvoiceReconciliationDto execute(ResolveExternalInvoiceReconciliationCommand command);
}
