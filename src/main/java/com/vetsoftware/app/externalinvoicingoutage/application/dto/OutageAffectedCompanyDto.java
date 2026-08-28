package com.vetsoftware.app.externalinvoicingoutage.application.dto;

import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageCompany;
import com.vetsoftware.app.externalinvoicingoutage.domain.OutageResolution;

/**
 * Una clinica del reparto de una caida.
 *
 * <p>
 * <strong>Sin {@code version} y sin {@code createdDate}</strong>, y aqui no es
 * una decision de que publicar: la tabla puente no tiene ninguna de las dos
 * columnas. Se escribe una vez y no se reescribe.
 */
public record OutageAffectedCompanyDto(Long id, Long outageId, Long companyId,
        int failedDocumentCount, OutageResolution resolvedBy, boolean contingencyNumbering) {

    public static OutageAffectedCompanyDto from(ExternalInvoicingOutageCompany affected) {
        return new OutageAffectedCompanyDto(affected.getId(), affected.getOutageId(),
                affected.getCompanyId(), affected.getFailedDocumentCount(),
                affected.getResolvedBy(), affected.usedContingencyNumbering());
    }
}
