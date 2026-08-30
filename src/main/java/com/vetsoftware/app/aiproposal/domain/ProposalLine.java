package com.vetsoftware.app.aiproposal.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Una linea persistida de la propuesta (tabla {@code ai_proposal_lines},
 * changeset 385).
 *
 * <p>
 * <strong>{@code itemCode} se guarda verbatim y {@code catalogItemId} es
 * nulable</strong>: no se puede poner una FK a una fila que no existe, y la
 * alucinacion del modelo ({@code UNKNOWN_CODE}) es precisamente el dato que
 * mide su calidad. El unico de la tabla va sobre {@code (turn_id, item_code)} y
 * no sobre el id: con el id la restriccion no valdria nada para las
 * alucinaciones, porque MySQL admite multiples {@code NULL} en un indice unico
 * y el mismo codigo inventado podria repetirse veinte veces en el mismo turno.
 *
 * <p>
 * ⛔ <strong>El veredicto no sale nunca por HTTP.</strong> Ver
 * {@link LineVerdict}.
 *
 * <p>
 * <strong>{@code reasonRedactedAt} es la correccion que hacia falsa la
 * anonimizacion de toda la feature.</strong> El prompt obliga al modelo a citar
 * al cliente -"le vendes a credito a una fundacion que paga a fin de mes"-, asi
 * que {@code reason} guarda las palabras del prospecto: una fila marcada
 * {@code anonymized_at} seguia llevandolas dentro mientras el informe de
 * cumplimiento la daba por limpia. La marca va <strong>en la propia
 * linea</strong> y no se lee {@code ai_proposals.anonymized_at}, porque MySQL
 * no permite que un {@code CHECK} referencie otra tabla. Se rechazo el
 * centinela ({@code reason = '[anonimizado]'}): es un valor magico que
 * cualquier {@code COUNT} sobre motivos contaria como motivo.
 */
public class ProposalLine {

    private Long id;
    private final Long turnId;
    private final String itemCode;
    private final Long catalogItemId;
    private final LineAction action;
    private final LineSource source;
    private final LineVerdict verdict;
    private final int quantity;
    private final BigDecimal unitAmount;
    private String reason;
    private LocalDateTime reasonRedactedAt;
    private final int sortOrder;
    private final LocalDateTime createdDate;
    private Long version;

    @SuppressWarnings("java:S107")
    public ProposalLine(Long id, Long turnId, String itemCode, Long catalogItemId,
            LineAction action, LineSource source, LineVerdict verdict, int quantity,
            BigDecimal unitAmount, String reason, LocalDateTime reasonRedactedAt, int sortOrder,
            LocalDateTime createdDate, Long version) {
        if (turnId == null)
            throw new IllegalArgumentException("turnId is required");
        validarCodigo(itemCode, quantity, sortOrder);
        if (action == null)
            throw new IllegalArgumentException("action is required: " + itemCode);
        if (source == null)
            throw new IllegalArgumentException("source is required: " + itemCode);
        if (verdict == null)
            throw new IllegalArgumentException("verdict is required: " + itemCode);
        validarImporte(unitAmount, itemCode);
        validarResolucion(verdict, catalogItemId, itemCode);
        validarMotivo(source, reason, reasonRedactedAt, itemCode);
        this.id = id;
        this.turnId = turnId;
        this.itemCode = itemCode;
        this.catalogItemId = catalogItemId;
        this.action = action;
        this.source = source;
        this.verdict = verdict;
        this.quantity = quantity;
        this.unitAmount = unitAmount;
        this.reason = reason;
        this.reasonRedactedAt = reasonRedactedAt;
        this.sortOrder = sortOrder;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * El puente entre el motor puro y la persistencia. {@code catalogItemId} entra
     * por parametro y no lo lleva la {@link CartLine}: el motor decide con codigos
     * y es quien consulta el catalogo quien conoce los ids, que es lo que mantiene
     * a {@link ProposalCart} sin una sola dependencia de infraestructura.
     */
    public static ProposalLine de(CartLine linea, Long turnId, Long catalogItemId, Clock clock) {
        if (linea == null)
            throw new IllegalArgumentException("cart line is required");
        return new ProposalLine(null, turnId, linea.code(), catalogItemId, LineAction.ADDED,
                linea.source(), linea.verdict(), linea.quantity(), linea.unitAmount(),
                linea.reason(), null, linea.sortOrder(), LocalDateTime.now(clock), null);
    }

    /**
     * Paso 3 del barrido de retencion. Deja el motivo a {@code NULL} y escribe la
     * marca: las dos cosas a la vez, porque {@code chk_ai_proposal_lines_redaccion}
     * exige que no coexistan y {@code chk_ai_proposal_lines_model_reason} exige que
     * una linea de modelo tenga motivo <em>o conste que se le borro</em>.
     */
    public void redactarMotivo(Clock clock) {
        this.reason = null;
        this.reasonRedactedAt = LocalDateTime.now(clock);
    }

    public boolean tieneMotivoBorrado() {
        return reasonRedactedAt != null;
    }

    private static void validarCodigo(String itemCode, int quantity, int sortOrder) {
        if (itemCode == null || itemCode.isBlank())
            throw new IllegalArgumentException("itemCode is required");
        if (itemCode.length() > 50)
            throw new IllegalArgumentException("itemCode must be 50 chars or less: " + itemCode);
        if (quantity < 1)
            throw new IllegalArgumentException("quantity must be at least 1: " + itemCode);
        if (sortOrder < 0)
            throw new IllegalArgumentException("sortOrder cannot be negative: " + itemCode);
    }

    /** Espejo de {@code chk_ai_proposal_lines_amount}. */
    private static void validarImporte(BigDecimal unitAmount, String itemCode) {
        if (unitAmount != null && unitAmount.signum() < 0)
            throw new IllegalArgumentException("unitAmount cannot be negative: " + itemCode);
    }

    /** Espejo de {@code chk_ai_proposal_lines_resolved}. */
    private static void validarResolucion(LineVerdict verdict, Long catalogItemId,
            String itemCode) {
        if (verdict.esAceptado() && catalogItemId == null)
            throw new IllegalArgumentException(
                    "an accepted line must resolve to a catalog item: " + itemCode);
    }

    /**
     * Espejo de {@code chk_ai_proposal_lines_model_reason} y
     * {@code chk_ai_proposal_lines_redaccion}.
     */
    private static void validarMotivo(LineSource source, String reason,
            LocalDateTime reasonRedactedAt, String itemCode) {
        if (reason != null && reason.length() > 500)
            throw new IllegalArgumentException("reason must be 500 chars or less: " + itemCode);
        if (reasonRedactedAt != null && reason != null)
            throw new IllegalArgumentException(
                    "a redacted line cannot keep its reason: " + itemCode);
        if (source.exigeMotivo() && reason == null && reasonRedactedAt == null)
            throw new IllegalArgumentException(
                    "a model line needs a reason, or the record that it was erased: " + itemCode);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTurnId() {
        return turnId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public LineAction getAction() {
        return action;
    }

    public LineSource getSource() {
        return source;
    }

    public LineVerdict getVerdict() {
        return verdict;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitAmount() {
        return unitAmount;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getReasonRedactedAt() {
        return reasonRedactedAt;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
