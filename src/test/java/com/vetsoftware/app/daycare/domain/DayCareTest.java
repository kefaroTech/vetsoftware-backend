package com.vetsoftware.app.daycare.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.daycare.testsupport.DayCareMother;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("DayCare — entidad de dominio")
class DayCareTest {

    /*
     * Builder local: aisla la construccion "valida" en un solo sitio para que cada
     * caso invalido cambie un unico argumento, que es como se detecta que el
     * mensaje de error corresponde al campo realmente mutado.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 5L;
        private LocalDate date = LocalDate.of(2026, 2, 1);
        private LocalDate startDate = LocalDate.of(2026, 2, 1);
        private LocalDate endDate = LocalDate.of(2026, 2, 3);
        private DayCareType type = DayCareType.DAYCARE;
        private String objects = "Correa, plato";
        private String observations = "Sin novedades";
        private AnimalRef animal = DayCareMother.FIRULAIS;
        private CompanyRef company = DayCareMother.CLINICA;
        private LocalDateTime createdDate = DayCareMother.CREADO;
        private boolean enabled = true;

        private Builder date(LocalDate v) {
            this.date = v;
            return this;
        }

        private Builder startDate(LocalDate v) {
            this.startDate = v;
            return this;
        }

        private Builder endDate(LocalDate v) {
            this.endDate = v;
            return this;
        }

        private Builder type(DayCareType v) {
            this.type = v;
            return this;
        }

        private Builder objects(String v) {
            this.objects = v;
            return this;
        }

        private Builder observations(String v) {
            this.observations = v;
            return this;
        }

        private Builder animal(AnimalRef v) {
            this.animal = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private DayCare build() {
            return new DayCare(id, date, startDate, endDate, type, objects, observations, animal,
                    company, createdDate, null, enabled);
        }

        private void applyTo(DayCare dayCare) {
            dayCare.update(date, startDate, endDate, type, objects, observations, animal, company);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            DayCare dayCare = valido().build();

            assertThat(dayCare.getId()).isEqualTo(5L);
            assertThat(dayCare.getDate()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(dayCare.getStartDate()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(dayCare.getEndDate()).isEqualTo(LocalDate.of(2026, 2, 3));
            assertThat(dayCare.getType()).isEqualTo(DayCareType.DAYCARE);
            assertThat(dayCare.getObjects()).isEqualTo("Correa, plato");
            assertThat(dayCare.getObservations()).isEqualTo("Sin novedades");
            assertThat(dayCare.getAnimal()).isEqualTo(DayCareMother.FIRULAIS);
            assertThat(dayCare.getCompany()).isEqualTo(DayCareMother.CLINICA);
            assertThat(dayCare.getCreatedDate()).isEqualTo(DayCareMother.CREADO);
            assertThat(dayCare.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con la fecha de hoy")
        void create_nace_sin_id_habilitado_y_con_la_fecha_de_hoy() {
            DayCare dayCare = DayCare.create(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
                    LocalDate.of(2026, 2, 3), DayCareType.DAYCARE, "Correa", "Sin novedades",
                    DayCareMother.FIRULAIS, DayCareMother.CLINICA);

            assertThat(dayCare.getId()).isNull();
            assertThat(dayCare.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada
            // en "Determinismo" del CLAUDE.md.
            assertThat(dayCare.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("endDate es opcional: un hotel sin fecha de salida definida es valido")
        void end_date_es_opcional() {
            assertThatCode(() -> valido().endDate(null).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("objects y observations son opcionales")
        void objects_y_observations_son_opcionales() {
            assertThatCode(() -> valido().objects(null).observations(null).build())
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(DayCareType.class)
        @DisplayName("cualquier tipo del enum es un tipo valido")
        void cualquier_tipo_del_enum_es_valido(DayCareType tipo) {
            assertThatCode(() -> valido().type(tipo).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("date null", (ThrowingCallable) () -> valido().date(null).build(),
                            "date is required"),
                    arguments("startDate null",
                            (ThrowingCallable) () -> valido().startDate(null).build(),
                            "startDate is required"),
                    arguments("endDate antes de startDate",
                            (ThrowingCallable) () -> valido().startDate(LocalDate.of(2026, 2, 10))
                                    .endDate(LocalDate.of(2026, 2, 1)).build(),
                            "endDate cannot be before startDate"),
                    arguments("type null", (ThrowingCallable) () -> valido().type(null).build(),
                            "type is required"),
                    arguments("objects de 1001 chars",
                            (ThrowingCallable) () -> valido().objects("x".repeat(1001)).build(),
                            "objects must be 1000 chars or less"),
                    arguments("observations de 2001 chars",
                            (ThrowingCallable) () -> valido().observations("x".repeat(2001))
                                    .build(),
                            "observations must be 2000 chars or less"),
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
        @DisplayName("endDate igual a startDate se acepta (una sola jornada)")
        void end_date_igual_a_start_date_se_acepta() {
            assertThatCode(() -> valido().startDate(LocalDate.of(2026, 2, 1))
                    .endDate(LocalDate.of(2026, 2, 1)).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("objects de 1000 chars exactos se acepta")
        void objects_de_1000_chars_se_acepta() {
            assertThatCode(() -> valido().objects("x".repeat(1000)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("observations de 2000 chars exactos se acepta")
        void observations_de_2000_chars_se_acepta() {
            assertThatCode(() -> valido().observations("x".repeat(2000)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            DayCare dayCare = valido().build();

            valido().date(LocalDate.of(2026, 3, 1)).startDate(LocalDate.of(2026, 3, 1))
                    .endDate(LocalDate.of(2026, 3, 5)).type(DayCareType.HOTEL).objects("Cama")
                    .observations("Alergico").animal(DayCareMother.MICHI)
                    .company(DayCareMother.OTRA_CLINICA).applyTo(dayCare);

            assertThat(dayCare.getDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(dayCare.getType()).isEqualTo(DayCareType.HOTEL);
            assertThat(dayCare.getObjects()).isEqualTo("Cama");
            assertThat(dayCare.getObservations()).isEqualTo("Alergico");
            assertThat(dayCare.getAnimal()).isEqualTo(DayCareMother.MICHI);
            assertThat(dayCare.getCompany()).isEqualTo(DayCareMother.OTRA_CLINICA);
            assertThat(dayCare.getId()).isEqualTo(5L);
            assertThat(dayCare.getCreatedDate()).isEqualTo(DayCareMother.CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            DayCare dayCare = valido().build();

            // El tipo es valido y la fecha no: si validate() no corriera ANTES de
            // asignar, el daycare se quedaria con el tipo nuevo y la fecha vieja.
            assertThatThrownBy(() -> valido().type(DayCareType.HOTEL).date(null).applyTo(dayCare))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(dayCare.getType()).isEqualTo(DayCareType.DAYCARE);
            assertThat(dayCare.getDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            DayCare dayCare = valido().build();
            dayCare.disable();

            valido().objects("Nuevo objeto").applyTo(dayCare);

            assertThat(dayCare.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            DayCare dayCare = valido().build();

            dayCare.disable();
            assertThat(dayCare.isEnabled()).isFalse();
            dayCare.disable();
            assertThat(dayCare.isEnabled()).isFalse();

            dayCare.enable();
            assertThat(dayCare.isEnabled()).isTrue();
            dayCare.enable();
            assertThat(dayCare.isEnabled()).isTrue();
        }
    }
}
