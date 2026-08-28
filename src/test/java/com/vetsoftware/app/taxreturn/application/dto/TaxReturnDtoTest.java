package com.vetsoftware.app.taxreturn.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import com.vetsoftware.app.taxreturn.testsupport.TaxReturnMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TaxReturnDto")
class TaxReturnDtoTest {

    @Test
    @DisplayName("traduce una declaracion presentada campo a campo, sin exponer la version")
    void traduce_una_declaracion_presentada_campo_a_campo() {
        TaxReturn origen = TaxReturnMother.retencionPresentada(120L);

        TaxReturnDto dto = TaxReturnDto.from(origen);

        assertThat(dto.id()).isEqualTo(origen.getId());
        assertThat(dto.taxKind()).isEqualTo(origen.getTaxKind());
        assertThat(dto.fiscalYear()).isEqualTo(origen.getFiscalYear());
        assertThat(dto.fiscalPeriodKey()).isEqualTo(origen.getFiscalPeriodKey());
        assertThat(dto.sequenceNumber()).isEqualTo(origen.getSequenceNumber());
        assertThat(dto.municipalityCode()).isEqualTo(origen.getMunicipalityCode());
        assertThat(dto.vatFrequency()).isEqualTo(origen.getVatFrequency());
        assertThat(dto.status()).isEqualTo(origen.getStatus());
        assertThat(dto.filedAt()).isEqualTo(origen.getFiledAt());
        assertThat(dto.filedBySystemUserId()).isEqualTo(origen.getFiledBySystemUserId());
        assertThat(dto.receiptRef()).isEqualTo(origen.getReceiptRef());
        assertThat(dto.fileRef()).isEqualTo(origen.getFileRef());
        assertThat(dto.totalGenerated()).isEqualByComparingTo(origen.getTotalGenerated());
        assertThat(dto.totalDeductible()).isEqualByComparingTo(origen.getTotalDeductible());
        assertThat(dto.balancePayable()).isEqualByComparingTo(origen.getBalancePayable());
        assertThat(dto.balanceCredit()).isEqualByComparingTo(origen.getBalanceCredit());
        assertThat(dto.firmezaUntil()).isEqualTo(origen.getFirmezaUntil());
        assertThat(dto.correctsReturnId()).isEqualTo(origen.getCorrectsReturnId());
        assertThat(dto.createdDate()).isEqualTo(origen.getCreatedDate());
    }

    @Test
    @DisplayName("traduce un borrador de ICA: los campos de presentacion viajan vacios")
    void traduce_un_borrador_de_ica_sin_datos_de_presentacion() {
        TaxReturn origen = TaxReturnMother.borradorDeIca();

        TaxReturnDto dto = TaxReturnDto.from(origen);

        assertThat(dto.id()).isNull();
        assertThat(dto.municipalityCode()).isEqualTo(origen.getMunicipalityCode());
        assertThat(dto.filedAt()).isNull();
        assertThat(dto.filedBySystemUserId()).isNull();
        assertThat(dto.receiptRef()).isNull();
        assertThat(dto.fileRef()).isNull();
        assertThat(dto.firmezaUntil()).isNull();
        assertThat(dto.correctsReturnId()).isNull();
    }
}
