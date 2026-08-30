package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import com.vetsoftware.app.aiproposal.domain.TurnStatus;
import com.vetsoftware.app.aiproposal.domain.TurnType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Espejo de {@code ai_proposal_turns} (changeset 384).
 *
 * <p>
 * <strong>Con {@code @Version}, y no exenta.</strong> El turno se escribe una
 * vez y se actualiza una vez ({@code PENDING} →
 * {@code SUCCEEDED}/{@code FAILED}), asi que {@code E1_APPEND_ONLY} seria falso
 * por escrito y {@code EXENCIONES_DE_VERSION_AL_DIA} existe justo para que el
 * repositorio no afirme algo que no es. Ademas hace legal bajo
 * {@code UPDATE_MASIVO_MUEVE_LA_VERSION} el {@code UPDATE} masivo del barrido
 * de retencion, que lleva {@code version = version + 1} en su {@code SET}.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @SQLDelete}</strong>: un turno no se
 * desactiva. La columna no existe en la tabla, y por eso
 * {@code BORRADO_LOGICO_RESPETA_LA_VERSION} no tiene nada que mirar aqui.
 *
 * <p>
 * <strong>{@code proposal_id} es una columna suelta, no un
 * {@code @ManyToOne}.</strong> La primera version si colgaba la asociacion, y
 * {@code REPOS_CON_ENTITYGRAPH} la marco: un finder que devuelve una entidad
 * con {@code @ManyToOne} y sin {@code @EntityGraph} es N+1. La salida no fue
 * anadir el {@code @EntityGraph} sino <strong>quitar la asociacion</strong>,
 * porque nadie lee un solo campo del padre -el dominio guarda
 * {@code proposalId} y el mapper solo necesitaba el id, que la columna FK ya
 * tiene-. Un {@code JOIN} obligatorio a la cabecera en cada lectura de turnos
 * seria coste puro, y la integridad la sigue garantizando
 * {@code fk_ai_proposal_turns_proposal} en la base. Mismo criterio que
 * {@code PriceListJpaEntity.published_by_system_user_id}.
 *
 * <p>
 * {@code raw_response} es {@code JSON} en el esquema y por eso lleva
 * {@code @JdbcTypeCode(SqlTypes.JSON)} -mismo patron que
 * {@code CompanyEntitlementSnapshotJpaEntity.payload}-. Es evidencia de corta
 * vida: no se indexa y el barrido de retencion la borra. El <strong>prompt
 * renderizado</strong> (~16 KB por turno) deliberadamente no se guarda: se
 * reconstruye con {@code prompt_version} mas
 * {@code ai_proposals.catalog_snapshot_hash}.
 */
@Entity
@Table(name = "ai_proposal_turns")
public class AiProposalTurnJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proposal_id", nullable = false)
    private Long proposalId;

    @Column(name = "turn_number", nullable = false)
    private int turnNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "turn_type", nullable = false, length = 20)
    private TurnType turnType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TurnStatus status;

    @Column(name = "input_text", length = 2000)
    private String inputText;

    @Column(name = "input_text_chars")
    private Integer inputTextChars;

    @Column(name = "model_id", length = 120)
    private String modelId;

    @Column(name = "prompt_version", length = 20)
    private String promptVersion;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "stop_reason", length = 30)
    private String stopReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response")
    private String rawResponse;

    @Column(name = "failure_code", length = 40)
    private String failureCode;

    @Column(name = "client_request_id", length = 64)
    private String clientRequestId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected AiProposalTurnJpaEntity() {
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

    public void setProposalId(Long proposalId) {
        this.proposalId = proposalId;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void setTurnNumber(int turnNumber) {
        this.turnNumber = turnNumber;
    }

    public TurnType getTurnType() {
        return turnType;
    }

    public void setTurnType(TurnType turnType) {
        this.turnType = turnType;
    }

    public TurnStatus getStatus() {
        return status;
    }

    public void setStatus(TurnStatus status) {
        this.status = status;
    }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public Integer getInputTextChars() {
        return inputTextChars;
    }

    public void setInputTextChars(Integer inputTextChars) {
        this.inputTextChars = inputTextChars;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getStopReason() {
        return stopReason;
    }

    public void setStopReason(String stopReason) {
        this.stopReason = stopReason;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
