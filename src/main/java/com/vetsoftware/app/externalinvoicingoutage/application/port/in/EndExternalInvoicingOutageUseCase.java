package com.vetsoftware.app.externalinvoicingoutage.application.port.in;

import com.vetsoftware.app.externalinvoicingoutage.application.command.EndExternalInvoicingOutageCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface EndExternalInvoicingOutageUseCase {

    /**
     * Cierra la caida: escribe la hora en que volvio el servicio.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Cerrar es lo que libera
     * el hueco de {@code uq_eio_open} para que se pueda abrir la siguiente del
     * mismo causante; dejar esa llave en manos de un tenant permitiria dar por
     * terminada, para todos, una caida que sigue viva.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    ExternalInvoicingOutageDto execute(EndExternalInvoicingOutageCommand command);
}
