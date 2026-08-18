package com.vetsoftware.app.laboratorytesttype.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("LaboratoryTestType")
class LaboratoryTestTypeTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final CompanyRef CLINICA = new CompanyRef(9L, "Clinica Norte", "NIT-900");

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("un tipo propio de una empresa exige company y queda habilitado")
        void un_tipo_propio_de_empresa_exige_company() {
            LaboratoryTestType tipo = new LaboratoryTestType(70L, "Hemograma", "Hemograma completo",
                    CLINICA, false, CREADO, true);

            assertThat(tipo.getId()).isEqualTo(70L);
            assertThat(tipo.getName()).isEqualTo("Hemograma");
            assertThat(tipo.getDescription()).isEqualTo("Hemograma completo");
            assertThat(tipo.getCompany()).isEqualTo(CLINICA);
            assertThat(tipo.isGeneral()).isFalse();
            assertThat(tipo.getCreatedDate()).isEqualTo(CREADO);
            assertThat(tipo.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un tipo general no lleva company")
        void un_tipo_general_no_lleva_company() {
            LaboratoryTestType tipo = new LaboratoryTestType(71L, "Perfil renal",
                    "Perfil renal basico", null, true, CREADO, true);

            assertThat(tipo.getCompany()).isNull();
            assertThat(tipo.isGeneral()).isTrue();
        }

        @Test
        @DisplayName("create() fabrica un tipo nuevo habilitado sin id")
        void create_fabrica_un_tipo_nuevo_habilitado_sin_id() {
            LaboratoryTestType tipo = LaboratoryTestType.create("Hemograma", "Hemograma completo",
                    CLINICA, false);

            assertThat(tipo.getId()).isNull();
            assertThat(tipo.isEnabled()).isTrue();
            assertThat(tipo.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("la descripcion es opcional")
        void la_descripcion_es_opcional() {
            LaboratoryTestType tipo = new LaboratoryTestType(70L, "Hemograma", null, CLINICA, false,
                    CREADO, true);

            assertThat(tipo.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("validaciones del constructor — cada invariante es un caso")
    class Validaciones {

        static Stream<Arguments> datosInvalidos() {
            return Stream.of(Arguments.of(null, "d", CLINICA, false, "name is required"),
                    Arguments.of("  ", "d", CLINICA, false, "name is required"),
                    Arguments.of("x".repeat(101), "d", CLINICA, false,
                            "name must be 100 chars or less"),
                    Arguments.of("Hemograma", "x".repeat(501), CLINICA, false,
                            "description must be 500 chars or less"),
                    Arguments.of("Perfil renal", "d", CLINICA, true,
                            "general type cannot have company"),
                    Arguments.of("Hemograma", "d", null, false,
                            "non-general type requires company"));
        }

        @ParameterizedTest(name = "{4}")
        @MethodSource("datosInvalidos")
        @DisplayName("cada invariante rechaza su combinacion invalida")
        void cada_invariante_rechaza_su_combinacion_invalida(String name, String description,
                CompanyRef company, boolean general, String mensajeEsperado) {
            assertThatThrownBy(() -> new LaboratoryTestType(70L, name, description, company,
                    general, CREADO, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensajeEsperado);
        }

        @Test
        @DisplayName("un nombre de exactamente 100 caracteres es valido")
        void nombre_de_cien_caracteres_es_valido() {
            String limite = "x".repeat(100);

            LaboratoryTestType tipo = new LaboratoryTestType(70L, limite, "d", CLINICA, false,
                    CREADO, true);

            assertThat(tipo.getName()).isEqualTo(limite);
        }

        @Test
        @DisplayName("una descripcion de exactamente 500 caracteres es valida")
        void descripcion_de_quinientos_caracteres_es_valida() {
            String limite = "x".repeat(500);

            LaboratoryTestType tipo = new LaboratoryTestType(70L, "Hemograma", limite, CLINICA,
                    false, CREADO, true);

            assertThat(tipo.getDescription()).isEqualTo(limite);
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("update() reemplaza nombre, descripcion, company y general")
        void update_reemplaza_los_campos_editables() {
            LaboratoryTestType tipo = new LaboratoryTestType(70L, "Hemograma", "Hemograma completo",
                    CLINICA, false, CREADO, true);

            tipo.update("Hemograma completo", "Hemograma con formula", CLINICA, false);

            assertThat(tipo.getName()).isEqualTo("Hemograma completo");
            assertThat(tipo.getDescription()).isEqualTo("Hemograma con formula");
        }

        @Test
        @DisplayName("update() vuelve a validar las invariantes")
        void update_vuelve_a_validar_las_invariantes() {
            LaboratoryTestType tipo = new LaboratoryTestType(70L, "Hemograma", "Hemograma completo",
                    CLINICA, false, CREADO, true);

            assertThatThrownBy(() -> tipo.update("", "d", CLINICA, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("update() no cambia id, createdDate ni enabled")
        void update_no_toca_id_createdDate_ni_enabled() {
            LaboratoryTestType tipo = new LaboratoryTestType(70L, "Hemograma", "Hemograma completo",
                    CLINICA, false, CREADO, true);

            tipo.update("Hemograma completo", "d", CLINICA, false);

            assertThat(tipo.getId()).isEqualTo(70L);
            assertThat(tipo.getCreatedDate()).isEqualTo(CREADO);
            assertThat(tipo.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("transiciones de estado")
    class EstadoActivo {

        @Test
        @DisplayName("disable() deshabilita un tipo activo")
        void disable_deshabilita_un_tipo_activo() {
            LaboratoryTestType tipo = new LaboratoryTestType(70L, "Hemograma", "Hemograma completo",
                    CLINICA, false, CREADO, true);

            tipo.disable();

            assertThat(tipo.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable() vuelve a habilitar un tipo deshabilitado")
        void enable_vuelve_a_habilitar_un_tipo_deshabilitado() {
            LaboratoryTestType tipo = new LaboratoryTestType(70L, "Hemograma", "Hemograma completo",
                    CLINICA, false, CREADO, false);

            tipo.enable();

            assertThat(tipo.isEnabled()).isTrue();
        }
    }
}
