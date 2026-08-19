package com.vetsoftware.app.consultationtype.domain;

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

@DisplayName("ConsultationType")
class ConsultationTypeTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final String DESCRIPCION_VALIDA = "Consulta veterinaria general de rutina";

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("create genera sin id, habilitado y con fecha de creacion")
        void create_genera_sin_id_habilitado_y_con_fecha() {
            ConsultationType tipo = ConsultationType.create("Consulta general", DESCRIPCION_VALIDA);

            assertThat(tipo.getId()).isNull();
            assertThat(tipo.getName()).isEqualTo("Consulta general");
            assertThat(tipo.getDescription()).isEqualTo(DESCRIPCION_VALIDA);
            assertThat(tipo.isEnabled()).isTrue();
            assertThat(tipo.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("el constructor publico conserva cada campo tal cual se le pasa")
        void constructor_publico_conserva_cada_campo() {
            ConsultationType tipo = new ConsultationType(7L, "Consulta general", DESCRIPCION_VALIDA,
                    CREADO, null, false);

            assertThat(tipo.getId()).isEqualTo(7L);
            assertThat(tipo.getName()).isEqualTo("Consulta general");
            assertThat(tipo.getDescription()).isEqualTo(DESCRIPCION_VALIDA);
            assertThat(tipo.getCreatedDate()).isEqualTo(CREADO);
            assertThat(tipo.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validaciones de name")
    class ValidacionesName {

        @ParameterizedTest(name = "[{index}] {1}")
        @DisplayName("un nombre invalido lanza IllegalArgumentException")
        @MethodSource("nombresInvalidos")
        void nombre_invalido_lanza_excepcion(String nombreInvalido, String descripcionCaso,
                String mensajeEsperado) {
            assertThatThrownBy(() -> new ConsultationType(null, nombreInvalido, DESCRIPCION_VALIDA,
                    CREADO, null, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensajeEsperado);
        }

        static Stream<Arguments> nombresInvalidos() {
            return Stream.of(Arguments.of(null, "nulo", "name is required"),
                    Arguments.of("   ", "en blanco", "name is required"),
                    Arguments.of("a".repeat(101), "de 101 caracteres", "100 chars or less"));
        }

        @Test
        @DisplayName("un nombre de exactamente 100 caracteres es valido")
        void nombre_de_exactamente_cien_caracteres_es_valido() {
            String nombreLimite = "a".repeat(100);

            ConsultationType tipo = new ConsultationType(null, nombreLimite, DESCRIPCION_VALIDA,
                    CREADO, null, true);

            assertThat(tipo.getName()).hasSize(100);
        }
    }

    @Nested
    @DisplayName("Validaciones de description")
    class ValidacionesDescription {

        @ParameterizedTest(name = "[{index}] {1}")
        @DisplayName("una descripcion invalida lanza IllegalArgumentException")
        @MethodSource("descripcionesInvalidas")
        void descripcion_invalida_lanza_excepcion(String descripcionInvalida,
                String descripcionCaso, String mensajeEsperado) {
            assertThatThrownBy(() -> new ConsultationType(null, "Consulta general",
                    descripcionInvalida, CREADO, null, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensajeEsperado);
        }

        static Stream<Arguments> descripcionesInvalidas() {
            return Stream.of(Arguments.of(null, "nula", "description is required"),
                    Arguments.of("   ", "en blanco", "description is required"),
                    Arguments.of("a".repeat(501), "de 501 caracteres", "500 chars or less"));
        }

        @Test
        @DisplayName("una descripcion de exactamente 500 caracteres es valida")
        void descripcion_de_exactamente_quinientos_caracteres_es_valida() {
            String descripcionLimite = "a".repeat(500);

            ConsultationType tipo = new ConsultationType(null, "Consulta general",
                    descripcionLimite, CREADO, null, true);

            assertThat(tipo.getDescription()).hasSize(500);
        }
    }

    @Nested
    @DisplayName("Mutacion")
    class Mutacion {

        @Test
        @DisplayName("update reemplaza nombre y descripcion tras validar")
        void update_reemplaza_nombre_y_descripcion_tras_validar() {
            ConsultationType tipo = new ConsultationType(7L, "Original", DESCRIPCION_VALIDA, CREADO,
                    null, true);

            tipo.update("Actualizado", "Nueva descripcion de la consulta");

            assertThat(tipo.getName()).isEqualTo("Actualizado");
            assertThat(tipo.getDescription()).isEqualTo("Nueva descripcion de la consulta");
        }

        @Test
        @DisplayName("update con nombre invalido lanza y no deja el estado a medias")
        void update_con_nombre_invalido_lanza_y_no_deja_estado_a_medias() {
            ConsultationType tipo = new ConsultationType(7L, "Original", DESCRIPCION_VALIDA, CREADO,
                    null, true);

            assertThatThrownBy(() -> tipo.update(null, "Nueva descripcion de la consulta"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(tipo.getName()).isEqualTo("Original");
            assertThat(tipo.getDescription()).isEqualTo(DESCRIPCION_VALIDA);
        }

        @Test
        @DisplayName("update con descripcion invalida lanza y no deja el estado a medias")
        void update_con_descripcion_invalida_lanza_y_no_deja_estado_a_medias() {
            ConsultationType tipo = new ConsultationType(7L, "Original", DESCRIPCION_VALIDA, CREADO,
                    null, true);

            assertThatThrownBy(() -> tipo.update("Nuevo nombre", ""))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(tipo.getName()).isEqualTo("Original");
            assertThat(tipo.getDescription()).isEqualTo(DESCRIPCION_VALIDA);
        }

        @Test
        @DisplayName("enable y disable cambian el estado")
        void enable_y_disable_cambian_el_estado() {
            ConsultationType tipo = new ConsultationType(7L, "Original", DESCRIPCION_VALIDA, CREADO,
                    null, false);

            tipo.enable();
            assertThat(tipo.isEnabled()).isTrue();

            tipo.disable();
            assertThat(tipo.isEnabled()).isFalse();
        }
    }
}
