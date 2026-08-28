package com.vetsoftware.app.securityincident.infrastructure.persistence;

import com.vetsoftware.app.securityincident.domain.AffectedScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * {@code security_incident_companies} (changeset 357) — la puente de afectados.
 *
 * <h2>{@code company_id} es un escalar y NO un {@code @ManyToOne}</h2>
 *
 * <p>
 * <strong>Esa decision sostiene la feature entera.</strong> Si esta clase
 * alcanzara {@code CompanyJpaEntity} por una asociacion, las cuatro reglas
 * duras de BE-COV se activarian sobre <em>toda</em> la rodaja —incluida
 * {@code security_incidents}, que no tiene empresa que acotar— y romperian el
 * build. La clave foranea sigue existiendo y vigilando en la base
 * ({@code fk_sic_company}); lo que no existe es la navegacion desde Java, que
 * ademas no hace falta: la puente archiva la clinica por id y la respuesta no
 * publica ni su nombre.
 *
 * <p>
 * El incidente si va como {@code @ManyToOne(LAZY)} porque es de esta misma
 * rodaja y el mapper necesita su id; el {@code @EntityGraph} del repositorio
 * evita el N+1.
 *
 * <h2>Sin {@code @Version}, sin {@code created_date}, sin {@code enabled}</h2>
 *
 * <p>
 * La tabla no tiene ninguna de las tres columnas. Va exenta de bloqueo
 * optimista con el codigo {@code E2_TABLA_PUENTE}: se escribe una sola vez, al
 * cerrar el incidente, y ningun caso de uso la reescribe. La entrada en
 * {@code ENTIDADES_EXENTAS_DE_VERSION} es obligatoria o
 * {@code ENTIDADES_CON_BLOQUEO_OPTIMISTA} rompe el build.
 *
 * <h2>Sin {@code @SQLDelete} y sin borrado</h2>
 *
 * <p>
 * Quitar una clinica de la lista de afectados es destruir la prueba de que se
 * le notifico. Ni hay borrado logico, ni fisico, ni metodo en el puerto, ni
 * endpoint.
 */
@Entity
@Table(name = "security_incident_companies")
public class SecurityIncidentCompanyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_incident_id", nullable = false)
    private SecurityIncidentJpaEntity incident;

    /** Escalar a proposito: ver el javadoc de la clase. */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "affected_scope", nullable = false, length = 30)
    private AffectedScope affectedScope;

    /** Los titulares <b>de esa clinica</b>, no los del incidente entero. */
    @Column(name = "affected_subject_count", nullable = false)
    private int affectedSubjectCount;

    protected SecurityIncidentCompanyJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecurityIncidentJpaEntity getIncident() {
        return incident;
    }

    public void setIncident(SecurityIncidentJpaEntity incident) {
        this.incident = incident;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public AffectedScope getAffectedScope() {
        return affectedScope;
    }

    public void setAffectedScope(AffectedScope affectedScope) {
        this.affectedScope = affectedScope;
    }

    public int getAffectedSubjectCount() {
        return affectedSubjectCount;
    }

    public void setAffectedSubjectCount(int affectedSubjectCount) {
        this.affectedSubjectCount = affectedSubjectCount;
    }
}
