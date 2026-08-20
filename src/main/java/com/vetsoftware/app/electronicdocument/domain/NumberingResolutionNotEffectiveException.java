package com.vetsoftware.app.electronicdocument.domain;

import java.time.LocalDate;

/**
 * La resolución de numeración que la empresa tiene activa está fuera de su
 * ventana de vigencia, así que no puede respaldar la emisión.
 *
 * <p>
 * <b>Existe para que el hecho tenga nombre propio</b> (#125). Antes se lanzaba
 * una {@code IllegalStateException} desnuda, que caía en el handler genérico y
 * salía con {@code INVALID_STATE}: exactamente el mismo código que «la cuenta
 * no está abierta» y que otros veinte guardas de estado del backend. Con un
 * solo código para todos, el front no podía distinguir un problema tributario
 * que bloquea toda la facturación —y que solo se arregla registrando una
 * resolución nueva ante la DIAN— de un conflicto corriente de flujo, y el
 * operador tampoco podía contarlos por separado en Grafana.
 *
 * <p>
 * Vive en {@code electronicdocument/domain} y no en {@code numberingresolution}
 * porque quien la lanza es el adaptador de esta feature: el vertical slicing
 * permite que {@code infrastructure/persistence} cruce a la persistencia de la
 * otra feature, nunca a su dominio.
 *
 * <p>
 * Los campos son <b>datos para el handler</b>, no un mensaje: el detalle que ve
 * el cliente lo compone {@code GlobalExceptionHandler} a partir de ellos, de
 * modo que el texto de esta excepción nunca llega al cliente (#118).
 */
public class NumberingResolutionNotEffectiveException extends RuntimeException {

    private final String resolutionNumber;
    private final LocalDate validFrom;
    private final LocalDate validTo;

    public NumberingResolutionNotEffectiveException(String resolutionNumber, LocalDate validFrom,
            LocalDate validTo) {
        super("Numbering resolution is not effective");
        this.resolutionNumber = resolutionNumber;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    /** Número de la resolución DIAN afectada; puede ser null. */
    public String getResolutionNumber() {
        return resolutionNumber;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }
}
