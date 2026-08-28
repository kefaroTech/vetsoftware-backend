package com.vetsoftware.app.companyusageevent.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("UsagePeriodKey")
class UsagePeriodKeyTest {

    @ParameterizedTest(name = "{0} es una forma valida")
    @ValueSource(strings = {"2026-01", "2026-12", "2026-Q1", "2026-Q4", "2026-S1", "2026-S2",
            "ALLTIME"})
    @DisplayName("las cuatro formas aceptadas se construyen sin problema")
    void las_cuatro_formas_aceptadas_se_construyen(String valor) {
        assertThat(UsagePeriodKey.of(valor).value()).isEqualTo(valor);
    }

    @ParameterizedTest(name = "{0} no tiene una forma reconocida")
    @ValueSource(strings = {"2026-13", "2026-00", "2026-Q5", "2026-S3", "2026-1", "2026/01",
            "alltime", "26-01"})
    @DisplayName("las formas que no encajan en el CHECK se rechazan")
    void las_formas_que_no_encajan_se_rechazan(String valor) {
        assertThatThrownBy(() -> UsagePeriodKey.of(valor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has an unknown shape");
    }

    @Test
    @DisplayName("null se rechaza sin llegar al patron")
    void null_se_rechaza_sin_llegar_al_patron() {
        assertThatThrownBy(() -> UsagePeriodKey.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periodKey is required");
    }

    @Test
    @DisplayName("en blanco se rechaza sin llegar al patron")
    void en_blanco_se_rechaza_sin_llegar_al_patron() {
        assertThatThrownBy(() -> UsagePeriodKey.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periodKey is required");
    }

    @Test
    @DisplayName("la constante ALLTIME es el centinela literal")
    void la_constante_alltime_es_el_centinela_literal() {
        assertThat(UsagePeriodKey.ALLTIME).isEqualTo("ALLTIME");
    }
}
