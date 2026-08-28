package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import com.vetsoftware.app.externalinvoicingoutage.domain.CauseParty;
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
 * {@code external_invoicing_outages} — cuando la emision fiscal se cae.
 *
 * <p>
 * <strong>Esta clase NO alcanza {@code CompanyJpaEntity} por ninguna
 * asociacion, y no es estetica.</strong> La tabla es global y sin columna de
 * empresa; el dia que alguien le cuelgue un {@code @ManyToOne} a companies
 * «para saber a quien afecto», las cuatro reglas duras de aislamiento de BE-COV
 * se activan sobre la feature entera y rompen el build. El reparto por clinica
 * vive en {@link ExternalInvoicingOutageCompanyJpaEntity}, que tampoco navega a
 * companies y por el mismo motivo.
 *
 * <p>
 * <strong>La columna GENERATED STORED no se mapea, a proposito.</strong>
 * {@code open_outage_marker} la calcula MySQL —vale {@code cause_party}
 * mientras {@code ended_at} es nulo y {@code NULL} en cuanto se cierra— y solo
 * existe para que {@code uq_eio_open} pueda imponer <b>una sola caida abierta
 * por causante</b>, algo que con {@code NULL} no se podia restringir: en un
 * indice unico dos vacios no chocan. Mapearla obligaria a
 * {@code insertable = false, updatable = false} y, peor, invitaria a escribirla
 * desde Java: el primer {@code INSERT} que llevara un valor propio para una
 * columna generada lo rechazaria el motor.
 *
 * <p>
 * <strong>Lleva {@code @Version}</strong> porque la tabla tiene la columna y
 * porque hay dos escrituras que editan la fila: el cierre y la marca de aviso.
 * Sin el, dos cierres concurrentes se pisarian y la hora en que volvio el
 * servicio —la que mide la duracion de la interrupcion— se perderia sin
 * excepcion y sin log.
 *
 * <p>
 * <strong>Sin {@code enabled}</strong>: es una bitacora probatoria y una prueba
 * que se puede desactivar no prueba nada. Por eso tampoco lleva
 * {@code @SQLDelete} —no hay borrado logico que acotar por version— y
 * {@code BORRADO_LOGICO_RESPETA_LA_VERSION} no tiene nada que mirar aqui.
 */
@Entity
@Table(name = "external_invoicing_outages")
public class ExternalInvoicingOutageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** Nulo mientras la caida sigue viva. Es lo que alimenta la generada. */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cause_party", nullable = false, length = 20)
    private CauseParty causeParty;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    @Column(name = "affected_company_count", nullable = false)
    private int affectedCompanyCount;

    @Column(name = "notified_companies_at")
    private LocalDateTime notifiedCompaniesAt;

    @Column(name = "external_incident_ref", length = 100)
    private String externalIncidentRef;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected ExternalInvoicingOutageJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public CauseParty getCauseParty() {
        return causeParty;
    }

    public void setCauseParty(CauseParty causeParty) {
        this.causeParty = causeParty;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getAffectedCompanyCount() {
        return affectedCompanyCount;
    }

    public void setAffectedCompanyCount(int affectedCompanyCount) {
        this.affectedCompanyCount = affectedCompanyCount;
    }

    public LocalDateTime getNotifiedCompaniesAt() {
        return notifiedCompaniesAt;
    }

    public void setNotifiedCompaniesAt(LocalDateTime notifiedCompaniesAt) {
        this.notifiedCompaniesAt = notifiedCompaniesAt;
    }

    public String getExternalIncidentRef() {
        return externalIncidentRef;
    }

    public void setExternalIncidentRef(String externalIncidentRef) {
        this.externalIncidentRef = externalIncidentRef;
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
