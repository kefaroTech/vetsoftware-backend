package com.vetsoftware.app.accountmapping.domain;

import java.time.LocalDate;

/**
 * No hay mapeo vigente para el supuesto que se preguntó.
 *
 * <p>
 * <strong>Lanza en vez de devolver vacio, y esa decision es lo mas importante
 * de esta feature.</strong> El fallo caro de un puente concepto → cuenta no es
 * un error: es un asiento que no se genera, o que se genera contra la cuenta
 * equivocada. Un {@code Optional} vacio invita al llamador a tratar la ausencia
 * como «no habia nada que asentar» y el descuadre aparece meses despues, al
 * cuadrar el balance de prueba, sin un solo rastro de por donde entro.
 *
 * <p>
 * El mensaje lleva el supuesto completo —clase, subclave, articulo, tipo de
 * cargo, tratamiento y fecha— para que quien lo lea sepa <em>que</em> mapeo hay
 * que configurar y no solo que algo no salio.
 */
public class NoEffectiveAccountMappingException extends RuntimeException {

    public NoEffectiveAccountMappingException(MappingKind mappingKind, String mappingKey,
            Long catalogItemId, String chargeType, String taxTreatment, LocalDate on) {
        super("No effective account mapping for kind=" + mappingKind + ", key=" + mappingKey
                + ", catalogItemId=" + catalogItemId + ", chargeType=" + chargeType
                + ", taxTreatment=" + taxTreatment + " on " + on);
    }
}
