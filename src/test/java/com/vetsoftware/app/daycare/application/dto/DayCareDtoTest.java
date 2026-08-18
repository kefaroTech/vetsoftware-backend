package com.vetsoftware.app.daycare.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.daycare.domain.DayCare;
import com.vetsoftware.app.daycare.domain.DayCareType;
import com.vetsoftware.app.daycare.testsupport.DayCareMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DayCareDto.from")
class DayCareDtoTest {

    @Test
    @DisplayName("mapea cada campo del agregado, campo por campo")
    void mapea_cada_campo() {
        DayCare dayCare = DayCareMother.guarderiaValida();

        DayCareDto dto = DayCareDto.from(dayCare);

        assertThat(dto.id()).isEqualTo(dayCare.getId());
        assertThat(dto.date()).isEqualTo(dayCare.getDate());
        assertThat(dto.startDate()).isEqualTo(dayCare.getStartDate());
        assertThat(dto.endDate()).isEqualTo(dayCare.getEndDate());
        assertThat(dto.type()).isEqualTo(DayCareType.DAYCARE);
        assertThat(dto.objects()).isEqualTo(dayCare.getObjects());
        assertThat(dto.observations()).isEqualTo(dayCare.getObservations());
        assertThat(dto.animal()).isEqualTo(AnimalSummaryDto.from(dayCare.getAnimal()));
        assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(dayCare.getCompany()));
        assertThat(dto.createdDate()).isEqualTo(dayCare.getCreatedDate());
        assertThat(dto.enabled()).isEqualTo(dayCare.isEnabled());
    }

    @Test
    @DisplayName("un daycare deshabilitado mapea enabled en false")
    void un_daycare_deshabilitado_mapea_enabled_en_false() {
        DayCare dayCare = DayCareMother.guarderiaValida();
        dayCare.disable();

        assertThat(DayCareDto.from(dayCare).enabled()).isFalse();
    }
}
