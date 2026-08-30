package com.vetsoftware.app.aiproposal.infrastructure.ai;

/**
 * La costura con el SDK del modelo, y la razon por la que
 * {@link BedrockProposalGenerator} se puede probar entero sin red.
 *
 * <p>
 * <strong>Todo lo caro esta a este lado</strong> —el prompt, la validacion, el
 * saneamiento, el tope de gasto, la degradacion— y nada de eso necesita una
 * llamada real para comprobarse. Lo que queda al otro lado es la serializacion
 * del SDK: un cambio de dependencia, no de logica.
 *
 * <p>
 * ⛔ <strong>Quien implemente esto contra Bedrock hereda una regla
 * dura.</strong> {@code SIN_IO_EXTERNO_EN_TRANSACCION} veta por prefijo los
 * paquetes {@code com.anthropic.} y
 * {@code software.amazon.awssdk.services.bedrockruntime.}, y sigue la cadena de
 * llamadas completa: si algun {@code @Transactional} alcanza este metodo,
 * aunque sea a cuatro saltos, el build se cae. Es lo que se busca.
 */
public interface ModelInvoker {

    /**
     * {@code false} apaga la feature sin desplegar nada: es el kill switch de S10.4
     * y tambien el estado de hoy, porque el acceso al modelo en Bedrock no esta
     * habilitado (un formulario manual sin completar, S10.1.1).
     *
     * <p>
     * Se consulta <strong>antes</strong> de reservar gasto: reservar y liberar por
     * algo que se sabia de antemano solo anade ruido al contador.
     */
    boolean isAvailable();

    /**
     * @throws ModelInvocationException
     *             siempre que no haya una respuesta utilizable. El generador la
     *             traduce a {@code MODEL_FAILED} con un codigo de vocabulario
     *             cerrado; <strong>nunca propaga su mensaje</strong>, que puede
     *             arrastrar el cuerpo de la peticion y con el el texto del
     *             prospecto
     */
    ModelInvocation invoke(ProposalPrompt prompt);

    /**
     * @param rawJson
     *            el cuerpo tal cual. Va a {@code ai_proposal_turns.raw_response},
     *            que no se serializa jamas y se borra a los 90 dias
     */
    record ModelInvocation(String modelId, String rawJson, Integer inputTokens,
            Integer outputTokens, String stopReason) {
    }

    /** Fallo de invocacion. Sin causa encadenada hacia arriba a proposito. */
    class ModelInvocationException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final String failureCode;

        public ModelInvocationException(String failureCode, String message) {
            super(message);
            this.failureCode = failureCode;
        }

        public String getFailureCode() {
            return failureCode;
        }
    }
}
