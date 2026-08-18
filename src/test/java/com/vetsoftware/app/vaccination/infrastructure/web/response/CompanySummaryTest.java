package com.vetsoftware.app.vaccination.infrastructure.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummary")
class CompanySummaryTest {

    @Test
    @DisplayName("expone id, nombre e identificador")
    void expone_id_nombre_e_identificador() {
        CompanySummary summary = new CompanySummary(9L, "Clinica Norte", "NIT-900");

        assertThat(summary.id()).isEqualTo(9L);
        assertThat(summary.name()).isEqualTo("Clinica Norte");
        assertThat(summary.identifier()).isEqualTo("NIT-900");
    }
}
