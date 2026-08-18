package com.vetsoftware.app.openaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("EmployeeRef — companion VO de empleado")
class EmployeeRefTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("conserva cada campo")
        void conserva_cada_campo() {
            EmployeeRef ref = new EmployeeRef(4L, "Dra. Vet");

            assertThat(ref.id()).isEqualTo(4L);
            assertThat(ref.name()).isEqualTo("Dra. Vet");
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        @Test
        @DisplayName("rechaza id null")
        void rechaza_id_null() {
            assertThatThrownBy(() -> new EmployeeRef(null, "Dra. Vet"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("rechaza un name en blanco")
        void rechaza_name_en_blanco(String name) {
            assertThatThrownBy(() -> new EmployeeRef(4L, name))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee name is required");
        }

        @Test
        @DisplayName("los datos validos no lanzan")
        void los_datos_validos_no_lanzan() {
            assertThatCode(() -> new EmployeeRef(4L, "Dra. Vet")).doesNotThrowAnyException();
        }
    }
}
