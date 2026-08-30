package com.vetsoftware.app.aiproposal.domain;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Un turno: una llamada al modelo, o una edicion manual del cliente (tabla
 * {@code ai_proposal_turns}, changeset 384).
 *
 * <p>
 * <strong>El texto libre vive aqui, no en la cabecera.</strong> Dos columnas en
 * {@code ai_proposals} resolverian hoy y obligarian a un {@code ALTER} el dia
 * que alguien refine dos veces; un turno por entrada lo resuelve para n
 * refinamientos sin cambio de esquema: turno 1 es el texto original, los turnos
 * 2..n son refinamientos, y son <strong>acumulativos</strong>.
 *
 * <p>
 * <strong>{@code PENDING} no es comodidad, es una regla de
 * arquitectura.</strong> {@code SIN_IO_EXTERNO_EN_TRANSACCION} prohibe invocar
 * un cliente HTTP dentro de una transaccion -retiene la conexion de Hikari 3-20
 * s y con trafico se agota el pool-, asi que la secuencia obligada es: TX1
 * escribe el turno {@code PENDING} y commitea, se llama al modelo
 * <em>fuera</em> de transaccion, y TX2 lo cierra. Por eso todas las columnas de
 * resultado son nulables y {@link TurnStatus#FAILED} es un estado normal, no
 * una anomalia.
 *
 * <p>
 * <strong>El arco de modelo es exclusivo</strong>
 * ({@code chk_ai_proposal_turns_model_arc}): un turno de modelo lleva
 * {@code modelId} y {@code promptVersion}; una edicion del cliente no lleva
 * ninguna de las dos ni consume tokens. Sin esa separacion, "tokens consumidos"
 * acaba sumando filas que nunca llamaron al modelo.
 */
public class ProposalTurn {

    private Long id;
    private final Long proposalId;
    private final int turnNumber;
    private final TurnType turnType;
    private TurnStatus status;
    private final String inputText;
    private final Integer inputTextChars;
    private final String modelId;
    private final String promptVersion;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer latencyMs;
    private String stopReason;
    private String rawResponse;
    private String failureCode;
    private final String clientRequestId;
    private final LocalDateTime createdDate;
    private LocalDateTime completedAt;
    private Long version;

    @SuppressWarnings("java:S107")
    public ProposalTurn(Long id, Long proposalId, int turnNumber, TurnType turnType,
            TurnStatus status, String inputText, Integer inputTextChars, String modelId,
            String promptVersion, Integer inputTokens, Integer outputTokens, Integer latencyMs,
            String stopReason, String rawResponse, String failureCode, String clientRequestId,
            LocalDateTime createdDate, LocalDateTime completedAt, Long version) {
        if (proposalId == null)
            throw new IllegalArgumentException("proposalId is required");
        if (turnType == null)
            throw new IllegalArgumentException("turnType is required");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        validarNumero(turnNumber, turnType);
        validarTexto(inputText, clientRequestId);
        validarArcoDeModelo(turnType, modelId, promptVersion, inputTokens, outputTokens,
                rawResponse, stopReason);
        validarMedidas(inputTokens, outputTokens, latencyMs);
        validarCierre(status, failureCode, completedAt);
        this.id = id;
        this.proposalId = proposalId;
        this.turnNumber = turnNumber;
        this.turnType = turnType;
        this.status = status;
        this.inputText = inputText;
        this.inputTextChars = inputTextChars;
        this.modelId = modelId;
        this.promptVersion = promptVersion;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.latencyMs = latencyMs;
        this.stopReason = stopReason;
        this.rawResponse = rawResponse;
        this.failureCode = failureCode;
        this.clientRequestId = clientRequestId;
        this.createdDate = createdDate;
        this.completedAt = completedAt;
        this.version = version;
    }

    /**
     * El turno que se escribe y se commitea <strong>antes</strong> de invocar al
     * modelo. {@code inputTextChars} se deriva del propio texto: guardarlo aparte
     * permite medir la longitud de lo que escribio el prospecto cuando el texto ya
     * se anonimizo.
     */
    @SuppressWarnings("java:S107")
    public static ProposalTurn pendienteDeModelo(Long proposalId, int turnNumber, TurnType turnType,
            String inputText, String modelId, String promptVersion, String clientRequestId,
            Clock clock) {
        if (turnType == null || !turnType.invocaAlModelo())
            throw new IllegalArgumentException("this factory only builds model turns");
        return new ProposalTurn(null, proposalId, turnNumber, turnType, TurnStatus.PENDING,
                inputText, inputText == null ? null : inputText.length(), modelId, promptVersion,
                null, null, null, null, null, null, clientRequestId, LocalDateTime.now(clock), null,
                null);
    }

    /**
     * La edicion manual: no llama a nadie, asi que nace cerrada. Ni {@code modelId}
     * ni tokens ni {@code rawResponse} -el arco exclusivo del {@code CHECK}-.
     */
    public static ProposalTurn edicionDelCliente(Long proposalId, int turnNumber,
            String clientRequestId, Clock clock) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        return new ProposalTurn(null, proposalId, turnNumber, TurnType.CUSTOMER_EDIT,
                TurnStatus.SUCCEEDED, null, null, null, null, null, null, null, null, null, null,
                clientRequestId, ahora, ahora, null);
    }

    /** TX2 cuando el modelo respondio. */
    public void cerrarConExito(Integer inputTokens, Integer outputTokens, Integer latencyMs,
            String stopReason, String rawResponse, Clock clock) {
        exigirPendiente();
        validarMedidas(inputTokens, outputTokens, latencyMs);
        validarArcoDeModelo(turnType, modelId, promptVersion, inputTokens, outputTokens,
                rawResponse, stopReason);
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.latencyMs = latencyMs;
        this.stopReason = stopReason;
        this.rawResponse = rawResponse;
        this.failureCode = null;
        this.status = TurnStatus.SUCCEEDED;
        this.completedAt = LocalDateTime.now(clock);
    }

    /**
     * TX2 cuando no respondio, o cuando TX2 misma fallo despues de haber pagado la
     * llamada. El endpoint devuelve 200 con el modo degradado, no un 500: el
     * usuario no puede hacer nada con un error y ya se pago por leer la respuesta.
     */
    public void cerrarConFallo(String failureCode, Integer latencyMs, Clock clock) {
        exigirPendiente();
        if (failureCode == null || failureCode.isBlank())
            throw new IllegalArgumentException("a failed turn needs its failureCode");
        if (failureCode.length() > 40)
            throw new IllegalArgumentException("failureCode must be 40 chars or less");
        validarMedidas(null, null, latencyMs);
        this.latencyMs = latencyMs;
        this.failureCode = failureCode;
        this.status = TurnStatus.FAILED;
        this.completedAt = LocalDateTime.now(clock);
    }

    private void exigirPendiente() {
        if (status != TurnStatus.PENDING)
            throw new IllegalArgumentException("only a pending turn can be closed: " + status);
    }

    /**
     * Espejo de {@code chk_ai_proposal_turns_number} y
     * {@code ..._initial_is_first}.
     */
    private static void validarNumero(int turnNumber, TurnType turnType) {
        if (turnNumber < 1)
            throw new IllegalArgumentException("turnNumber must be at least 1");
        if (turnType == TurnType.MODEL_INITIAL && turnNumber != 1)
            throw new IllegalArgumentException("the initial turn is always number 1");
    }

    private static void validarTexto(String inputText, String clientRequestId) {
        if (inputText != null && inputText.length() > 2000)
            throw new IllegalArgumentException("inputText must be 2000 chars or less");
        if (clientRequestId != null && clientRequestId.length() > 64)
            throw new IllegalArgumentException("clientRequestId must be 64 chars or less");
    }

    /** Espejo de {@code chk_ai_proposal_turns_model_arc}. */
    @SuppressWarnings("java:S107")
    private static void validarArcoDeModelo(TurnType turnType, String modelId, String promptVersion,
            Integer inputTokens, Integer outputTokens, String rawResponse, String stopReason) {
        if (turnType.invocaAlModelo()) {
            if (modelId == null || modelId.isBlank())
                throw new IllegalArgumentException("a model turn needs its modelId");
            if (promptVersion == null || promptVersion.isBlank())
                throw new IllegalArgumentException("a model turn needs its promptVersion");
            return;
        }
        if (modelId != null || promptVersion != null || inputTokens != null || outputTokens != null
                || rawResponse != null || stopReason != null)
            throw new IllegalArgumentException("a customer edit never carries model output");
    }

    /** Espejo de {@code chk_ai_proposal_turns_tokens}. */
    private static void validarMedidas(Integer inputTokens, Integer outputTokens,
            Integer latencyMs) {
        if (inputTokens != null && inputTokens < 0)
            throw new IllegalArgumentException("inputTokens cannot be negative");
        if (outputTokens != null && outputTokens < 0)
            throw new IllegalArgumentException("outputTokens cannot be negative");
        if (latencyMs != null && latencyMs < 0)
            throw new IllegalArgumentException("latencyMs cannot be negative");
    }

    /** Espejo de {@code chk_ai_proposal_turns_closed} y {@code ..._failure}. */
    private static void validarCierre(TurnStatus status, String failureCode,
            LocalDateTime completedAt) {
        if (status != TurnStatus.PENDING && completedAt == null)
            throw new IllegalArgumentException("a closed turn needs its completedAt");
        if (status == TurnStatus.FAILED && (failureCode == null || failureCode.isBlank()))
            throw new IllegalArgumentException("a failed turn needs its failureCode");
        if (status != TurnStatus.FAILED && failureCode != null)
            throw new IllegalArgumentException("only a failed turn carries a failureCode");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProposalId() {
        return proposalId;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public TurnType getTurnType() {
        return turnType;
    }

    public TurnStatus getStatus() {
        return status;
    }

    public String getInputText() {
        return inputText;
    }

    public Integer getInputTextChars() {
        return inputTextChars;
    }

    public String getModelId() {
        return modelId;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public String getStopReason() {
        return stopReason;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public Long getVersion() {
        return version;
    }
}
