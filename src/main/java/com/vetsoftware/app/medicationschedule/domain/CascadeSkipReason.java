package com.vetsoftware.app.medicationschedule.domain;

/**
 * Por que una reprogramacion pedida en {@link RescheduleMode#CASCADE} acabo
 * moviendo solo el pivote.
 *
 * <p>
 * Solo toma valor cuando la cascada <em>se pidio</em> y no se pudo aplicar: con
 * {@link RescheduleMode#ONE} no hay nada que saltarse y viaja nulo, igual que
 * cuando la cascada si se aplico.
 */
public enum CascadeSkipReason {
    /**
     * No se pudo resolver la orden de medicacion. Solo alcanzable por el camino
     * SYSTEM: con empresa, la orden ya quedo resuelta al validar la propiedad y su
     * ausencia habria sido un 404 antes de tocar nada.
     */
    MEDICATION_ORDER_NOT_FOUND,
    /**
     * La pauta no es de INTERVALO. Sus horas son de reloj —no se cronometran desde
     * la anterior—, asi que mover una toma no desplaza a las siguientes.
     */
    GUIDELINE_NOT_INTERVAL,
    /**
     * La frecuencia no es discreta (CONTINUOUS, SINGLE o desconocida), asi que no
     * hay intervalo en horas que propagar.
     */
    FREQUENCY_NOT_DISCRETE
}
