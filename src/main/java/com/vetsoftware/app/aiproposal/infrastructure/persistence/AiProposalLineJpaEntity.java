package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import com.vetsoftware.app.aiproposal.domain.LineAction;
import com.vetsoftware.app.aiproposal.domain.LineSource;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Espejo de {@code ai_proposal_lines} (changeset 385).
 *
 * <p>
 * <strong>Con {@code @Version}, y no exenta.</strong> Con el borrado del motivo
 * en la retencion una linea si se toca una vez, asi que {@code E1_APPEND_ONLY}
 * seria falso por escrito. Sigue <strong>sin {@code enabled}</strong>: una
 * linea no se desactiva.
 *
 * <p>
 * <strong>Ninguna de sus dos FK es una asociacion.</strong> {@code turn_id} lo
 * fue en la primera version y {@code REPOS_CON_ENTITYGRAPH} lo marco como N+1;
 * la salida no fue anadir el {@code @EntityGraph} sino quitar el
 * {@code @ManyToOne}, porque esta es la tabla con mas filas de la feature y
 * nadie lee un campo del turno: un {@code JOIN} obligatorio en cada lectura de
 * lineas seria coste puro por un id que la propia columna ya guarda. La
 * integridad la sostiene {@code fk_ai_proposal_lines_turn} en la base.
 *
 * <p>
 * <strong>{@code catalog_item_id} es una columna suelta por el mismo motivo, y
 * uno propio.</strong> Es nulable a proposito -no se puede poner una FK a una
 * fila que no existe, y la alucinacion del modelo es el dato que mide su
 * calidad-, y colgar un {@code @ManyToOne} a {@code CatalogItemJpaEntity}
 * traeria el grafo del catalogo dentro de la rodaja mas caliente por N+1 sin
 * que esta feature lea un solo campo de el: aqui solo se guarda a que articulo
 * resolvio el codigo.
 *
 * <p>
 * {@code unit_amount} es {@code DECIMAL(19,2)}, la misma precision que
 * {@code catalog_prices.unit_amount}: congela el importe mostrado. Es un
 * registro de auditoria, no una foto que cambia sola cuando se publica una
 * tarifa nueva.
 */
@Entity
@Table(name = "ai_proposal_lines")
public class AiProposalLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turn_id", nullable = false)
    private Long turnId;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "catalog_item_id")
    private Long catalogItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private LineAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private LineSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 30)
    private LineVerdict verdict;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_amount", precision = 19, scale = 2)
    private BigDecimal unitAmount;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "reason_redacted_at")
    private LocalDateTime reasonRedactedAt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected AiProposalLineJpaEntity() {
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

    public void setTurnId(Long turnId) {
        this.turnId = turnId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public LineAction getAction() {
        return action;
    }

    public void setAction(LineAction action) {
        this.action = action;
    }

    public LineSource getSource() {
        return source;
    }

    public void setSource(LineSource source) {
        this.source = source;
    }

    public LineVerdict getVerdict() {
        return verdict;
    }

    public void setVerdict(LineVerdict verdict) {
        this.verdict = verdict;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitAmount() {
        return unitAmount;
    }

    public void setUnitAmount(BigDecimal unitAmount) {
        this.unitAmount = unitAmount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getReasonRedactedAt() {
        return reasonRedactedAt;
    }

    public void setReasonRedactedAt(LocalDateTime reasonRedactedAt) {
        this.reasonRedactedAt = reasonRedactedAt;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
