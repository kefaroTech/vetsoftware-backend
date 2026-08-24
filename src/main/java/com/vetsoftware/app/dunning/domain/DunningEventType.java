package com.vetsoftware.app.dunning.domain;

/**
 * Los cinco hitos de un expediente de cobranza. Espejo de
 * {@code chk_dunning_events_type}.
 *
 * <p>
 * <strong>R18 - aqui no hay, ni debe haber, un valor que signifique corte total
 * de acceso.</strong> El estado maximo de restriccion es
 * {@link #READ_ONLY_APPLIED}: el moroso consulta e imprime su propia historia
 * clinica, y no puede crear ni modificar. Dejar sin acceso a un cliente a su
 * informacion clinica es una reclamacion garantizada y un riesgo legal real, y
 * esta nota esta escrita aqui porque una politica sin registro la deroga el
 * primer PR que "arregle" la morosidad.
 */
public enum DunningEventType {
    /** Se le aviso. Exige canal: un recordatorio sin canal no prueba nada. */
    REMINDER_SENT,
    /** Empezo a contar la gracia pactada por contrato. */
    GRACE_STARTED,
    /** Se bajo la cuenta a solo lectura. Nunca mas alla de esto. */
    READ_ONLY_APPLIED,
    /** Pago y se le devolvio la operacion normal. */
    REACTIVATED,
    /** Se dio la deuda por incobrable. */
    WRITTEN_OFF
}
