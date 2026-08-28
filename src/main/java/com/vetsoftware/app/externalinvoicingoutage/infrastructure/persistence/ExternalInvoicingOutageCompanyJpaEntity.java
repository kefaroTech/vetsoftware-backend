package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import com.vetsoftware.app.externalinvoicingoutage.domain.OutageResolution;
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
 * {@code external_invoicing_outage_companies} — el reparto de una caida por
 * clinica.
 *
 * <p>
 * <strong>{@code company_id} es un escalar {@code Long} y NUNCA un
 * {@code @ManyToOne} a {@code CompanyJpaEntity}, y esa es la decision de diseno
 * mas cargada de este fichero.</strong> Colgar la asociacion haria que esta
 * entidad <em>alcanzara</em> {@code CompanyJpaEntity}, y con ella la feature
 * entera: las cuatro reglas duras de BE-COV
 * ({@code TENANT_DEFENSA_EN_PROFUNDIDAD},
 * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA},
 * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM},
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}) se activarian sobre
 * {@link ExternalInvoicingOutageJpaEntity} tambien, que no tiene empresa que
 * acotar, y romperian el build. Es el aviso literal del changeset 354 y de la
 * seccion 4 de la especificacion.
 *
 * <p>
 * Y no se pierde nada: la clave foranea {@code fk_eioc_company} sigue
 * existiendo y vigilando en la base; lo que no existe es la navegacion desde
 * Java. La puente no necesita un solo campo de la empresa —ni el nombre ni el
 * NIT entran aqui—, asi que la asociacion solo traeria el grafo de
 * {@code companies} para no usarlo. La existencia se comprueba con
 * {@link com.vetsoftware.app.externalinvoicingoutage.application.port.out.CompanyValidationPort}.
 *
 * <p>
 * <strong>La caida SI va como {@code @ManyToOne(LAZY)}</strong> —es de la misma
 * feature, no hay cruce que evitar— y por eso el repositorio declara
 * {@code @EntityGraph} en sus lecturas: sin el, listar el reparto de una caida
 * dispara una consulta por fila.
 *
 * <p>
 * <strong>Sin {@code @Version}, sin {@code created_date} y sin
 * {@code enabled}</strong>, porque la tabla no tiene ninguna de las tres
 * columnas. Se escribe una sola vez al repartir y ningun caso de uso la
 * reescribe: es la exencion {@code E2_TABLA_PUENTE} de
 * {@code ENTIDADES_EXENTAS_DE_VERSION}.
 *
 * <p>
 * <strong>Sin {@code @SQLDelete} y sin borrado de ningun tipo.</strong> Quitar
 * una clinica del reparto destruye la prueba de que se le aviso y de por que
 * uso numeracion de contingencia. Las tres puertas van cerradas —entidad,
 * puerto y controller—, no una.
 */
@Entity
@Table(name = "external_invoicing_outage_companies")
public class ExternalInvoicingOutageCompanyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outage_id", nullable = false)
    private ExternalInvoicingOutageJpaEntity outage;

    /** Escalar a proposito. Ver el javadoc de la clase. */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "failed_document_count", nullable = false)
    private int failedDocumentCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_by", nullable = false, length = 25)
    private OutageResolution resolvedBy;

    protected ExternalInvoicingOutageCompanyJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExternalInvoicingOutageJpaEntity getOutage() {
        return outage;
    }

    public void setOutage(ExternalInvoicingOutageJpaEntity outage) {
        this.outage = outage;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public int getFailedDocumentCount() {
        return failedDocumentCount;
    }

    public void setFailedDocumentCount(int failedDocumentCount) {
        this.failedDocumentCount = failedDocumentCount;
    }

    public OutageResolution getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(OutageResolution resolvedBy) {
        this.resolvedBy = resolvedBy;
    }
}
