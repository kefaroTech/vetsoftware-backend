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
     *            el cuerpo del modelo. Va a {@code ai_proposal_turns.raw_response}
     *            —que no se serializa jamas y se borra a los 90 dias— <strong>solo
     *            si el parser lo puede leer</strong>: el camino de fallo cierra el
     *            turno sin el
     * @param failureCode
     *            ⛔ <strong>un desenlace declarado con la llamada ya
     *            cobrada</strong>, o {@code null} en el camino normal. Existe
     *            porque hay fallos que no pueden viajar como excepcion sin tirar
     *            los contadores de una invocacion que ya se pago: hoy el unico es
     *            {@code MODEL_STRUCTURED_OUTPUT_UNSUPPORTED}, el modelo que no
     *            honra el mecanismo de salida estructurada. El generador lo traduce
     *            a {@code MODEL_FAILED} con este codigo <em>despues</em> de
     *            reconciliar el gasto real
     */
    record ModelInvocation(String modelId, String rawJson, Integer inputTokens,
            Integer outputTokens, String stopReason, String failureCode) {

        /** El camino normal: hubo respuesta y no hay nada declarado que contar. */
        public ModelInvocation(String modelId, String rawJson, Integer inputTokens,
                Integer outputTokens, String stopReason) {
            this(modelId, rawJson, inputTokens, outputTokens, stopReason, null);
        }
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
