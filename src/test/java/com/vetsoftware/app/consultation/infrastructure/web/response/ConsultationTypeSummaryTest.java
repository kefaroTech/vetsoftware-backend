package com.vetsoftware.app.consultation.infrastructure.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationTypeSummary")
class ConsultationTypeSummaryTest {

    @Test
    @DisplayName("constructor publico conserva cada campo")
    void constructor_publico_conserva_cada_campo() {
        ConsultationTypeSummary summary = new ConsultationTypeSummary(5L, "Control");

        assertThat(summary.id()).isEqualTo(5L);
        assertThat(summary.name()).isEqualTo("Control");
    }
}
