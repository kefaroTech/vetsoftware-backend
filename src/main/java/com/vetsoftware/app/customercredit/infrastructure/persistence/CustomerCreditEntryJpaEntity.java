package com.vetsoftware.app.customercredit.infrastructure.persistence;

import com.vetsoftware.app.customercredit.domain.CreditEntryKind;
import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code customer_credit_entries} — el libro del saldo a favor.
 *
 * <p>
 * <strong>Sin {@code @Version} y sin {@code enabled}</strong>
 * ({@code E1_APPEND_ONLY}): el libro del saldo a favor es estrictamente de solo
 * anadir —el saldo no se guarda, se suma— y corregir un asiento es escribir
 * otro que lo compensa. No hay dos ediciones simultaneas que puedan pisarse
 * porque no hay ninguna edicion; y un asiento que se pudiera desactivar haria
 * el saldo irreconstruible, que es justo lo que esta tabla vino a impedir.
 *
 * <p>
 * <strong>Las claves foraneas van como escalares y no como
 * asociaciones.</strong> Las cuatro son compuestas {@code (company_id, id)} —el
 * esquema arrastra la empresa dentro de la clave para que la base <em>rechace
 * la fila</em> cuando un asiento de una clinica apunta al pago de otra— y
 * mapearlas con {@code @ManyToOne} obliga a un {@code @JoinColumns} donde
 * {@code company_id} se comparte entre todas: Hibernate exige que todas las
 * columnas de una propiedad compartan modo de escritura, y solo un mapeo puede
 * ser dueno de una columna fisica. La mezcla no falla en esta clase sino en el
 * {@code entityManagerFactory}, asi que se lleva por delante la aplicacion
 * entera sin senalar aqui — es la trampa que documenta
 * {@code BillingDocumentApplicationJpaEntity}. Este slice no navega ninguna de
 * las cuatro asociaciones: resuelve lo que necesita por puerto, asi que el
 * escalar es todo lo que hace falta y de paso no puede producir un N+1.
 *
 * <p>
 * <strong>{@code origin_marker} no se mapea a proposito.</strong> Es una
 * columna {@code GENERATED ALWAYS AS ... STORED} que sostiene
 * {@code uq_cce_origin}: la calcula el motor, y mapearla haria que Hibernate
 * intentara escribirla. Que no este aqui no la desactiva — la unicidad sigue
 * viva en la base, que es donde tiene que estar.
 */
@Entity
@Table(name = "customer_credit_entries")
public class CustomerCreditEntryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_kind", nullable = false, length = 20)
    private CreditEntryKind entryKind;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Autorreferencia al asiento de alta del que sale este consumo. */
    @Column(name = "lot_entry_id")
    private Long lotEntryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_kind", nullable = false, length = 20)
    private CreditOriginKind originKind;

    @Column(name = "origin_payment_id")
    private Long originPaymentId;

    @Column(name = "origin_document_id")
    private Long originDocumentId;

    @Column(name = "origin_subscription_id")
    private Long originSubscriptionId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    /**
     * Obligatoria: la columna es {@code NOT NULL} y la respalda
     * {@code uq_cce_idempotency}.
     */
    @Column(name = "client_request_id", nullable = false, length = 64)
    private String clientRequestId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected CustomerCreditEntryJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public CreditEntryKind getEntryKind() {
        return entryKind;
    }

    public void setEntryKind(CreditEntryKind entryKind) {
        this.entryKind = entryKind;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getLotEntryId() {
        return lotEntryId;
    }

    public void setLotEntryId(Long lotEntryId) {
        this.lotEntryId = lotEntryId;
    }

    public CreditOriginKind getOriginKind() {
        return originKind;
    }

    public void setOriginKind(CreditOriginKind originKind) {
        this.originKind = originKind;
    }

    public Long getOriginPaymentId() {
        return originPaymentId;
    }

    public void setOriginPaymentId(Long originPaymentId) {
        this.originPaymentId = originPaymentId;
    }

    public Long getOriginDocumentId() {
        return originDocumentId;
    }

    public void setOriginDocumentId(Long originDocumentId) {
        this.originDocumentId = originDocumentId;
    }

    public Long getOriginSubscriptionId() {
        return originSubscriptionId;
    }

    public void setOriginSubscriptionId(Long originSubscriptionId) {
        this.originSubscriptionId = originSubscriptionId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public void setValueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
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
}
