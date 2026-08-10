package com.vetsoftware.app.company.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("MembershipRef — VO de la membresia referenciada")
class MembershipRefTest {

    @Test
    @DisplayName("conserva id, nombre y estado sin cruzar los dos campos de texto")
    void conserva_id_nombre_y_estado() {
        MembershipRef ref = new MembershipRef(21L, "Premium", "ACTIVE");

        assertThat(ref.id()).isEqualTo(21L);
        assertThat(ref.name()).isEqualTo("Premium");
        assertThat(ref.status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("rechaza el id nulo: sin id no hay referencia que resolver")
    void rechaza_el_id_nulo() {
        assertThatThrownBy(() -> new MembershipRef(null, "Premium", "ACTIVE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("membership id is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("rechaza el nombre ausente o en blanco")
    void rechaza_el_nombre_en_blanco(String nombre) {
        assertThatThrownBy(() -> new MembershipRef(21L, nombre, "ACTIVE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("membership name is required");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("rechaza el estado ausente o en blanco")
    void rechaza_el_estado_en_blanco(String estado) {
        assertThatThrownBy(() -> new MembershipRef(21L, "Premium", estado))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("membership status is required");
    }

    @Test
    @DisplayName("dos referencias con los mismos valores son iguales")
    void dos_referencias_con_los_mismos_valores_son_iguales() {
        assertThat(new MembershipRef(21L, "Premium", "ACTIVE"))
                .isEqualTo(new MembershipRef(21L, "Premium", "ACTIVE"))
                .isNotEqualTo(new MembershipRef(21L, "Premium", "EXPIRED"));
    }
}
