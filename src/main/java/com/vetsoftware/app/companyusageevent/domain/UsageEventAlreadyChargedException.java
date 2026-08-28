package com.vetsoftware.app.companyusageevent.domain;

/**
 * El hecho de uso ya entro en un cargo y no se puede recolgar de otro.
 *
 * <p>
 * <strong>Es 409 y no 400</strong>: la peticion esta bien escrita: lo que choca
 * es el estado del dinero. Reasignar un hecho ya facturado dejaria el desglose
 * del cargo original sin cuadrar con su importe, y ese descuadre es exactamente
 * lo que esta tabla existe para poder demostrar que no ocurre.
 */
public class UsageEventAlreadyChargedException extends RuntimeException {

    public UsageEventAlreadyChargedException(Long usageEventId, Long chargeId) {
        super("Company usage event " + usageEventId + " is already attached to charge " + chargeId
                + ": a usage fact is billed once. Re-attaching it would leave the original"
                + " charge's breakdown out of step with its amount, silently");
    }
}
