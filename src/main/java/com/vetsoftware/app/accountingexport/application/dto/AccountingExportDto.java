package com.vetsoftware.app.accountingexport.application.dto;

import com.vetsoftware.app.accountingexport.domain.AccountingExport;
import com.vetsoftware.app.accountingexport.domain.AccountingExportKind;
import com.vetsoftware.app.accountingexport.domain.AccountingExportStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** <strong>Sin {@code version}</strong>: es una barandilla del que escribe. */
public record AccountingExportDto(Long id, String periodKey, AccountingExportKind exportKind,
        int attemptNumber, AccountingExportStatus status, LocalDateTime generatedAt,
        Long generatedBySystemUserId, BigDecimal totalDebit, BigDecimal totalCredit,
        String totalsHash, String fileRef, LocalDateTime deliveredAt, LocalDateTime rejectedAt,
        String rejectionReason, LocalDateTime createdDate) {

    public static AccountingExportDto from(AccountingExport export) {
        return new AccountingExportDto(export.getId(), export.getPeriodKey(),
                export.getExportKind(), export.getAttemptNumber(), export.getStatus(),
                export.getGeneratedAt(), export.getGeneratedBySystemUserId(),
                export.getTotalDebit(), export.getTotalCredit(), export.getTotalsHash(),
                export.getFileRef(), export.getDeliveredAt(), export.getRejectedAt(),
                export.getRejectionReason(), export.getCreatedDate());
    }
}
