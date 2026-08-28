package com.vetsoftware.app.customercredit.application.dto;

import com.vetsoftware.app.customercredit.domain.CreditEntryKind;
import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import com.vetsoftware.app.customercredit.domain.CustomerCreditEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerCreditEntryDto(Long id, Long companyId, CreditEntryKind entryKind,
        BigDecimal amount, Long lotEntryId, CreditOriginKind originKind, Long originPaymentId,
        Long originDocumentId, Long originSubscriptionId, LocalDateTime occurredAt,
        LocalDate valueDate, LocalDate expiresOn, LocalDateTime createdDate) {

    public static CustomerCreditEntryDto from(CustomerCreditEntry entry) {
        return new CustomerCreditEntryDto(entry.getId(), entry.getCompanyId(), entry.getEntryKind(),
                entry.getAmount(), entry.getLotEntryId(), entry.getOriginKind(),
                entry.getOriginPaymentId(), entry.getOriginDocumentId(),
                entry.getOriginSubscriptionId(), entry.getOccurredAt(), entry.getValueDate(),
                entry.getExpiresOn(), entry.getCreatedDate());
    }
}
