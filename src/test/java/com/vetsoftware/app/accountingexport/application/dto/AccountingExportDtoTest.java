package com.vetsoftware.app.accountingexport.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.accountingexport.domain.AccountingExport;
import com.vetsoftware.app.accountingexport.domain.AccountingExportStatus;
import com.vetsoftware.app.accountingexport.testsupport.AccountingExportMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * <strong>Sin {@code version}</strong>: el DTO es una barandilla del que
 * escribe.
 */
@DisplayName("AccountingExportDto.from")
class AccountingExportDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado, sin version")
    void copia_cada_campo_del_agregado_sin_version() {
        LocalDateTime deliveredAt = AccountingExportMother.GENERATED_AT.plusDays(1);
        AccountingExport export = AccountingExportMother.entregado(deliveredAt);

        AccountingExportDto dto = AccountingExportDto.from(export);

        assertThat(dto.id()).isEqualTo(AccountingExportMother.EXPORT_ID);
        assertThat(dto.periodKey()).isEqualTo(AccountingExportMother.PERIOD_KEY);
        assertThat(dto.exportKind()).isEqualTo(AccountingExportMother.KIND);
        assertThat(dto.attemptNumber()).isEqualTo(AccountingExportMother.ATTEMPT_NUMBER);
        assertThat(dto.status()).isEqualTo(AccountingExportStatus.DELIVERED);
        assertThat(dto.generatedAt()).isEqualTo(AccountingExportMother.GENERATED_AT);
        assertThat(dto.generatedBySystemUserId()).isEqualTo(AccountingExportMother.GENERATED_BY);
        assertThat(dto.totalDebit()).isEqualByComparingTo(AccountingExportMother.TOTAL);
        assertThat(dto.totalCredit()).isEqualByComparingTo(AccountingExportMother.TOTAL);
        assertThat(dto.totalsHash()).isEqualTo(AccountingExportMother.TOTALS_HASH);
        assertThat(dto.fileRef()).isEqualTo(AccountingExportMother.FILE_REF);
        assertThat(dto.deliveredAt()).isEqualTo(deliveredAt);
        assertThat(dto.rejectedAt()).isNull();
        assertThat(dto.rejectionReason()).isNull();
        assertThat(dto.createdDate()).isEqualTo(AccountingExportMother.CREATED);
    }

    @Test
    @DisplayName("propaga fecha y motivo de un rechazo")
    void propaga_fecha_y_motivo_de_un_rechazo() {
        LocalDateTime rejectedAt = AccountingExportMother.GENERATED_AT.plusHours(3);
        AccountingExport export = AccountingExportMother.rechazado(rejectedAt,
                "Totales no cuadran");

        AccountingExportDto dto = AccountingExportDto.from(export);

        assertThat(dto.status()).isEqualTo(AccountingExportStatus.REJECTED);
        assertThat(dto.rejectedAt()).isEqualTo(rejectedAt);
        assertThat(dto.rejectionReason()).isEqualTo("Totales no cuadran");
        assertThat(dto.deliveredAt()).isNull();
    }
}
