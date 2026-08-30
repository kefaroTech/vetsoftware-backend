package com.vetsoftware.app.aiproposal.application.dto;

import java.math.BigDecimal;

/**
 * Las medidas de una invocacion, que son las columnas de resultado del turno.
 *
 * @param rawResponse
 *            el cuerpo crudo del modelo. <strong>No se serializa jamas</strong>
 *            y se borra a los 90 dias: es donde vive la prosa sin sanear, y
 *            existe para poder medir la calidad del modelo sin tener que dejar
 *            texto sin sanear en {@code ai_proposal_lines}, al alcance del
 *            siguiente consumidor que no sepa que tiene que sanear
 * @param costUsd
 *            lo que costo de verdad, para reconciliar la reserva del tope de
 *            gasto. Se reconcilia <strong>antes</strong> de abrir TX2: el gasto
 *            ocurrio aunque la escritura falle
 */
public record ModelUsage(String modelId, String promptVersion, Integer inputTokens,
        Integer outputTokens, Integer latencyMs, String stopReason, String rawResponse,
        BigDecimal costUsd) {

    public ModelUsage {
        if (modelId == null || modelId.isBlank())
            throw new IllegalArgumentException("modelId is required");
        if (promptVersion == null || promptVersion.isBlank())
            throw new IllegalArgumentException("promptVersion is required");
        if (costUsd == null || costUsd.signum() < 0)
            throw new IllegalArgumentException("costUsd must be zero or positive");
    }
}
