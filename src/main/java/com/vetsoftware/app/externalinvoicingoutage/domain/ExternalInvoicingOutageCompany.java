package com.vetsoftware.app.externalinvoicingoutage.domain;

/**
 * Una clinica alcanzada por una caida: cuantos documentos se le quedaron sin
 * transmitir y como salio adelante.
 *
 * <p>
 * <strong>{@code failedDocumentCount} es el numero que sostiene la
 * reclamacion.</strong> Sin el, «nos afecto la caida» es una afirmacion; con
 * el, es una cifra que se coteja contra la del proveedor.
 *
 * <p>
 * <strong>Una fila por clinica y caida</strong> ({@code uq_eioc_pair}), sin
 * ambito que multiplique: al contrario que en la puente de incidentes de
 * seguridad, aqui una caida alcanza a una clinica de una sola forma.
 *
 * <p>
 * <strong>No se borra.</strong> Ni el puerto expone {@code delete}, ni la
 * entidad lleva {@code @SQLDelete}, ni el controller tiene endpoint: quitar una
 * clinica de la lista de alcanzadas es destruir la prueba de que se le aviso y
 * de por que uso numeracion de contingencia. Por lo mismo la fila no lleva
 * {@code enabled}: una prueba que se puede desactivar no prueba nada.
 *
 * <p>
 * <strong>La empresa viaja como identificador y no como entidad.</strong> Ver
 * {@code ExternalInvoicingOutageCompanyJpaEntity} para el porque completo:
 * colgar un {@code @ManyToOne} a {@code CompanyJpaEntity} activaria las cuatro
 * reglas duras de BE-COV sobre la feature entera.
 */
public class ExternalInvoicingOutageCompany {

    private final Long id;
    private final Long outageId;
    private final Long companyId;
    private final int failedDocumentCount;
    private final OutageResolution resolvedBy;

    public ExternalInvoicingOutageCompany(Long id, Long outageId, Long companyId,
            int failedDocumentCount, OutageResolution resolvedBy) {
        if (outageId == null)
            throw new IllegalArgumentException("outageId is required");
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (failedDocumentCount < 0)
            throw new IllegalArgumentException("failedDocumentCount must not be negative");
        if (resolvedBy == null)
            throw new IllegalArgumentException("resolvedBy is required");
        this.id = id;
        this.outageId = outageId;
        this.companyId = companyId;
        this.failedDocumentCount = failedDocumentCount;
        this.resolvedBy = resolvedBy;
    }

    /**
     * Registra a una clinica alcanzada.
     *
     * <p>
     * Cero documentos fallidos es legitimo y por eso la comprobacion es {@code < 0}
     * y no {@code <= 0}: una clinica puede estar dentro del alcance de la caida sin
     * haber intentado emitir nada en esa franja, y dejarla fuera del reparto la
     * borraria del expediente.
     */
    public static ExternalInvoicingOutageCompany register(Long outageId, Long companyId,
            int failedDocumentCount, OutageResolution resolvedBy) {
        return new ExternalInvoicingOutageCompany(null, outageId, companyId, failedDocumentCount,
                resolvedBy);
    }

    /** Si salio con numeracion de contingencia, que es lo que hay que demostrar. */
    public boolean usedContingencyNumbering() {
        return resolvedBy == OutageResolution.CONTINGENCY_NUMBERING;
    }

    public Long getId() {
        return id;
    }

    public Long getOutageId() {
        return outageId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public int getFailedDocumentCount() {
        return failedDocumentCount;
    }

    public OutageResolution getResolvedBy() {
        return resolvedBy;
    }
}
