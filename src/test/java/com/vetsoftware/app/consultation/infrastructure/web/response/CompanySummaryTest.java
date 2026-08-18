package com.vetsoftware.app.consultation.infrastructure.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummary")
class CompanySummaryTest {

    @Test
    @DisplayName("constructor publico conserva cada campo")
    void constructor_publico_conserva_cada_campo() {
        CompanySummary summary = new CompanySummary(9L, "Clinica Norte", "NIT-900");

        assertThat(summary.id()).isEqualTo(9L);
        assertThat(summary.name()).isEqualTo("Clinica Norte");
        assertThat(summary.identifier()).isEqualTo("NIT-900");
    }
}
