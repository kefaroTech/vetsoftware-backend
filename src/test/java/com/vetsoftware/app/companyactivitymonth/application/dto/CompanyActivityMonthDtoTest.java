package com.vetsoftware.app.companyactivitymonth.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companyactivitymonth.domain.CompanyActivityMonth;
import com.vetsoftware.app.companyactivitymonth.testsupport.CompanyActivityMonthMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyActivityMonthDto")
class CompanyActivityMonthDtoTest {

    @Test
    @DisplayName("from copia cada campo, con el periodo como texto")
    void from_copia_cada_campo() {
        CompanyActivityMonth mes = CompanyActivityMonthMother.pagada();

        CompanyActivityMonthDto dto = CompanyActivityMonthDto.from(mes);

        assertThat(dto.id()).isEqualTo(mes.getId());
        assertThat(dto.companyId()).isEqualTo(mes.getCompanyId());
        assertThat(dto.periodKey()).isEqualTo(mes.getPeriodKey().value());
        assertThat(dto.commercialState()).isEqualTo(mes.getCommercialState());
        assertThat(dto.activeDays()).isEqualTo(mes.getActiveDays());
        assertThat(dto.activeUsers()).isEqualTo(mes.getActiveUsers());
        assertThat(dto.recordsCreated()).isEqualTo(mes.getRecordsCreated());
        assertThat(dto.mrrSnapshot()).isEqualByComparingTo(mes.getMrrSnapshot());
        assertThat(dto.createdDate()).isEqualTo(mes.getCreatedDate());
    }
}
