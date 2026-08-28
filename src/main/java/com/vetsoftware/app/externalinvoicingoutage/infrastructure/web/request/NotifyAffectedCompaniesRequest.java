package com.vetsoftware.app.externalinvoicingoutage.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;

/**
 * Anota que ya se aviso a las clinicas alcanzadas.
 *
 * <p>
 * <strong>Se puede repetir, y sobrescribe.</strong> Durante una caida larga se
 * avisa varias veces; lo que hay que conservar es la ultima vez que se informo,
 * con el alcance ya corregido. Lo unico que la base impide
 * ({@code chk_eio_notified}) es informar <em>antes</em> de que la caida
 * empezara.
 *
 * @param notifiedAt
 *            cuando se envio el aviso. Lo pone quien lo envio, no el servidor:
 *            este endpoint registra el aviso, no lo manda
 * @param affectedCompanyCount
 *            el alcance con el que se aviso, ya corregido respecto a la
 *            estimacion de la apertura
 */
public record NotifyAffectedCompaniesRequest(
        @NotNull(message = "Debes indicar cuando se aviso a las empresas.") @Schema(description = "Instante del aviso. No puede preceder al inicio de la caida.") LocalDateTime notifiedAt,
        @PositiveOrZero(message = "El numero de empresas alcanzadas no puede ser negativo.") int affectedCompanyCount) {
}
