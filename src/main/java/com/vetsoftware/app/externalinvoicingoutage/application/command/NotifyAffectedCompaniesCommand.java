package com.vetsoftware.app.externalinvoicingoutage.application.command;

import java.time.LocalDateTime;

/**
 * Anota que ya se aviso a las clinicas alcanzadas.
 *
 * <p>
 * <strong>Se puede repetir, y sobrescribe.</strong> Avisar dos veces durante
 * una caida larga es lo normal —el segundo correo va con el contador ya
 * corregido— y lo que hay que conservar es la ultima vez que se informo. Lo
 * unico que la base impide ({@code chk_eio_notified}) es informar antes de que
 * la caida empezara.
 *
 * @param notifiedAt
 *            cuando se aviso. Lo pone quien envio el aviso
 * @param affectedCompanyCount
 *            el alcance con el que se aviso, ya corregido respecto a la
 *            estimacion de la apertura
 */
public record NotifyAffectedCompaniesCommand(Long id, LocalDateTime notifiedAt,
        int affectedCompanyCount) {
}
