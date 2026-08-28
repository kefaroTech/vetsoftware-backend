package com.vetsoftware.app.companyactivitymonth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ActivityPeriodKey")
class ActivityPeriodKeyTest {

    @ParameterizedTest(name = "{0} es un mes valido")
    @ValueSource(strings = {"2026-01", "2026-06", "2026-12"})
    @DisplayName("las formas AAAA-MM entre 01 y 12 se aceptan")
    void las_formas_validas_se_aceptan(String valor) {
        assertThat(new ActivityPeriodKey(valor).value()).isEqualTo(valor);
    }

    @ParameterizedTest(name = "{0} no es un mes valido")
    @ValueSource(strings = {"2026-13", "2026-00", "2026-1", "26-01", "2026/01", "2026-1a"})
    @DisplayName("cualquier otra forma se rechaza")
    void las_formas_invalidas_se_rechazan(String valor) {
        assertThatThrownBy(() -> new ActivityPeriodKey(valor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periodKey must be a month in AAAA-MM format");
    }

    @Test
    @DisplayName("null se rechaza")
    void null_se_rechaza() {
        assertThatThrownBy(() -> new ActivityPeriodKey(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periodKey is required");
    }

    @Test
    @DisplayName("en blanco se rechaza")
    void en_blanco_se_rechaza() {
        assertThatThrownBy(() -> new ActivityPeriodKey("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periodKey is required");
    }

    @Test
    @DisplayName("of(LocalDate) toma el mes de la fecha")
    void of_local_date_toma_el_mes_de_la_fecha() {
        assertThat(ActivityPeriodKey.of(LocalDate.of(2026, 3, 15)))
                .isEqualTo(new ActivityPeriodKey("2026-03"));
    }

    @Test
    @DisplayName("of(LocalDate) exige una fecha")
    void of_local_date_exige_una_fecha() {
        assertThatThrownBy(() -> ActivityPeriodKey.of((LocalDate) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("date is required");
    }

    @Test
    @DisplayName("of(YearMonth) formatea el mes con ceros")
    void of_year_month_formatea_el_mes_con_ceros() {
        assertThat(ActivityPeriodKey.of(YearMonth.of(2026, 3)))
                .isEqualTo(new ActivityPeriodKey("2026-03"));
    }

    @Test
    @DisplayName("of(YearMonth) exige un mes")
    void of_year_month_exige_un_mes() {
        assertThatThrownBy(() -> ActivityPeriodKey.of((YearMonth) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("month is required");
    }

    @Test
    @DisplayName("lengthOfMonth en un febrero no bisiesto da 28")
    void length_of_month_en_febrero_no_bisiesto_da_28() {
        assertThat(new ActivityPeriodKey("2026-02").lengthOfMonth()).isEqualTo(28);
    }

    @Test
    @DisplayName("lengthOfMonth en un febrero bisiesto da 29")
    void length_of_month_en_febrero_bisiesto_da_29() {
        assertThat(new ActivityPeriodKey("2024-02").lengthOfMonth()).isEqualTo(29);
    }

    @Test
    @DisplayName("lengthOfMonth en un mes de 31 dias da 31")
    void length_of_month_en_un_mes_de_31_dias_da_31() {
        assertThat(new ActivityPeriodKey("2026-01").lengthOfMonth()).isEqualTo(31);
    }

    @Test
    @DisplayName("toString es el valor tal cual")
    void to_string_es_el_valor_tal_cual() {
        assertThat(new ActivityPeriodKey("2026-03")).hasToString("2026-03");
    }
}
