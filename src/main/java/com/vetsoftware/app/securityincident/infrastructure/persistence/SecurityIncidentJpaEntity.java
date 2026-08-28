package com.vetsoftware.app.securityincident.infrastructure.persistence;

import com.vetsoftware.app.securityincident.domain.IncidentSeverity;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentKind;
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

/**
 * {@code security_incidents} (changeset 356) — cuando algo se rompe y hay que
 * avisar.
 *
 * <p>
 * <strong>Esta clase NO alcanza {@code CompanyJpaEntity} por ninguna
 * asociacion, y no es estetica.</strong> La tabla no tiene columna de empresa:
 * el incidente es de la plataforma y el reparto por clinica vive en la puente.
 * El dia que alguien le cuelgue un {@code @ManyToOne} a companies, las cuatro
 * reglas duras de aislamiento de BE-COV se activan sobre la feature entera
 * —incluida esta tabla, que no tiene empresa que acotar— y rompen el build.
 *
 * <p>
 * <strong>Lleva {@code @Version}</strong> porque la tabla tiene la columna y
 * porque hay dos escrituras que editan la fila: el reporte y el cierre. Sin el,
 * dos operadores concurrentes se pisarian y la fecha que prueba que se aviso
 * dentro del plazo desapareceria sin excepcion y sin log.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @SQLDelete}</strong>: la tabla no
 * lleva marca de activo a proposito —una prueba que se puede desactivar no
 * prueba nada— y no hay borrado logico que escribir.
 */
@Entity
@Table(name = "security_incidents")
public class SecurityIncidentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    /**
     * El escalamiento interno. <strong>Obligatorio</strong>: es el punto desde el
     * que corren los quince dias habiles de la SIC, y sin el el vencimiento se
     * calcularia desde la deteccion y saldria mas largo del real.
     */
    @Column(name = "escalated_at", nullable = false)
    private LocalDateTime escalatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private SecurityIncidentKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private IncidentSeverity severity;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    @Column(name = "affected_subject_count", nullable = false)
    private int affectedSubjectCount;

    /**
     * El vencimiento como <b>dato</b>, no como formula: lo calcula el caso de uso
     * contra el calendario laboral al registrar, y no se recalcula al leer.
     */
    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Column(name = "reported_to_authority_at")
    private LocalDateTime reportedToAuthorityAt;

    @Column(name = "report_reference", length = 100)
    private String reportReference;

    @Column(name = "notified_subjects_at")
    private LocalDateTime notifiedSubjectsAt;

    /**
     * Las dos narraciones son {@code TEXT} en el esquema y llevan
     * {@code columnDefinition = "TEXT"} por el mismo motivo que
     * {@code LegalDocumentVersionJpaEntity.content} lleva el suyo: sin el,
     * Hibernate espera un {@code varchar(255)} para un {@code String} y
     * {@code ddl-auto:
     * validate} rechaza el arranque. {@code @Lob} tampoco vale —mapearia a
     * {@code longtext}, que es otro tipo—.
     */
    @Column(name = "containment", columnDefinition = "TEXT")
    private String containment;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected SecurityIncidentJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public SecurityIncidentKind getKind() {
        return kind;
    }

    public void setKind(SecurityIncidentKind kind) {
        this.kind = kind;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getAffectedSubjectCount() {
        return affectedSubjectCount;
    }

    public void setAffectedSubjectCount(int affectedSubjectCount) {
        this.affectedSubjectCount = affectedSubjectCount;
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(LocalDateTime deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public LocalDateTime getReportedToAuthorityAt() {
        return reportedToAuthorityAt;
    }

    public void setReportedToAuthorityAt(LocalDateTime reportedToAuthorityAt) {
        this.reportedToAuthorityAt = reportedToAuthorityAt;
    }

    public String getReportReference() {
        return reportReference;
    }

    public void setReportReference(String reportReference) {
        this.reportReference = reportReference;
    }

    public LocalDateTime getNotifiedSubjectsAt() {
        return notifiedSubjectsAt;
    }

    public void setNotifiedSubjectsAt(LocalDateTime notifiedSubjectsAt) {
        this.notifiedSubjectsAt = notifiedSubjectsAt;
    }

    public String getContainment() {
        return containment;
    }

    public void setContainment(String containment) {
        this.containment = containment;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
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
