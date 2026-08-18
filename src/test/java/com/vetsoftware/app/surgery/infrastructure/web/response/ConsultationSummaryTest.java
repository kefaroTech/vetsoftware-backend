package com.vetsoftware.app.surgery.infrastructure.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationSummary")
class ConsultationSummaryTest {

    @Test
    @DisplayName("constructor publico conserva cada campo")
    void constructor_publico_conserva_cada_campo() {
        ConsultationSummary summary = new ConsultationSummary(200L, LocalDate.of(2026, 3, 9));

        assertThat(summary.id()).isEqualTo(200L);
        assertThat(summary.date()).isEqualTo(LocalDate.of(2026, 3, 9));
    }
}
