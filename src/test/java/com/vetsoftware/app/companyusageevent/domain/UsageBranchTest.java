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
    @EnumSource(UsageBranch.class)
    @DisplayName("los cuatro ejes contables se resuelven por su codigo")
    void los_cuatro_ejes_contables_se_resuelven_por_su_codigo(UsageBranch rama) {
        assertThat(UsageBranch.ofDimensionCode(rama.code())).isEqualTo(rama);
        assertThat(rama.code()).isEqualTo(rama.name());
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
