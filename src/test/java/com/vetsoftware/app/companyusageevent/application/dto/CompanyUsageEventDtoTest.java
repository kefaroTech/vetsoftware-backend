package com.vetsoftware.app.companyusageevent.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companyusageevent.domain.CompanyUsageEvent;
import com.vetsoftware.app.companyusageevent.testsupport.CompanyUsageEventMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyUsageEventDto")
class CompanyUsageEventDtoTest {

    @Test
    @DisplayName("from copia cada campo de un hecho sin cargo")
    void from_copia_cada_campo_de_un_hecho_sin_cargo() {
        CompanyUsageEvent evento = CompanyUsageEventMother.hechoSinCargo();

        CompanyUsageEventDto dto = CompanyUsageEventDto.from(evento);

        assertThat(dto.id()).isEqualTo(evento.getId());
        assertThat(dto.companyId()).isEqualTo(evento.getCompanyId());
        assertThat(dto.limitDimensionId()).isEqualTo(evento.getLimitDimensionId());
        assertThat(dto.branch()).isEqualTo(evento.getBranch());
        assertThat(dto.usageReferenceId()).isEqualTo(evento.getUsageReferenceId());
        assertThat(dto.occurredAt()).isEqualTo(evento.getOccurredAt());
        assertThat(dto.periodKey()).isEqualTo(evento.getPeriodKey().value());
        assertThat(dto.billable()).isEqualTo(evento.isBillable());
        assertThat(dto.chargeId()).isNull();
        assertThat(dto.createdDate()).isEqualTo(evento.getCreatedDate());
    }

    @Test
    @DisplayName("un hecho ya colgado de un cargo publica el chargeId")
    void un_hecho_ya_colgado_publica_el_charge_id() {
        CompanyUsageEvent evento = CompanyUsageEventMother.hechoConCargo();

        CompanyUsageEventDto dto = CompanyUsageEventDto.from(evento);

        assertThat(dto.chargeId()).isEqualTo(CompanyUsageEventMother.CHARGE_ID);
    }
}
