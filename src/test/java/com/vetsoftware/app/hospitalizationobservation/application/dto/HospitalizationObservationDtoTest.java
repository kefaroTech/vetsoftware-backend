package com.vetsoftware.app.hospitalizationobservation.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.hospitalizationobservation.testsupport.HospitalizationObservationMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HospitalizationObservationDto — from()")
class HospitalizationObservationDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado, incluidas las referencias companion")
    void copia_cada_campo_del_agregado() {
        HospitalizationObservation observation = HospitalizationObservationMother
                .observacionValida();

        HospitalizationObservationDto dto = HospitalizationObservationDto.from(observation);

        assertThat(dto.id()).isEqualTo(observation.getId());
        assertThat(dto.description()).isEqualTo(observation.getDescription());
        assertThat(dto.createdDate()).isEqualTo(observation.getCreatedDate());
        assertThat(dto.enabled()).isEqualTo(observation.isEnabled());
        assertThat(dto.hospitalization()).isEqualTo(
                HospitalizationSummaryDto.from(HospitalizationObservationMother.HOSPITALIZACION));
        assertThat(dto.createdBy())
                .isEqualTo(EmployeeSummaryDto.from(HospitalizationObservationMother.VETERINARIO));
    }
}
