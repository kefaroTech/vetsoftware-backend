package com.vetsoftware.app.hospitalizationobservation.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationRef;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HospitalizationSummaryDto — from()")
class HospitalizationSummaryDtoTest {

    @Test
    @DisplayName("copia cada campo del ref")
    void copia_cada_campo_del_ref() {
        HospitalizationRef ref = new HospitalizationRef(600L, LocalDate.of(2026, 3, 1));

        HospitalizationSummaryDto dto = HospitalizationSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(600L);
        assertThat(dto.date()).isEqualTo(LocalDate.of(2026, 3, 1));
    }
}
