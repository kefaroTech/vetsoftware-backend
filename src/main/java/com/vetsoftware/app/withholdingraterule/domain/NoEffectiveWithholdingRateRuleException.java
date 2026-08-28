package com.vetsoftware.app.withholdingraterule.domain;

import java.time.LocalDate;

/**
 * No hay tarifa vigente para el supuesto consultado.
 *
 * <p>
 * <strong>Esta excepcion existe para que el fallo mas caro del modelo deje de
 * ser silencioso.</strong> El comentario del changeset 317 lo dice con nombre y
 * apellido: si {@code service_nature} diverge entre {@code catalog_items} y
 * {@code withholding_rate_rules}, la busqueda de la tarifa devuelve vacio, la
 * retencion esperada sale cero y no hay error —la factura se emite, el cliente
 * gira de menos porque el si retuvo, y nadie se entera hasta cuadrar la
 * cartera—.
 *
 * <p>
 * Devolver un {@code Optional} vacio desde el caso de uso reproduciria
 * exactamente esa forma de fallar: el llamador que no lo mire tratara la
 * ausencia como un cero. Un 404 con el supuesto completo escrito en el mensaje
 * —tipo, naturaleza, municipio y fecha— convierte «no hay tarifa» en algo que
 * se ve en el momento y que dice que falta configurar.
 */
public class NoEffectiveWithholdingRateRuleException extends RuntimeException {

    public NoEffectiveWithholdingRateRuleException(WithholdingType withholdingType,
            ServiceNature serviceNature, String municipalityCode, LocalDate on) {
        super("No effective withholding rate rule for " + withholdingType + "/" + serviceNature
                + " in municipality " + (municipalityCode == null ? "-" : municipalityCode) + " on "
                + on);
    }
}
