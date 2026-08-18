package com.vetsoftware.app.surgery.infrastructure.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SurgeryTypeSummary")
class SurgeryTypeSummaryTest {

    @Test
    @DisplayName("constructor publico conserva cada campo")
    void constructor_publico_conserva_cada_campo() {
        SurgeryTypeSummary summary = new SurgeryTypeSummary(5L, "Ovariohisterectomia");

        assertThat(summary.id()).isEqualTo(5L);
        assertThat(summary.name()).isEqualTo("Ovariohisterectomia");
    }
}
