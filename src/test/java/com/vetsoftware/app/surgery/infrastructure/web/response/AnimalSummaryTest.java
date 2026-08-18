package com.vetsoftware.app.surgery.infrastructure.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnimalSummary")
class AnimalSummaryTest {

    @Test
    @DisplayName("constructor publico conserva cada campo")
    void constructor_publico_conserva_cada_campo() {
        AnimalSummary summary = new AnimalSummary(100L, "Firulais", "A-001");

        assertThat(summary.id()).isEqualTo(100L);
        assertThat(summary.name()).isEqualTo("Firulais");
        assertThat(summary.code()).isEqualTo("A-001");
    }
}
