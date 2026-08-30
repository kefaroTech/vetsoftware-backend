package com.vetsoftware.app.aiproposal.application.dto;

import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;

/**
 * Lo que devuelve el generador: siempre un borrador utilizable y siempre un
 * {@link GenerationOutcome} que dice como se llego a el.
 *
 * @param usage
 *            las medidas de la invocacion, o {@code null} si no la hubo. Es lo
 *            que TX2 escribe en el turno; {@code output_tokens} es nulable en
 *            el esquema justamente por esto
 * @param failureCode
 *            codigo corto y de vocabulario cerrado, espejo de
 *            {@code chk_ai_proposal_turns_failure} (maximo 40 caracteres).
 *            <strong>Nunca lleva el mensaje de la excepcion</strong>: eso puede
 *            arrastrar el cuerpo de la peticion, y el cuerpo lleva el texto del
 *            prospecto
 * @param latencyMs
 *            lo que tardo la invocacion, tambien cuando fallo. Va aparte de
 *            {@link ModelUsage} porque {@code chk_ai_proposal_turns_model_arc}
 *            no admite tokens en un turno sin salida, pero
 *            {@code ProposalTurn.cerrarConFallo} si quiere la latencia: un
 *            fallo a los 25 segundos y uno a los 40 milisegundos son averias
 *            distintas
 */
public record ProposalGenerationResult(GenerationOutcome outcome, ProposalDraft draft,
        ModelUsage usage, String failureCode, Integer latencyMs) {

    public ProposalGenerationResult {
        if (outcome == null)
            throw new IllegalArgumentException("outcome is required");
        if (draft == null)
            throw new IllegalArgumentException("a result always carries a usable draft");
        if (failureCode != null && failureCode.length() > 40)
            throw new IllegalArgumentException("failureCode must be 40 chars or less");
        if (latencyMs != null && latencyMs < 0)
            throw new IllegalArgumentException("latencyMs cannot be negative");
    }

    /**
     * La degradacion: sin lineas, sin medidas y con el motivo en el
     * {@code outcome}. El carrito lo construye el camino determinista.
     */
    public static ProposalGenerationResult degradado(GenerationOutcome outcome) {
        return new ProposalGenerationResult(outcome, ProposalDraft.sinLineas(false, false), null,
                null, null);
    }

    public boolean seInvocoAlModelo() {
        return usage != null;
    }
}
