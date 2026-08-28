package com.vetsoftware.app.externalinvoicingoutage.domain;

/**
 * Esa clinica ya estaba en el reparto de esa caida. <b>409</b>: lo impone
 * {@code uq_eioc_pair}, y aqui se traduce a un choque legible en vez de a una
 * violacion de integridad cruda.
 *
 * <p>
 * Reintentar el reparto es normal —el proceso que lo arma puede morir a mitad—,
 * asi que este choque es informacion, no un fallo: la fila que ya esta es la
 * buena y no se pisa, porque su {@code failed_document_count} es el numero que
 * sostiene la reclamacion de esa clinica.
 */
public class AffectedCompanyAlreadyRegisteredException extends RuntimeException {

    public AffectedCompanyAlreadyRegisteredException(Long outageId, Long companyId) {
        super("Company " + companyId + " is already registered as affected by outage " + outageId
                + ": its failed document count backs that clinic's claim and is not overwritten");
    }
}
