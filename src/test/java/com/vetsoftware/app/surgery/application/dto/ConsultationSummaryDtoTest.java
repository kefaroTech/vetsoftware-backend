package com.vetsoftware.app.surgery.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.surgery.domain.ConsultationRef;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationSummaryDto.from")
class ConsultationSummaryDtoTest {

    @Test
    @DisplayName("copia cada campo del companion VO")
    void copia_cada_campo_del_companion_vo() {
        ConsultationRef ref = new ConsultationRef(200L, LocalDate.of(2026, 3, 9));

        ConsultationSummaryDto dto = ConsultationSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(200L);
        assertThat(dto.date()).isEqualTo(LocalDate.of(2026, 3, 9));
    }
}
