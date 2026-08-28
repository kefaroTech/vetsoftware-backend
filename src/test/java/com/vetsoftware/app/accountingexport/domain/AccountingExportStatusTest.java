package com.vetsoftware.app.accountingexport.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("AccountingExportStatus.occupiesTheCurrentSlot")
class AccountingExportStatusTest {

    @ParameterizedTest
    @EnumSource(value = AccountingExportStatus.class, names = {"GENERATED", "DELIVERED"})
    @DisplayName("GENERATED y DELIVERED ocupan el hueco de uq_accounting_exports_current")
    void generated_y_delivered_ocupan_el_hueco(AccountingExportStatus status) {
        assertThat(status.occupiesTheCurrentSlot()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = AccountingExportStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {
            "GENERATED", "DELIVERED"})
    @DisplayName("cualquier otro estado (REJECTED, SUPERSEDED) libera el hueco")
    void cualquier_otro_estado_libera_el_hueco(AccountingExportStatus status) {
        assertThat(status.occupiesTheCurrentSlot()).isFalse();
    }
}
