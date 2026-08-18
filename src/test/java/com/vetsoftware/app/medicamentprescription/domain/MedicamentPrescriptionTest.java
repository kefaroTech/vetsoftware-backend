package com.vetsoftware.app.medicamentprescription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("MedicamentPrescription — dosis y linea de una receta")
class MedicamentPrescriptionTest {

    private static final MedicamentRef MEDICAMENTO = new MedicamentRef(1L, "Amoxicilina 500mg");
    private static final PrescriptionRef RECETA = new PrescriptionRef(2L,
            LocalDate.of(2026, 1, 10));
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 10, 9, 0);

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir nueve
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 10L;
        private MedicamentRef medicament = MEDICAMENTO;
        private String presentation = "Tableta";
        private Double quantity = 2.0;
        private String posology = "Cada 12 horas por 7 dias";
        private String observation = "Con alimento";
        private PrescriptionRef prescription = RECETA;

        private Builder medicament(MedicamentRef v) {
            this.medicament = v;
            return this;
        }

        private Builder presentation(String v) {
            this.presentation = v;
            return this;
        }

        private Builder quantity(Double v) {
            this.quantity = v;
            return this;
        }

        private Builder posology(String v) {
            this.posology = v;
            return this;
        }

        private Builder observation(String v) {
            this.observation = v;
            return this;
        }

        private Builder prescription(PrescriptionRef v) {
            this.prescription = v;
            return this;
        }

        private MedicamentPrescription build() {
            return new MedicamentPrescription(id, medicament, presentation, quantity, posology,
                    observation, prescription, CREADO, true);
        }

        private void applyTo(MedicamentPrescription line) {
            line.update(medicament, presentation, quantity, posology, observation, prescription);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            MedicamentPrescription line = valido().build();

            assertThat(line.getId()).isEqualTo(10L);
            assertThat(line.getMedicament()).isEqualTo(MEDICAMENTO);
            assertThat(line.getPresentation()).isEqualTo("Tableta");
            assertThat(line.getQuantity()).isEqualTo(2.0);
            assertThat(line.getPosology()).isEqualTo("Cada 12 horas por 7 dias");
            assertThat(line.getObservation()).isEqualTo("Con alimento");
            assertThat(line.getPrescription()).isEqualTo(RECETA);
            assertThat(line.getCreatedDate()).isEqualTo(CREADO);
            assertThat(line.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id y habilitado")
        void create_nace_sin_id_y_habilitado() {
            MedicamentPrescription line = MedicamentPrescription.create(MEDICAMENTO, "Tableta", 2.0,
                    "Cada 12 horas", "Con alimento", RECETA);

            assertThat(line.getId()).isNull();
            assertThat(line.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(line.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("getMedicamentId y getName delegan en el companion VO")
        void get_medicament_id_y_get_name_delegan_en_el_companion_vo() {
            MedicamentPrescription line = valido().build();

            assertThat(line.getMedicamentId()).isEqualTo(MEDICAMENTO.id());
            assertThat(line.getName()).isEqualTo(MEDICAMENTO.name());
        }

        @Test
        @DisplayName("observation vacia u en blanco se normaliza a null")
        void observation_en_blanco_se_normaliza_a_null() {
            assertThat(valido().observation("   ").build().getObservation()).isNull();
            assertThat(valido().observation("").build().getObservation()).isNull();
        }

        @Test
        @DisplayName("observation es opcional: una linea sin observacion es valida")
        void observation_es_opcional() {
            assertThatCode(() -> valido().observation(null).build()).doesNotThrowAnyException();
            assertThat(valido().observation(null).build().getObservation()).isNull();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("medicament null",
                            (ThrowingCallable) () -> valido().medicament(null).build(),
                            "medicament is required"),
                    arguments("presentation null",
                            (ThrowingCallable) () -> valido().presentation(null).build(),
                            "presentation is required"),
                    arguments("presentation vacia",
                            (ThrowingCallable) () -> valido().presentation("").build(),
                            "presentation is required"),
                    arguments("presentation en blanco",
                            (ThrowingCallable) () -> valido().presentation("   ").build(),
                            "presentation is required"),
                    arguments("presentation de 201 caracteres",
                            (ThrowingCallable) () -> valido().presentation("x".repeat(201)).build(),
                            "presentation must be 200 chars or less"),
                    arguments("quantity null",
                            (ThrowingCallable) () -> valido().quantity(null).build(),
                            "quantity is required"),
                    arguments("quantity cero",
                            (ThrowingCallable) () -> valido().quantity(0.0).build(),
                            "quantity must be positive"),
                    arguments("quantity negativa",
                            (ThrowingCallable) () -> valido().quantity(-1.0).build(),
                            "quantity must be positive"),
                    arguments("posology null",
                            (ThrowingCallable) () -> valido().posology(null).build(),
                            "posology is required"),
                    arguments("posology vacia",
                            (ThrowingCallable) () -> valido().posology("").build(),
                            "posology is required"),
                    arguments("posology en blanco",
                            (ThrowingCallable) () -> valido().posology("   ").build(),
                            "posology is required"),
                    arguments("posology de 1001 caracteres",
                            (ThrowingCallable) () -> valido().posology("x".repeat(1001)).build(),
                            "posology must be 1000 chars or less"),
                    arguments("observation de 1001 caracteres",
                            (ThrowingCallable) () -> valido().observation("x".repeat(1001)).build(),
                            "observation must be 1000 chars or less"),
                    arguments("prescription null",
                            (ThrowingCallable) () -> valido().prescription(null).build(),
                            "prescription is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("presentation de 200 caracteres, el limite exacto, se acepta")
        void presentation_de_200_caracteres_se_acepta() {
            assertThatCode(() -> valido().presentation("x".repeat(200)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("posology de 1000 caracteres, el limite exacto, se acepta")
        void posology_de_1000_caracteres_se_acepta() {
            assertThatCode(() -> valido().posology("x".repeat(1000)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("observation de 1000 caracteres, el limite exacto, se acepta")
        void observation_de_1000_caracteres_se_acepta() {
            assertThatCode(() -> valido().observation("x".repeat(1000)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("quantity fraccionaria y positiva se acepta")
        void quantity_fraccionaria_y_positiva_se_acepta() {
            assertThatCode(() -> valido().quantity(0.5).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            MedicamentPrescription line = valido().build();
            MedicamentRef otroMedicamento = new MedicamentRef(9L, "Ivermectina 1%");
            PrescriptionRef otraReceta = new PrescriptionRef(8L, LocalDate.of(2026, 2, 1));

            valido().medicament(otroMedicamento).presentation("Ampolla").quantity(1.0)
                    .posology("Una vez al dia").observation("Refrigerar").prescription(otraReceta)
                    .applyTo(line);

            assertThat(line.getMedicament()).isEqualTo(otroMedicamento);
            assertThat(line.getPresentation()).isEqualTo("Ampolla");
            assertThat(line.getQuantity()).isEqualTo(1.0);
            assertThat(line.getPosology()).isEqualTo("Una vez al dia");
            assertThat(line.getObservation()).isEqualTo("Refrigerar");
            assertThat(line.getPrescription()).isEqualTo(otraReceta);
            assertThat(line.getId()).isEqualTo(10L);
            assertThat(line.getCreatedDate()).isEqualTo(CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            MedicamentPrescription line = valido().build();

            // La presentacion es valida y la cantidad no: si validate() no corriera ANTES
            // de asignar, la linea se quedaria con la presentacion nueva y la cantidad
            // vieja rechazada a medias.
            assertThatThrownBy(() -> valido().presentation("Ampolla").quantity(-1.0).applyTo(line))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(line.getPresentation()).isEqualTo("Tableta");
            assertThat(line.getQuantity()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            MedicamentPrescription line = valido().build();
            line.disable();

            valido().applyTo(line);

            assertThat(line.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("update tambien normaliza observation en blanco a null")
        void update_tambien_normaliza_observation_en_blanco() {
            MedicamentPrescription line = valido().build();

            valido().observation("   ").applyTo(line);

            assertThat(line.getObservation()).isNull();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            MedicamentPrescription line = valido().build();

            line.disable();
            assertThat(line.isEnabled()).isFalse();
            line.disable();
            assertThat(line.isEnabled()).isFalse();

            line.enable();
            assertThat(line.isEnabled()).isTrue();
            line.enable();
            assertThat(line.isEnabled()).isTrue();
        }
    }
}
