package com.vetsoftware.app.externalinvoicingoutage.application.command;

import java.time.LocalDateTime;

/**
 * Cierra una caida.
 *
 * @param endedAt
 *            cuando volvio el servicio. Con la hora de inicio mide la duracion
 *            de la interrupcion, que es el numero de la reclamacion, y por eso
 *            <b>lo pone quien lo observo</b> y no el reloj del servidor: la
 *            ficha se cierra cuando alguien se da cuenta, no cuando el servicio
 *            vuelve
 */
public record EndExternalInvoicingOutageCommand(Long id, LocalDateTime endedAt) {
}
