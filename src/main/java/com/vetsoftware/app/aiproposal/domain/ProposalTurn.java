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

    /**
     * Lo que admite {@code ai_proposal_turns.stop_reason}. Ver
     * {@link #validarStopReason}.
     */
    private static final int MAX_STOP_REASON = 30;

    private Long id;
    private final Long proposalId;
    private final int turnNumber;
    private final TurnType turnType;
    private TurnStatus status;
    private final String inputText;
    private final Integer inputTextChars;
    private String modelId;
    private String promptVersion;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer latencyMs;
    private String stopReason;
    private String rawResponse;
    private String failureCode;
    private ProposalPresentation presentation;
    private final String clientRequestId;
    private final LocalDateTime createdDate;
    private LocalDateTime completedAt;
    private Long version;

    @SuppressWarnings("java:S107")
    public ProposalTurn(Long id, Long proposalId, int turnNumber, TurnType turnType,
            TurnStatus status, String inputText, Integer inputTextChars, String modelId,
            String promptVersion, Integer inputTokens, Integer outputTokens, Integer latencyMs,
            String stopReason, String rawResponse, String failureCode,
            ProposalPresentation presentation, String clientRequestId, LocalDateTime createdDate,
            LocalDateTime completedAt, Long version) {
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
        validarStopReason(stopReason);
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
        this.presentation = presentation;
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
                null, null, null, null, null, null, null, clientRequestId, LocalDateTime.now(clock),
                null, null);
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
                null, clientRequestId, ahora, ahora, null);
    }

    /** TX2 cuando el modelo respondio. */
    public void cerrarConExito(Integer inputTokens, Integer outputTokens, Integer latencyMs,
            String stopReason, String rawResponse, Clock clock) {
        exigirPendiente();
        validarMedidas(inputTokens, outputTokens, latencyMs);
        validarStopReason(stopReason);
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

    /**
     * &#9940; <strong>El modelo que DE VERDAD respondio, no el que estaba
     * configurado cuando se abrio el turno.</strong>
     *
     * <p>
     * TX1 escribe {@code model_id} con el valor de la configuracion —tiene que
     * escribir algo: {@code chk_ai_proposal_turns_model_arc} lo exige NOT NULL en
     * un turno de modelo, y todavia no se ha llamado a nadie—. Pero quien contesta
     * es el adaptador, y nada obliga a que sea el mismo: un alias de inferencia que
     * enruta a otra version, un <em>fallback</em> del proveedor o un despliegue que
     * cambia la propiedad entre TX1 y TX2 dejan el turno <strong>afirmando por
     * escrito una falsedad</strong>. Hoy los dos valores coinciden, y por eso el
     * defecto es invisible: el dia que dejen de coincidir, la columna con la que se
     * compara la calidad y el coste entre modelos estara atribuyendo la respuesta
     * de uno a otro, y no habra forma de saber desde cuando.
     *
     * <p>
     * <strong>Sin efecto si el argumento no dice nada.</strong> Un {@code null} o
     * un blanco dejan lo que ya habia: el arco del {@code CHECK} sigue exigiendo
     * las dos columnas y borrarlas convertiria una imprecision en una violacion de
     * esquema.
     */
    public void registrarModeloQueRespondio(String modelId, String promptVersion) {
        if (!turnType.invocaAlModelo())
            throw new IllegalArgumentException("a customer edit never carries model output");
        if (modelId != null && !modelId.isBlank())
            this.modelId = modelId;
        if (promptVersion != null && !promptVersion.isBlank())
            this.promptVersion = promptVersion;
    }

    /**
     * &#9888; <strong>El estado de pantalla, persistido.</strong> Se calcula al
     * responder y hasta ahora no se guardaba, asi que una relectura solo podia
     * deducirlo de si quedaba alguna linea aceptada: eso separa
     * {@link ProposalPresentation#OUT_OF_DOMAIN} de todo lo demas y
     * <strong>funde</strong> {@link ProposalPresentation#NOT_UNDERSTOOD} con
     * {@link ProposalPresentation#DETERMINISTIC} y con
     * {@link ProposalPresentation#PROPOSAL}, porque las tres escriben el carrito
     * determinista. El prospecto que volvia por su enlace veia otra pantalla que la
     * que le contesto.
     */
    public void registrarPantalla(ProposalPresentation presentation) {
        this.presentation = presentation;
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

    /**
     * ⛔ <strong>Espejo de la columna {@code stop_reason VARCHAR(30)}, y la razon
     * por la que existe es dinero.</strong> El motivo de parada lo elige el
     * proveedor de un vocabulario que crece solo: el mas largo que hoy declara el
     * SDK de Bedrock es {@code MODEL_CONTEXT_WINDOW_EXCEEDED}, <em>29
     * caracteres</em>, a uno del limite. Sin esta guarda, un valor nuevo un poco
     * mas largo —o el vocabulario propio de otra familia de modelo— reventaria el
     * {@code INSERT} de TX2 <strong>despues de que la llamada al modelo se
     * cobro</strong>: se paga el modelo y se pierde el turno entero, con el carrito
     * y todo.
     *
     * <p>
     * <strong>Aqui se rechaza, en la costura se acota</strong>, y las dos cosas son
     * correctas. {@code BedrockModelInvoker.parada} recorta lo que viene de fuera y
     * lo cuenta en el log, que es lo unico que no cuesta la llamada. Para cuando el
     * valor llega hasta aqui ya paso por ahi, asi que un valor largo en este punto
     * solo puede ser un invocador futuro que se salto ese paso —un error de
     * programacion, no un dato del proveedor— y esos fallan alto, igual que
     * {@code inputText} con sus 2000.
     */
    private static void validarStopReason(String stopReason) {
        if (stopReason != null && stopReason.length() > MAX_STOP_REASON)
            throw new IllegalArgumentException("stopReason must be 30 chars or less");
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

    public ProposalPresentation getPresentation() {
        return presentation;
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
