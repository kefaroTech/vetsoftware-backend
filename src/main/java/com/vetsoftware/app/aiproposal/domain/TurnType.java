package com.vetsoftware.app.aiproposal.domain;

/**
 * Que produjo el turno ({@code chk_ai_proposal_turns_type}).
 *
 * <p>
 * El arco es exclusivo y lo comprueba tambien la base
 * ({@code chk_ai_proposal_turns_model_arc}): un turno de modelo lleva
 * {@code modelId} y {@code promptVersion}; una edicion del cliente no lleva
 * ninguna de las dos ni consume tokens. Sin esa separacion, "tokens consumidos"
 * acaba sumando filas que nunca llamaron al modelo.
 */
public enum TurnType {

    MODEL_INITIAL,

    MODEL_REFINEMENT,

    CUSTOMER_EDIT;

    /** Los dos turnos que pagan una llamada al modelo. */
    public boolean invocaAlModelo() {
        return this == MODEL_INITIAL || this == MODEL_REFINEMENT;
    }
}
