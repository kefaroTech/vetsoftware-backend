package com.vetsoftware.app.companyusageevent.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("UsageBranch")
class UsageBranchTest {

    @ParameterizedTest(name = "{0} se resuelve por su propio codigo")
    @EnumSource(value = UsageBranch.class, names = {"OWNER", "ANIMAL", "APPOINTMENT", "INVOICE"})
    @DisplayName("los cuatro ejes 1:1 se resuelven por su codigo")
    void los_cuatro_ejes_1_1_se_resuelven_por_su_codigo(UsageBranch rama) {
        assertThat(UsageBranch.ofDimensionCode(rama.code())).isEqualTo(rama);
        assertThat(rama.code()).isEqualTo(rama.name());
    }

    @ParameterizedTest(name = "{0} nombra el codigo GROOMING_SERVICE, no su propio nombre")
    @EnumSource(value = UsageBranch.class, names = {"GROOMING_SERVICE_SPA",
            "GROOMING_SERVICE_DAYCARE"})
    @DisplayName("las dos ramas de guarderia/spa comparten el mismo codigo de eje")
    void las_dos_ramas_de_grooming_service_comparten_el_mismo_codigo(UsageBranch rama) {
        assertThat(rama.code()).isEqualTo("GROOMING_SERVICE");
        assertThat(rama.code()).isNotEqualTo(rama.name());
    }

    @Test
    @DisplayName("GROOMING_SERVICE es ambiguo: resolverlo por codigo falla en voz alta")
    void grooming_service_es_ambiguo_por_codigo() {
        assertThatThrownBy(() -> UsageBranch.ofDimensionCode("GROOMING_SERVICE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("names two branches");
    }

    @Test
    @DisplayName("un eje de existencias no acumula hechos de uso")
    void un_eje_de_existencias_no_acumula_hechos() {
        assertThatThrownBy(() -> UsageBranch.ofDimensionCode("USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not accumulate usage events");
    }

    @Test
    @DisplayName("un codigo que no esta en el catalogo tampoco acumula hechos")
    void un_codigo_desconocido_tampoco_acumula_hechos() {
        assertThatThrownBy(() -> UsageBranch.ofDimensionCode("STORAGE_GB"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not accumulate usage events");
    }

    @Test
    @DisplayName("un codigo nulo se rechaza antes de mirar el catalogo")
    void un_codigo_nulo_se_rechaza_antes_de_mirar_el_catalogo() {
        assertThatThrownBy(() -> UsageBranch.ofDimensionCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limitDimensionCode is required");
    }

    @Test
    @DisplayName("un codigo en blanco se rechaza antes de mirar el catalogo")
    void un_codigo_en_blanco_se_rechaza_antes_de_mirar_el_catalogo() {
        assertThatThrownBy(() -> UsageBranch.ofDimensionCode("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limitDimensionCode is required");
    }
}
