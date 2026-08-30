package com.vetsoftware.app.aiproposal.domain;

/**
 * Estado de la propuesta, espejo de {@code chk_ai_proposals_status} (changeset
 * 383).
 *
 * <p>
 * {@code CONVERTED} lo escribe la rodaja {@code company} cuando nace la empresa
 * -el puente vive alli y no aqui (plan S5.1): una sola entidad de esta rodaja
 * que alcance {@code CompanyJpaEntity} enciende las cuatro reglas duras de
 * BE-COV sobre todos sus puertos-.
 */
public enum ProposalStatus {

    DRAFT,

    PROPOSED,

    ABANDONED,

    CONVERTED,

    EXPIRED
}
