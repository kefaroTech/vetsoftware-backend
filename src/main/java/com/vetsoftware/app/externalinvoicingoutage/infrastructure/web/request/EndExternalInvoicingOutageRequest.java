package com.vetsoftware.app.externalinvoicingoutage.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Cierra una caida.
 *
 * @param endedAt
 *            cuando volvio el servicio. Con la hora de inicio mide la duracion
 *            de la interrupcion, que es el numero de la reclamacion, y por eso
 *            lo pone quien lo observo y no el reloj del servidor. Tiene que ser
 *            <b>estrictamente posterior</b> al inicio —lo comprueba el dominio
 *            y lo respalda {@code chk_eio_ended}—: una caida que empieza y
 *            termina en el mismo instante no es una caida, es un registro
 *            escrito con un solo reloj
 */
public record EndExternalInvoicingOutageRequest(
        @NotNull(message = "Debes indicar cuando volvio el servicio.") @Schema(description = "Instante observado del fin. Estrictamente posterior al inicio.") LocalDateTime endedAt) {
}
