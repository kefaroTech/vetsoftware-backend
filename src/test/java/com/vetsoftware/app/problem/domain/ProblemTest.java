package com.vetsoftware.app.problem.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.problem.testsupport.ProblemMother;
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

@DisplayName("Problem — invariantes y ciclo de vida del agregado")
class ProblemTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir diez
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 1L;
        private AnimalRef animal = ProblemMother.FIRULAIS;
        private CompanyRef company = ProblemMother.CLINICA;
        private String description = "Dermatitis alergica";
        private ProblemStatus status = ProblemStatus.ACTIVE;
        private LocalDate onsetDate = ProblemMother.INICIO;
        private LocalDate resolvedDate;
        private String notes = "Revisar en dos semanas";
        private boolean enabled = true;

        private Builder animal(AnimalRef v) {
            this.animal = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Builder description(String v) {
            this.description = v;
            return this;
        }

        private Builder status(ProblemStatus v) {
            this.status = v;
            return this;
        }

        private Builder resolvedDate(LocalDate v) {
            this.resolvedDate = v;
            return this;
        }

        private Builder notes(String v) {
            this.notes = v;
            return this;
        }

        private Builder enabled(boolean v) {
            this.enabled = v;
            return this;
        }

        private Problem build() {
            return new Problem(id, animal, company, description, status, onsetDate, resolvedDate,
                    notes, ProblemMother.CREADO, enabled);
        }

        private void applyTo(Problem problem) {
            problem.update(description, status, onsetDate, resolvedDate, notes, company);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Problem problem = valido().build();

            assertThat(problem.getId()).isEqualTo(1L);
            assertThat(problem.getAnimal()).isEqualTo(ProblemMother.FIRULAIS);
            assertThat(problem.getCompany()).isEqualTo(ProblemMother.CLINICA);
            assertThat(problem.getDescription()).isEqualTo("Dermatitis alergica");
            assertThat(problem.getStatus()).isEqualTo(ProblemStatus.ACTIVE);
            assertThat(problem.getOnsetDate()).isEqualTo(ProblemMother.INICIO);
            assertThat(problem.getResolvedDate()).isNull();
            assertThat(problem.getNotes()).isEqualTo("Revisar en dos semanas");
            assertThat(problem.getCreatedDate()).isEqualTo(ProblemMother.CREADO);
            assertThat(problem.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id y habilitado")
        void create_nace_sin_id_y_habilitado() {
            Problem problem = Problem.create(ProblemMother.FIRULAIS, ProblemMother.CLINICA,
                    "Dermatitis alergica", ProblemStatus.ACTIVE, ProblemMother.INICIO, null,
                    "Revisar en dos semanas");

            assertThat(problem.getId()).isNull();
            assertThat(problem.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(problem.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("resolvedDate es opcional: un problema activo sin fecha de resolucion es valido")
        void resolved_date_es_opcional() {
            assertThatCode(() -> valido().resolvedDate(null).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("notas — se normalizan a null cuando estan en blanco")
    class Notas {

        @Test
        @DisplayName("notes null se conserva como null")
        void notes_null_se_conserva_como_null() {
            assertThat(valido().notes(null).build().getNotes()).isNull();
        }

        @Test
        @DisplayName("notes vacio se normaliza a null")
        void notes_vacio_se_normaliza_a_null() {
            assertThat(valido().notes("").build().getNotes()).isNull();
        }

        @Test
        @DisplayName("notes en blanco se normaliza a null")
        void notes_en_blanco_se_normaliza_a_null() {
            assertThat(valido().notes("   ").build().getNotes()).isNull();
        }

        @Test
        @DisplayName("notes no vacio se conserva tal cual")
        void notes_no_vacio_se_conserva_tal_cual() {
            assertThat(valido().notes("Seguimiento semanal").build().getNotes())
                    .isEqualTo("Seguimiento semanal");
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("description null",
                            (ThrowingCallable) () -> valido().description(null).build(),
                            "description is required"),
                    arguments("description vacia",
                            (ThrowingCallable) () -> valido().description("").build(),
                            "description is required"),
                    arguments("description en blanco",
                            (ThrowingCallable) () -> valido().description("   ").build(),
                            "description is required"),
                    arguments("description de 256 chars",
                            (ThrowingCallable) () -> valido().description("x".repeat(256)).build(),
                            "description must be 255 chars or less"),
                    arguments("status null", (ThrowingCallable) () -> valido().status(null).build(),
                            "status is required"),
                    arguments("notes de 2001 chars",
                            (ThrowingCallable) () -> valido().notes("x".repeat(2001)).build(),
                            "notes must be 2000 chars or less"),
                    arguments("animal null", (ThrowingCallable) () -> valido().animal(null).build(),
                            "animal is required"),
                    arguments("company null",
                            (ThrowingCallable) () -> valido().company(null).build(),
                            "company is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("description de 255 chars se acepta")
        void description_de_255_chars_se_acepta() {
            assertThatCode(() -> valido().description("x".repeat(255)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("notes de 2000 chars se acepta")
        void notes_de_2000_chars_se_acepta() {
            assertThatCode(() -> valido().notes("x".repeat(2000)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id, animal y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_animal_y_created_date() {
            Problem problem = valido().build();
            CompanyRef otraEmpresa = new CompanyRef(20L, "Clinica Sur", "NIT-800");

            valido().description("Resuelto tras tratamiento").status(ProblemStatus.RESOLVED)
                    .resolvedDate(LocalDate.of(2026, 2, 1)).notes("Sin recaidas")
                    .company(otraEmpresa).applyTo(problem);

            assertThat(problem.getDescription()).isEqualTo("Resuelto tras tratamiento");
            assertThat(problem.getStatus()).isEqualTo(ProblemStatus.RESOLVED);
            assertThat(problem.getResolvedDate()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(problem.getNotes()).isEqualTo("Sin recaidas");
            assertThat(problem.getCompany()).isEqualTo(otraEmpresa);
            assertThat(problem.getId()).isEqualTo(1L);
            assertThat(problem.getAnimal()).isEqualTo(ProblemMother.FIRULAIS);
            assertThat(problem.getCreatedDate()).isEqualTo(ProblemMother.CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            Problem problem = valido().build();

            // La descripcion es valida y el estado no: si validate() no corriera ANTES de
            // asignar, el problema se quedaria con la descripcion nueva y el estado viejo.
            assertThatThrownBy(() -> valido().description("Resuelto").status(null).applyTo(problem))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(problem.getDescription()).isEqualTo("Dermatitis alergica");
            assertThat(problem.getStatus()).isEqualTo(ProblemStatus.ACTIVE);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            Problem problem = valido().build();
            problem.disable();

            valido().description("Resuelto tras tratamiento").applyTo(problem);

            assertThat(problem.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Problem problem = valido().build();

            problem.disable();
            assertThat(problem.isEnabled()).isFalse();
            problem.disable();
            assertThat(problem.isEnabled()).isFalse();

            problem.enable();
            assertThat(problem.isEnabled()).isTrue();
            problem.enable();
            assertThat(problem.isEnabled()).isTrue();
        }
    }
}
