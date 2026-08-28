package com.vetsoftware.app.externalinvoicingoutage.application.port.in;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListOpenExternalInvoicingOutagesUseCase {

    /**
     * <strong>La consulta que justifica el bloque</strong>: que esta caido ahora
     * mismo. Se apoya en {@code ix_eio_open (ended_at, started_at)}.
     *
     * <p>
     * Devuelve {@code List} y no una pagina <b>a proposito</b>, y la cota no es una
     * preferencia sino una invariante del esquema: {@code uq_eio_open} admite una
     * sola caida abierta por causante y
     * {@link com.vetsoftware.app.externalinvoicingoutage.domain.CauseParty} tiene
     * cuatro valores, asi que esta lista <b>no puede pasar de cuatro filas</b>.
     * Paginar cuatro filas seria fabricar metadatos para nada.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas</strong>, como sus hermanas: sin
     * columna de empresa no hay forma de acotarla.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<ExternalInvoicingOutageDto> listOpen();
}
