package com.vetsoftware.app.externalinvoicereconciliation.application.port.in;

import com.vetsoftware.app.externalinvoicereconciliation.application.command.MatchExternalInvoiceCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MatchExternalInvoiceUseCase {

    /**
     * Registra la factura del tercero, calcula {@code difference} y <strong>deja
     * que el dominio decida el estado</strong> entre {@code MATCHED},
     * {@code WITHIN_TOLERANCE} y {@code MISMATCH}. El caso de uso no clasifica: la
     * regla de los dos pesos es una invariante y vive en la entidad.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> No hay camino de tenant
     * en este bloque -ver
     * {@link FindExternalInvoiceReconciliationUseCase#findById(Long)}-, y ademas
     * este es el punto donde entra el dato del <em>tercero</em>: dejarlo escribir a
     * la clinica seria dejarle declarar contra que numero se cuadra su propio
     * cobro.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    ExternalInvoiceReconciliationDto execute(MatchExternalInvoiceCommand command);
}
