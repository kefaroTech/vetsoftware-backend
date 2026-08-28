package com.vetsoftware.app.externalinvoicingoutage.domain;

import java.time.LocalDateTime;

/**
 * Cerrar una caida que ya estaba cerrada. <b>409, no 400</b>: la peticion esta
 * bien escrita y choca con el estado del mundo.
 *
 * <p>
 * El mensaje lleva la hora del cierre anterior porque es exactamente el dato
 * que necesita quien lo intento: si la que trae es distinta, alguien midio la
 * duracion de la interrupcion de dos maneras y eso hay que resolverlo mirando,
 * no sobrescribiendo.
 */
public class ExternalInvoicingOutageAlreadyEndedException extends RuntimeException {

    public ExternalInvoicingOutageAlreadyEndedException(Long id, LocalDateTime endedAt) {
        super("External invoicing outage " + id + " already ended at " + endedAt
                + ": the end time measures the length of the interruption and is not overwritten");
    }
}
