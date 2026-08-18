package com.vetsoftware.app.hospitalizationprocedure.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationRef;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HospitalizationSummaryDto.from")
class HospitalizationSummaryDtoTest {

    @Test
    @DisplayName("copia id y fecha del ref")
    void copia_id_y_fecha_del_ref() {
        LocalDate fecha = LocalDate.of(2026, 3, 1);
        HospitalizationRef ref = new HospitalizationRef(20L, fecha);

        HospitalizationSummaryDto dto = HospitalizationSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(20L);
        assertThat(dto.date()).isEqualTo(fecha);
    }
}
