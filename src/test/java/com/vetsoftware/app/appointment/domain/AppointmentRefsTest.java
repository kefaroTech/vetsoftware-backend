package com.vetsoftware.app.appointment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Referencias de la cita — invariantes de los value objects")
class AppointmentRefsTest {

    @Nested
    @DisplayName("AnimalRef")
    class AnimalRefTest {

        @Test
        @DisplayName("exige identificador del animal")
        void exige_identificador() {
            assertThatThrownBy(() -> new AnimalRef(null, "Firulais", "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("exige nombre del animal con contenido")
        void exige_nombre_con_contenido(String nombre) {
            assertThatThrownBy(() -> new AnimalRef(1L, nombre, "A-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("animal name is required");
        }

        @Test
        @DisplayName("acepta un animal sin codigo: el codigo es opcional")
        void acepta_un_animal_sin_codigo() {
            assertThat(new AnimalRef(1L, "Firulais", null).code()).isNull();
        }
    }

    @Nested
    @DisplayName("OwnerRef")
    class OwnerRefTest {

        @Test
        @DisplayName("exige identificador del propietario")
        void exige_identificador() {
            assertThatThrownBy(() -> new OwnerRef(null, "Ana"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("owner id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("exige nombre del propietario con contenido")
        void exige_nombre_con_contenido(String nombre) {
            assertThatThrownBy(() -> new OwnerRef(1L, nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("owner name is required");
        }
    }

    @Nested
    @DisplayName("EmployeeRef")
    class EmployeeRefTest {

        @Test
        @DisplayName("exige identificador del veterinario")
        void exige_identificador() {
            assertThatThrownBy(() -> new EmployeeRef(null, "Dra. Vet"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("exige nombre del veterinario con contenido")
        void exige_nombre_con_contenido(String nombre) {
            assertThatThrownBy(() -> new EmployeeRef(7L, nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee name is required");
        }
    }

    @Nested
    @DisplayName("BranchRef")
    class BranchRefTest {

        @Test
        @DisplayName("exige identificador de la sede")
        void exige_identificador() {
            assertThatThrownBy(() -> new BranchRef(null, "Principal", "PRINCIPAL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("branch id is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("exige nombre de la sede con contenido")
        void exige_nombre_con_contenido(String nombre) {
            assertThatThrownBy(() -> new BranchRef(1L, nombre, "PRINCIPAL"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("branch name is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("exige codigo de la sede con contenido")
        void exige_codigo_con_contenido(String codigo) {
            assertThatThrownBy(() -> new BranchRef(1L, "Principal", codigo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("branch code is required");
        }

        @Test
        @DisplayName("expone los tres datos de la sede")
        void expone_los_tres_datos() {
            BranchRef sede = new BranchRef(11L, "Sede Norte", "NORTE");

            assertThat(sede.id()).isEqualTo(11L);
            assertThat(sede.name()).isEqualTo("Sede Norte");
            assertThat(sede.code()).isEqualTo("NORTE");
        }
    }

    @Nested
    @DisplayName("CompanyRef")
    class CompanyRefTest {

        @Test
        @DisplayName("exige identificador de la empresa")
        void exige_identificador() {
            assertThatThrownBy(() -> new CompanyRef(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @Test
        @DisplayName("of construye la misma referencia que el constructor")
        void of_construye_la_misma_referencia() {
            assertThat(CompanyRef.of(9L)).isEqualTo(new CompanyRef(9L));
        }

        @Test
        @DisplayName("of tambien exige identificador")
        void of_tambien_exige_identificador() {
            assertThatThrownBy(() -> CompanyRef.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @Test
        @DisplayName("acepta cualquier identificador no nulo")
        void acepta_cualquier_identificador_no_nulo() {
            assertThatCode(() -> CompanyRef.of(1L)).doesNotThrowAnyException();
        }
    }
}
