package com.vetsoftware.app.documentwithholding.application.dto;

import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import com.vetsoftware.app.documentwithholding.domain.WithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <strong>Lleva {@code certificateId} a proposito, incluso cuando es
 * nulo.</strong> Ese nulo no es un dato que falte: es el estado «sin respaldo»,
 * y es justo lo que el cliente necesita ver para saber que retenciones tiene
 * que reclamarle a quien se las practico.
 *
 * <p>
 * <strong>Sin {@code version}</strong>: el numero de version es la barandilla
 * del bloqueo optimista, no un dato del expediente. Publicarlo invitaria a un
 * cliente a construirse un {@code If-Match} a mano sobre una operacion que aqui
 * no lo usa.
 */
public record DocumentWithholdingDto(Long id, Long companyId, Long billingDocumentId,
        WithholdingType type, BigDecimal taxableBase, BigDecimal ratePercent, BigDecimal amount,
        String municipalityCode, int fiscalYear, String fiscalPeriodKey, LocalDate practicedOn,
        Long certificateId, LocalDateTime createdDate) {

    public static DocumentWithholdingDto from(DocumentWithholding withholding) {
        return new DocumentWithholdingDto(withholding.getId(), withholding.getCompanyId(),
                withholding.getBillingDocumentId(), withholding.getType(),
                withholding.getTaxableBase(), withholding.getRatePercent(), withholding.getAmount(),
                withholding.getMunicipalityCode(), withholding.getFiscalYear(),
                withholding.getFiscalPeriodKey(), withholding.getPracticedOn(),
                withholding.getCertificateId(), withholding.getCreatedDate());
    }
}
