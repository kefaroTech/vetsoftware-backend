package com.vetsoftware.app.hospitalizationprogressnote.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Nota de evolucion clinica dentro de una hospitalizacion. Las invariantes
 * viven en el constructor y {@code update} las reafirma: es el unico punto por
 * el que puede colarse una descripcion vacia o sin sus dos referencias.
 */
@DisplayName("HospitalizationProgressNote")
class HospitalizationProgressNoteTest {

    private static final HospitalizationRef HOSPITALIZACION = new HospitalizationRef(55L,
            LocalDate.of(2026, 3, 1));
    private static final EmployeeRef VETERINARIO = new EmployeeRef(4L, "EMP-001", "Ana Ruiz");
    private static final String DESCRIPCION = "Paciente estable, buena respuesta al tratamiento";

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("create() arma la nota habilitada y sin id, con fecha propia")
        void create_arma_la_nota_habilitada_y_sin_id() {
            HospitalizationProgressNote nota = HospitalizationProgressNote.create(DESCRIPCION,
                    HOSPITALIZACION, VETERINARIO);

            assertThat(nota.getId()).isNull();
            assertThat(nota.getDescription()).isEqualTo(DESCRIPCION);
            assertThat(nota.getHospitalization()).isEqualTo(HOSPITALIZACION);
            assertThat(nota.getCreatedBy()).isEqualTo(VETERINARIO);
            assertThat(nota.getCreatedDate()).isNotNull();
            assertThat(nota.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("el constructor completo conserva cada campo tal cual")
        void el_constructor_completo_conserva_cada_campo() {
            LocalDateTime creada = LocalDateTime.of(2026, 3, 1, 9, 15);

            HospitalizationProgressNote nota = new HospitalizationProgressNote(500L, DESCRIPCION,
                    HOSPITALIZACION, VETERINARIO, creada, null, true);

            assertThat(nota.getId()).isEqualTo(500L);
            assertThat(nota.getCreatedDate()).isEqualTo(creada);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("rechaza descripcion vacia")
        void rechaza_descripcion_vacia(String descripcion) {
            assertThatThrownBy(() -> HospitalizationProgressNote.create(descripcion,
                    HOSPITALIZACION, VETERINARIO)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");
        }

        @Test
        @DisplayName("rechaza descripcion de mas de 2000 caracteres")
        void rechaza_descripcion_demasiado_larga() {
            String larga = "x".repeat(2001);

            assertThatThrownBy(
                    () -> HospitalizationProgressNote.create(larga, HOSPITALIZACION, VETERINARIO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description must be 2000 chars or less");
        }

        @Test
        @DisplayName("acepta descripcion de exactamente 2000 caracteres")
        void acepta_descripcion_de_2000_caracteres() {
            String limite = "x".repeat(2000);

            HospitalizationProgressNote nota = HospitalizationProgressNote.create(limite,
                    HOSPITALIZACION, VETERINARIO);

            assertThat(nota.getDescription()).hasSize(2000);
        }

        @Test
        @DisplayName("rechaza hospitalizacion nula")
        void rechaza_hospitalizacion_nula() {
            assertThatThrownBy(
                    () -> HospitalizationProgressNote.create(DESCRIPCION, null, VETERINARIO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("hospitalization is required");
        }

        @Test
        @DisplayName("rechaza empleado creador nulo")
        void rechaza_empleado_creador_nulo() {
            assertThatThrownBy(
                    () -> HospitalizationProgressNote.create(DESCRIPCION, HOSPITALIZACION, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("createdBy is required");
        }
    }

    @Nested
    @DisplayName("Estado habilitado")
    class EstadoHabilitado {

        @Test
        @DisplayName("disable() la deshabilita y enable() la revive")
        void disable_y_enable_alternan_el_estado() {
            HospitalizationProgressNote nota = HospitalizationProgressNote.create(DESCRIPCION,
                    HOSPITALIZACION, VETERINARIO);

            nota.disable();
            assertThat(nota.isEnabled()).isFalse();

            nota.enable();
            assertThat(nota.isEnabled()).isTrue();
        }
    }
}
