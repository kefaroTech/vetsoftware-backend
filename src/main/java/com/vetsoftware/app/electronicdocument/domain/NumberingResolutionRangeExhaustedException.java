package com.vetsoftware.app.electronicdocument.domain;

/**
 * La resolución de numeración activa consumió su último consecutivo: el rango
 * autorizado por la DIAN se agotó y no queda número que asignar.
 *
 * <p>
 * <b>Es un hecho distinto de «la resolución venció»</b> y por eso tiene su
 * propia clase (#125). Los dos bloquean la facturación y los dos salían antes
 * como {@code INVALID_STATE}, pero la acción del operador no es la misma: uno
 * se corrige ampliando las fechas de vigencia y el otro solicitando un rango
 * nuevo. Un único código obligaba a leer la prosa del mensaje para saber cuál
 * de los dos era —y ese mensaje ya no sale del servidor (#118)—.
 *
 * <p>
 * Vive en {@code electronicdocument/domain} por la misma razón que su hermana:
 * quien la lanza es el adaptador de esta feature, y cruzar al dominio de
 * {@code numberingresolution} rompería el vertical slicing.
 */
public class NumberingResolutionRangeExhaustedException extends RuntimeException {

    private final String resolutionNumber;
    private final Long rangeTo;

    public NumberingResolutionRangeExhaustedException(String resolutionNumber, Long rangeTo) {
        super("Numbering resolution range is exhausted");
        this.resolutionNumber = resolutionNumber;
        this.rangeTo = rangeTo;
    }

    /** Número de la resolución DIAN afectada; puede ser null. */
    public String getResolutionNumber() {
        return resolutionNumber;
    }

    /** Último consecutivo autorizado por la resolución; puede ser null. */
    public Long getRangeTo() {
        return rangeTo;
    }
}
