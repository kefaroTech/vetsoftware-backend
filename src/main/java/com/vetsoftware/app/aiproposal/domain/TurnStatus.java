package com.vetsoftware.app.aiproposal.domain;

/**
 * Estado del turno ({@code chk_ai_proposal_turns_status}).
 *
 * <p>
 * <strong>{@code PENDING} existe por una regla de arquitectura, no por
 * comodidad.</strong> {@code SIN_IO_EXTERNO_EN_TRANSACCION} prohibe invocar un
 * cliente HTTP dentro de una transaccion, asi que la secuencia obligada es: TX1
 * escribe el turno {@code PENDING} y commitea, se llama al modelo fuera de
 * transaccion, y TX2 lo cierra a {@code SUCCEEDED} o {@code FAILED}. Un turno
 * que nunca recibio respuesta es un estado normal del sistema.
 */
public enum TurnStatus {

    PENDING,

    SUCCEEDED,

    FAILED
}
