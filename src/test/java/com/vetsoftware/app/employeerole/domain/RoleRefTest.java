package com.vetsoftware.app.employeerole.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RoleRef")
class RoleRefTest {

    @Nested
    @DisplayName("invariantes del constructor compacto")
    class Validaciones {

        @Test
        @DisplayName("exige un id")
        void exige_un_id() {
            assertThatThrownBy(() -> new RoleRef(null, "Veterinario", "VET"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("role id is required");
        }

        @Test
        @DisplayName("rechaza un nombre nulo")
        void rechaza_nombre_nulo() {
            assertThatThrownBy(() -> new RoleRef(3L, null, "VET"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("role name is required");
        }

        @Test
        @DisplayName("rechaza un nombre en blanco")
        void rechaza_nombre_en_blanco() {
            assertThatThrownBy(() -> new RoleRef(3L, "   ", "VET"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("role name is required");
        }

        @Test
        @DisplayName("rechaza un codigo nulo")
        void rechaza_codigo_nulo() {
            assertThatThrownBy(() -> new RoleRef(3L, "Veterinario", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("role code is required");
        }

        @Test
        @DisplayName("rechaza un codigo en blanco")
        void rechaza_codigo_en_blanco() {
            assertThatThrownBy(() -> new RoleRef(3L, "Veterinario", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("role code is required");
        }
    }

    @Test
    @DisplayName("con datos validos, expone cada campo tal cual se paso")
    void con_datos_validos_expone_cada_campo() {
        RoleRef ref = new RoleRef(3L, "Veterinario", "VET");

        assertThat(ref.id()).isEqualTo(3L);
        assertThat(ref.name()).isEqualTo("Veterinario");
        assertThat(ref.code()).isEqualTo("VET");
    }
}
