package com.vetsoftware.app.withholdingcertificate.application.dto;

import com.vetsoftware.app.withholdingcertificate.domain.SubstituteEvidenceKind;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WithholdingCertificateDto(Long id, Long companyId, String issuedByTaxId,
        String certificateNumber, WithholdingType withholdingType, Integer fiscalYear,
        String fiscalPeriodKey, BigDecimal ratePercent, BigDecimal certifiedAmount,
        LocalDate issuedOn, LocalDate legalDeadlineOn, LocalDate receivedOn, String fileRef,
        SubstituteEvidenceKind substituteEvidenceKind, String substituteEvidenceRef,
        boolean supported, LocalDateTime createdDate) {

    /**
     * <strong>{@code supported} se proyecta y no se recalcula fuera.</strong> «Lo
     * que hoy se puede imputar» es el papel o el sustituto que lo suple, y esa
     * disyuncion vive en el dominio: si cada consumidor la reescribiera, el dia que
     * la ley admita un segundo sustituto habria que buscarla en todos.
     */
    public static WithholdingCertificateDto from(WithholdingCertificate certificate) {
        return new WithholdingCertificateDto(certificate.getId(), certificate.getCompanyId(),
                certificate.getIssuedByTaxId(), certificate.getCertificateNumber(),
                certificate.getWithholdingType(), certificate.getFiscalYear(),
                certificate.getFiscalPeriodKey(), certificate.getRatePercent(),
                certificate.getCertifiedAmount(), certificate.getIssuedOn(),
                certificate.getLegalDeadlineOn(), certificate.getReceivedOn(),
                certificate.getFileRef(), certificate.getSubstituteEvidenceKind(),
                certificate.getSubstituteEvidenceRef(), certificate.isSupported(),
                certificate.getCreatedDate());
    }
}
