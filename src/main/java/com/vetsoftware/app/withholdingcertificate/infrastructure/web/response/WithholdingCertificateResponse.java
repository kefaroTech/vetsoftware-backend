package com.vetsoftware.app.withholdingcertificate.infrastructure.web.response;

import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import com.vetsoftware.app.withholdingcertificate.domain.SubstituteEvidenceKind;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * El certificado tal como sale por HTTP. Lo ven la consola de plataforma y el
 * cliente: la plataforma escribe y los dos leen, porque la retencion se la
 * practicaron al cliente y es el quien la imputa.
 *
 * <p>
 * <strong>{@code supported} viaja calculado y no como dos campos que el front
 * tenga que combinar.</strong> «Esta retencion se puede imputar hoy» es el
 * papel o el sustituto que lo suple, y esa disyuncion es de negocio: dejarla al
 * cliente garantiza que los dos frontends la escriban distinto, y que el dia
 * que la ley admita otro soporte haya que corregirla en tres repositorios.
 */
public record WithholdingCertificateResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long companyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String issuedByTaxId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String certificateNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) WithholdingType withholdingType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer fiscalYear,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fiscalPeriodKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal ratePercent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal certifiedAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate issuedOn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate legalDeadlineOn,
        LocalDate receivedOn, String fileRef, SubstituteEvidenceKind substituteEvidenceKind,
        String substituteEvidenceRef,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean supported,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static WithholdingCertificateResponse from(WithholdingCertificateDto dto) {
        return new WithholdingCertificateResponse(dto.id(), dto.companyId(), dto.issuedByTaxId(),
                dto.certificateNumber(), dto.withholdingType(), dto.fiscalYear(),
                dto.fiscalPeriodKey(), dto.ratePercent(), dto.certifiedAmount(), dto.issuedOn(),
                dto.legalDeadlineOn(), dto.receivedOn(), dto.fileRef(),
                dto.substituteEvidenceKind(), dto.substituteEvidenceRef(), dto.supported(),
                dto.createdDate());
    }
}
