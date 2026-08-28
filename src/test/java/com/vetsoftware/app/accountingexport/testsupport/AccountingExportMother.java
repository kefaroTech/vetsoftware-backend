package com.vetsoftware.app.accountingexport.testsupport;

import com.vetsoftware.app.accountingexport.application.command.GenerateAccountingExportCommand;
import com.vetsoftware.app.accountingexport.application.command.RejectAccountingExportCommand;
import com.vetsoftware.app.accountingexport.domain.AccountingExport;
import com.vetsoftware.app.accountingexport.domain.AccountingExportKind;
import com.vetsoftware.app.accountingexport.domain.AccountingExportStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fixtures de la feature {@code accountingexport}. */
public final class AccountingExportMother {

    public static final Long EXPORT_ID = 700L;
    public static final String PERIOD_KEY = "2026-03";
    public static final AccountingExportKind KIND = AccountingExportKind.JOURNAL_SUMMARY;
    public static final int ATTEMPT_NUMBER = 1;
    public static final LocalDateTime GENERATED_AT = LocalDateTime.of(2026, 4, 1, 9, 0);
    public static final Long GENERATED_BY = 3L;
    public static final BigDecimal TOTAL = new BigDecimal("1000.00");
    public static final String TOTALS_HASH = "0123456789abcdef".repeat(4);
    public static final String FILE_REF = "s3://vetsoftware-exports/2026-03-journal-1.csv";
    public static final LocalDateTime CREATED = LocalDateTime.of(2026, 4, 1, 9, 0);
    public static final Long VERSION = 0L;

    private AccountingExportMother() {
    }

    /** Recien generada: GENERATED, sin desenlace. El caso por defecto. */
    public static AccountingExport generado() {
        return generado(EXPORT_ID);
    }

    public static AccountingExport generado(Long id) {
        return new AccountingExport(id, PERIOD_KEY, KIND, ATTEMPT_NUMBER,
                AccountingExportStatus.GENERATED, GENERATED_AT, GENERATED_BY, TOTAL, TOTAL,
                TOTALS_HASH, FILE_REF, null, null, null, CREATED, VERSION);
    }

    public static AccountingExport entregado(LocalDateTime deliveredAt) {
        return new AccountingExport(EXPORT_ID, PERIOD_KEY, KIND, ATTEMPT_NUMBER,
                AccountingExportStatus.DELIVERED, GENERATED_AT, GENERATED_BY, TOTAL, TOTAL,
                TOTALS_HASH, FILE_REF, deliveredAt, null, null, CREATED, VERSION);
    }

    public static AccountingExport rechazado(LocalDateTime rejectedAt, String reason) {
        return new AccountingExport(EXPORT_ID, PERIOD_KEY, KIND, ATTEMPT_NUMBER,
                AccountingExportStatus.REJECTED, GENERATED_AT, GENERATED_BY, TOTAL, TOTAL,
                TOTALS_HASH, FILE_REF, null, rejectedAt, reason, CREATED, VERSION);
    }

    public static AccountingExport reemplazado() {
        return new AccountingExport(EXPORT_ID, PERIOD_KEY, KIND, ATTEMPT_NUMBER,
                AccountingExportStatus.SUPERSEDED, GENERATED_AT, GENERATED_BY, TOTAL, TOTAL,
                TOTALS_HASH, FILE_REF, null, null, null, CREATED, VERSION);
    }

    /**
     * Una exportacion valida en el estado pedido, con fechas coherentes con
     * generatedAt.
     */
    public static AccountingExport paraEstado(AccountingExportStatus status) {
        return switch (status) {
            case GENERATED -> generado();
            case DELIVERED -> entregado(GENERATED_AT.plusDays(1));
            case REJECTED -> rechazado(GENERATED_AT.plusDays(1), "Totales no cuadran");
            case SUPERSEDED -> reemplazado();
        };
    }

    public static GenerateAccountingExportCommand comandoGenerar() {
        return new GenerateAccountingExportCommand(PERIOD_KEY, KIND, GENERATED_BY, TOTAL, TOTAL,
                TOTALS_HASH, FILE_REF);
    }

    public static RejectAccountingExportCommand comandoRechazar(Long id, String reason) {
        return new RejectAccountingExportCommand(id, reason);
    }
}
