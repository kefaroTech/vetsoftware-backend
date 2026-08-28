package com.vetsoftware.app.accountingexport.infrastructure.persistence;

import com.vetsoftware.app.accountingexport.domain.AccountingExportKind;
import com.vetsoftware.app.accountingexport.domain.AccountingExportStatus;
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
 * {@code accounting_exports} (changeset 345) — el asiento resumen mensual.
 *
 * <p>
 * <strong>Esta clase NO alcanza {@code CompanyJpaEntity} por ninguna
 * asociacion.</strong> La tabla es global; el dia que alguien le cuelgue un
 * {@code @ManyToOne} a companies, las cuatro reglas duras de aislamiento de
 * BE-COV se activan sobre la feature entera y rompen el build.
 *
 * <p>
 * <strong>Las dos claves foraneas van como escalares</strong>
 * ({@code period_key} contra {@code accounting_periods} y
 * {@code generated_by_system_user_id} contra {@code system_users}). Un
 * {@code @ManyToOne} traeria dos grafos ajenos y obligaria a un
 * {@code @EntityGraph} en cada finder para evitar el N+1; las claves siguen
 * vigilando en la base.
 *
 * <p>
 * <strong>{@code current_export_marker} no se mapea, a proposito.</strong> Es
 * {@code GENERATED ALWAYS … STORED}: la calcula MySQL y existe para que
 * {@code uq_accounting_exports_current} pueda restringir «un solo fichero vivo
 * por mes y clase». Mapearla invitaria a escribirla desde Java, y el primer
 * {@code INSERT} que llevara un valor propio para una columna generada lo
 * rechazaria el motor con el error 3105.
 *
 * <p>
 * <strong>Sin {@code enabled} y con {@code @Version}</strong>: es un documento
 * de dinero —deshabilitarlo lo sacaria de los totales sin rastro contable— pero
 * <em>si</em> se reescribe, porque recibe su desenlace despues. Dos operadores
 * que resuelvan el mismo fichero a la vez no se pisan gracias a esa columna.
 */
@Entity
@Table(name = "accounting_exports")
public class AccountingExportJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * {@code CHAR(7)} sin {@code columnDefinition}, igual que
     * {@code AccountingPeriodJpaEntity.periodKey}: su juego de caracteres
     * {@code ascii} y su colacion {@code ascii_bin} los fija el changeset con un
     * {@code MODIFY COLUMN}, y declararlos otra vez aqui duplicaria la decision en
     * dos sitios que pueden divergir.
     */
    @Column(name = "period_key", nullable = false, length = 7)
    private String periodKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_kind", nullable = false, length = 25)
    private AccountingExportKind exportKind;

    /**
     * {@code INT} en el esquema, {@code int} aqui: los tipos tienen que coincidir.
     */
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountingExportStatus status;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "generated_by_system_user_id", nullable = false)
    private Long generatedBySystemUserId;

    @Column(name = "total_debit", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDebit;

    @Column(name = "total_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCredit;

    /**
     * {@code columnDefinition = "char(64)"} explicito: la columna es {@code CHAR} y
     * sin el Hibernate espera un {@code varchar}. Mismo patron que
     * {@code LegalDocumentVersionJpaEntity.contentHash} y que
     * {@code PriceListJpaEntity.currency}. Con {@code ddl-auto: validate} el
     * desajuste no falla en esta rodaja: impide construir el
     * {@code SessionFactory}.
     */
    @Column(name = "totals_hash", nullable = false, columnDefinition = "char(64)")
    private String totalsHash;

    @Column(name = "file_ref", nullable = false, length = 255)
    private String fileRef;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected AccountingExportJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public AccountingExportKind getExportKind() {
        return exportKind;
    }

    public void setExportKind(AccountingExportKind exportKind) {
        this.exportKind = exportKind;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public AccountingExportStatus getStatus() {
        return status;
    }

    public void setStatus(AccountingExportStatus status) {
        this.status = status;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Long getGeneratedBySystemUserId() {
        return generatedBySystemUserId;
    }

    public void setGeneratedBySystemUserId(Long generatedBySystemUserId) {
        this.generatedBySystemUserId = generatedBySystemUserId;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
    }

    public String getTotalsHash() {
        return totalsHash;
    }

    public void setTotalsHash(String totalsHash) {
        this.totalsHash = totalsHash;
    }

    public String getFileRef() {
        return fileRef;
    }

    public void setFileRef(String fileRef) {
        this.fileRef = fileRef;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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
