package com.vetsoftware.app.taxreturn.application.dto;

import com.vetsoftware.app.taxreturn.domain.TaxKind;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.domain.TaxReturnStatus;
import com.vetsoftware.app.taxreturn.domain.VatFrequency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** <strong>Sin {@code version}</strong>: es una barandilla del que escribe. */
public record TaxReturnDto(Long id, TaxKind taxKind, int fiscalYear, String fiscalPeriodKey,
        int sequenceNumber, String municipalityCode, VatFrequency vatFrequency,
        TaxReturnStatus status, LocalDateTime filedAt, Long filedBySystemUserId, String receiptRef,
        String fileRef, BigDecimal totalGenerated, BigDecimal totalDeductible,
        BigDecimal balancePayable, BigDecimal balanceCredit, LocalDate firmezaUntil,
        Long correctsReturnId, LocalDateTime createdDate) {

    public static TaxReturnDto from(TaxReturn taxReturn) {
        return new TaxReturnDto(taxReturn.getId(), taxReturn.getTaxKind(),
                taxReturn.getFiscalYear(), taxReturn.getFiscalPeriodKey(),
                taxReturn.getSequenceNumber(), taxReturn.getMunicipalityCode(),
                taxReturn.getVatFrequency(), taxReturn.getStatus(), taxReturn.getFiledAt(),
                taxReturn.getFiledBySystemUserId(), taxReturn.getReceiptRef(),
                taxReturn.getFileRef(), taxReturn.getTotalGenerated(),
                taxReturn.getTotalDeductible(), taxReturn.getBalancePayable(),
                taxReturn.getBalanceCredit(), taxReturn.getFirmezaUntil(),
                taxReturn.getCorrectsReturnId(), taxReturn.getCreatedDate());
    }
}
