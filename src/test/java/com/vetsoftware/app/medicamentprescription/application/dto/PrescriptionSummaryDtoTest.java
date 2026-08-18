package com.vetsoftware.app.medicamentprescription.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PrescriptionSummaryDto.from")
class PrescriptionSummaryDtoTest {

    @Test
    @DisplayName("copia id y fecha del companion VO")
    void copia_id_y_fecha_del_companion_vo() {
        PrescriptionRef ref = new PrescriptionRef(2L, LocalDate.of(2026, 1, 10));

        PrescriptionSummaryDto dto = PrescriptionSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.date()).isEqualTo(LocalDate.of(2026, 1, 10));
    }
}
