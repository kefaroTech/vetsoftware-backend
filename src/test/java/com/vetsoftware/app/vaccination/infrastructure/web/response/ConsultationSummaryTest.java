package com.vetsoftware.app.vaccination.infrastructure.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationSummary")
class ConsultationSummaryTest {

    @Test
    @DisplayName("expone id y fecha")
    void expone_id_y_fecha() {
        LocalDate fecha = LocalDate.of(2026, 2, 28);

        ConsultationSummary summary = new ConsultationSummary(7L, fecha);

        assertThat(summary.id()).isEqualTo(7L);
        assertThat(summary.date()).isEqualTo(fecha);
    }
}
