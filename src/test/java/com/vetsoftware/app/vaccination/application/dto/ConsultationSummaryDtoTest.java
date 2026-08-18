package com.vetsoftware.app.vaccination.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.vaccination.domain.ConsultationRef;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationSummaryDto")
class ConsultationSummaryDtoTest {

    @Test
    @DisplayName("from() copia id y fecha del ref")
    void from_copia_cada_campo_del_ref() {
        LocalDate fecha = LocalDate.of(2026, 1, 10);
        ConsultationRef ref = new ConsultationRef(3L, fecha);

        ConsultationSummaryDto dto = ConsultationSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(3L);
        assertThat(dto.date()).isEqualTo(fecha);
    }
}
