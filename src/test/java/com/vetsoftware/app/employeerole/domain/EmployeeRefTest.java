package com.vetsoftware.app.employeerole.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EmployeeRef")
class EmployeeRefTest {

    @Nested
    @DisplayName("invariantes del constructor compacto")
    class Validaciones {

        @Test
        @DisplayName("exige un id")
        void exige_un_id() {
            assertThatThrownBy(() -> new EmployeeRef(null, "EMP-007", "Ana Ruiz"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee id is required");
        }

        @Test
        @DisplayName("rechaza un codigo nulo")
        void rechaza_codigo_nulo() {
            assertThatThrownBy(() -> new EmployeeRef(7L, null, "Ana Ruiz"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee code is required");
        }

        @Test
        @DisplayName("rechaza un codigo en blanco")
        void rechaza_codigo_en_blanco() {
            assertThatThrownBy(() -> new EmployeeRef(7L, "   ", "Ana Ruiz"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee code is required");
        }

        @Test
        @DisplayName("rechaza un nombre nulo")
        void rechaza_nombre_nulo() {
            assertThatThrownBy(() -> new EmployeeRef(7L, "EMP-007", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee name is required");
        }

        @Test
        @DisplayName("rechaza un nombre en blanco")
        void rechaza_nombre_en_blanco() {
            assertThatThrownBy(() -> new EmployeeRef(7L, "EMP-007", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee name is required");
        }
    }

    @Test
    @DisplayName("con datos validos, expone cada campo tal cual se paso")
    void con_datos_validos_expone_cada_campo() {
        EmployeeRef ref = new EmployeeRef(7L, "EMP-007", "Ana Ruiz");

        assertThat(ref.id()).isEqualTo(7L);
        assertThat(ref.employeeCode()).isEqualTo("EMP-007");
        assertThat(ref.name()).isEqualTo("Ana Ruiz");
    }
}
