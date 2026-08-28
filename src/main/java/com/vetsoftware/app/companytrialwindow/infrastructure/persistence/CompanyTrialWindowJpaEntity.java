package com.vetsoftware.app.companytrialwindow.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fila del reloj de la empresa.
 *
 * <p>
 * <strong>{@code open_window_marker} no se mapea.</strong> Es una columna
 * generada {@code STORED} que la base calcula sola y que solo existe para
 * sostener {@code uq_company_trial_windows_open}. Mapearla haría que Hibernate
 * intentara escribirla y el {@code INSERT} moriría.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin borrado lógico</strong>, por decisión del
 * esquema: una ventana cerrada y una desactivada son cosas distintas, y
 * confundir las dos devolvería el derecho a probar a quien ya probó. Lo que hay
 * es {@code closed_at}.
 *
 * <p>
 * Las dos claves foráneas van como columnas planas y no como asociaciones: nada
 * de esta feature necesita el agregado de la empresa ni el de la cotización, y
 * colgar asociaciones metería sus grafos enteros en esta rodaja.
 */
@Entity
@Table(name = "company_trial_windows")
public class CompanyTrialWindowJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Último día en prueba, incluido. */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "window_days", nullable = false)
    private int windowDays;

    @Column(name = "source_quote_id", nullable = false)
    private Long sourceQuoteId;

    /** Vacío = ventana viva. */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CompanyTrialWindowJpaEntity() {
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(int windowDays) {
        this.windowDays = windowDays;
    }

    public Long getSourceQuoteId() {
        return sourceQuoteId;
    }

    public void setSourceQuoteId(Long sourceQuoteId) {
        this.sourceQuoteId = sourceQuoteId;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
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
