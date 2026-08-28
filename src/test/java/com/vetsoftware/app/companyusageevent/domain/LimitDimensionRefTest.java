package com.vetsoftware.app.companyusageevent.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LimitDimensionRef")
class LimitDimensionRefTest {

    @Test
    @DisplayName("se construye con id y codigo")
    void se_construye_con_id_y_codigo() {
        LimitDimensionRef ref = new LimitDimensionRef(5L, "ANIMAL");

        assertThat(ref.id()).isEqualTo(5L);
        assertThat(ref.code()).isEqualTo("ANIMAL");
    }

    @Test
    @DisplayName("el id es obligatorio")
    void el_id_es_obligatorio() {
        assertThatThrownBy(() -> new LimitDimensionRef(null, "ANIMAL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit dimension id is required");
    }

    @Test
    @DisplayName("el codigo nulo se rechaza")
    void el_codigo_nulo_se_rechaza() {
        assertThatThrownBy(() -> new LimitDimensionRef(5L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit dimension code is required");
    }

    @Test
    @DisplayName("el codigo en blanco se rechaza")
    void el_codigo_en_blanco_se_rechaza() {
        assertThatThrownBy(() -> new LimitDimensionRef(5L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit dimension code is required");
    }

    @Test
    @DisplayName("branch delega en UsageBranch para un eje contable")
    void branch_delega_en_usage_branch_para_un_eje_contable() {
        LimitDimensionRef ref = new LimitDimensionRef(5L, "ANIMAL");

        assertThat(ref.branch()).isEqualTo(UsageBranch.ANIMAL);
    }

    @Test
    @DisplayName("branch falla en voz alta para un eje de existencias")
    void branch_falla_en_voz_alta_para_un_eje_de_existencias() {
        LimitDimensionRef ref = new LimitDimensionRef(2L, "USER");

        assertThatThrownBy(ref::branch).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not accumulate usage events");
    }
}
