package com.vetsoftware.app.subscription.domain;

import java.util.Locale;

/**
 * Por que cambio de estado el contrato. <b>Vocabulario cerrado, y ahora de
 * verdad.</b>
 *
 * <p>
 * {@code SubscriptionAuditPort} llevaba escrito en su javadoc que
 * {@code reason} era esta lista exacta y que texto libre de usuario en un canal
 * de auditoria es <i>log injection</i> esperando a ocurrir (ASVS V7.3.1). La
 * barandilla no existia: el controlador pasaba {@code request.reason()} del
 * cuerpo HTTP tal cual y el barrido de ciclo de vida concatenaba frases en
 * español. Un cliente podia meter saltos de linea y campos inventados en el
 * cuerpo y <b>fabricar entradas de bitacora que pareciesen de otro evento</b>.
 * En un registro que se usa como prueba ante una disputa de cobro —que es
 * exactamente para lo que existe este— eso no es cosmetico: es falsificar la
 * prueba con la que la plataforma se defiende.
 *
 * <p>
 * <b>Lo que se pierde al cerrarlo no se pierde.</b> Las frases que construia el
 * barrido llevaban dentro una fecha —«Periodo de prueba finalizado el
 * 2026-01-14»— que ya esta en {@code occurredAt} de la propia fila y en el
 * contrato. El javadoc del puerto lo decia: el motivo tecleado «no agrega nada
 * que la fila no tenga». El detalle narrativo que si es util —numero de
 * factura, dias de retraso— sigue escribiendose entero en la bitacora de
 * cobranza ({@code DunningEvent}), que es suya y no cruza a este canal.
 *
 * <p>
 * <b>Fuera de la lista se rechaza, no se sanea.</b> Sanear un valor invalido
 * esconde el intento: el que probo a inyectar recibiria un 2xx y nadie se
 * enteraria. En el borde HTTP el enum viaja por su nombre y es el propio
 * deserializador el que rechaza cualquier otra cosa con un 400 que nombra el
 * campo.
 *
 * <p>
 * <b>Sin anotaciones de Jackson, y no por descuido:</b> la regla dura
 * {@code DOMINIO_SIN_FRAMEWORK} prohibe que {@code ..domain..} dependa de
 * {@code com.fasterxml.jackson..} o de {@code tools.jackson..}. El nombre del
 * enum es la forma en que viaja por HTTP —igual que {@code SubscriptionStatus}
 * en esta misma peticion— y {@link #code()} es la forma en que se escribe en la
 * bitacora y en el canal de auditoria, que es el vocabulario en minusculas que
 * el puerto llevaba documentado.
 */
public enum SubscriptionStatusChangeReason {

    /** Hay saldo vencido: la cobranza degrada a PAST_DUE o a READ_ONLY. */
    OVERDUE_BALANCE,

    /** Ya no queda saldo vencido: la cobranza reactiva el contrato. */
    PAYMENT_RECEIVED,

    /** Se acabo el periodo de prueba y el contrato pasa a ACTIVE. */
    TRIAL_ENDED,

    /** Llego la fecha efectiva de una baja ya solicitada. */
    CANCELLATION_EFFECTIVE,

    /** Vencio el periodo contratado sin renovacion. */
    PERIOD_EXPIRED,

    /** Decision deliberada de una persona de plataforma. */
    MANUAL,

    /**
     * El cliente acepto una cotizacion y ese contrato sustituye al que habia
     * (DC-2).
     *
     * <p>
     * <strong>Es un codigo nuevo y no se reutilizo ninguno de los cinco
     * anteriores</strong>, aunque hubiera sido mas barato. Ninguno dice la verdad:
     * {@code MANUAL} atribuiria a una persona de plataforma una decision que tomo
     * el cliente; {@code CANCELLATION_EFFECTIVE} describe una baja que alguien
     * pidio y que llego a su fecha, y aqui no hay baja ninguna —el cliente no se
     * fue, compro mas—; {@code PERIOD_EXPIRED} afirma un vencimiento que no
     * ocurrio. Esta columna es prueba en una disputa de cobro: poner el rotulo
     * equivocado en la fila que explica por que murio un contrato es exactamente lo
     * que la convierte en inservible el dia que hay que exhibirla.
     */
    REPLACED_BY_NEW_CONTRACT;

    /**
     * La forma en que este motivo se escribe en la columna {@code reason} de la
     * bitacora y sale por el canal de auditoria. Minusculas, que es como estaba
     * documentado el vocabulario desde el principio.
     *
     * <p>
     * Es una constante derivada del nombre, no texto: no hay forma de que un salto
     * de linea ni un campo inventado lleguen hasta aqui.
     */
    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }
}
