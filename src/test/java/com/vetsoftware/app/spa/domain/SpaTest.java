package com.vetsoftware.app.spa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.spa.testsupport.SpaMother;
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

@DisplayName("Spa — entidad de dominio")
class SpaTest {

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
        private SpaTypeRef spaType = SpaMother.BANO_BASICO;
        private String reason = "Baño mensual";
        private String details = "Shampoo hipoalergenico";
        private String observations = "Sin novedades";
        private SpaStatus status = SpaStatus.AGENDADA;
        private AnimalRef animal = SpaMother.FIRULAIS;
        private CompanyRef company = SpaMother.CLINICA;
        private LocalDateTime createdDate = SpaMother.CREADO;
        private boolean enabled = true;

        private Builder date(LocalDate v) {
            this.date = v;
            return this;
        }

        private Builder spaType(SpaTypeRef v) {
            this.spaType = v;
            return this;
        }

        private Builder reason(String v) {
            this.reason = v;
            return this;
        }

        private Builder details(String v) {
            this.details = v;
            return this;
        }

        private Builder observations(String v) {
            this.observations = v;
            return this;
        }

        private Builder status(SpaStatus v) {
            this.status = v;
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

        private Spa build() {
            return new Spa(id, date, spaType, reason, details, observations, status, animal,
                    company, createdDate, null, enabled);
        }

        private void applyTo(Spa spa) {
            spa.update(date, spaType, reason, details, observations, animal, company);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Spa spa = valido().build();

            assertThat(spa.getId()).isEqualTo(5L);
            assertThat(spa.getDate()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(spa.getSpaType()).isEqualTo(SpaMother.BANO_BASICO);
            assertThat(spa.getReason()).isEqualTo("Baño mensual");
            assertThat(spa.getDetails()).isEqualTo("Shampoo hipoalergenico");
            assertThat(spa.getObservations()).isEqualTo("Sin novedades");
            assertThat(spa.getStatus()).isEqualTo(SpaStatus.AGENDADA);
            assertThat(spa.getAnimal()).isEqualTo(SpaMother.FIRULAIS);
            assertThat(spa.getCompany()).isEqualTo(SpaMother.CLINICA);
            assertThat(spa.getCreatedDate()).isEqualTo(SpaMother.CREADO);
            assertThat(spa.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, agendado, habilitado y con la fecha de hoy")
        void create_nace_sin_id_agendado_habilitado_y_con_la_fecha_de_hoy() {
            Spa spa = Spa.create(LocalDate.of(2026, 2, 1), SpaMother.BANO_BASICO, "Baño mensual",
                    "Shampoo hipoalergenico", "Sin novedades", SpaMother.FIRULAIS,
                    SpaMother.CLINICA);

            assertThat(spa.getId()).isNull();
            assertThat(spa.getStatus()).isEqualTo(SpaStatus.AGENDADA);
            assertThat(spa.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada
            // en "Determinismo" del CLAUDE.md.
            assertThat(spa.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @ParameterizedTest
        @EnumSource(SpaStatus.class)
        @DisplayName("cualquier estado del enum es un estado valido en el constructor")
        void cualquier_estado_del_enum_es_valido(SpaStatus status) {
            assertThatCode(() -> valido().status(status).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("date null", (ThrowingCallable) () -> valido().date(null).build(),
                            "date is required"),
                    arguments("spaType null",
                            (ThrowingCallable) () -> valido().spaType(null).build(),
                            "spaType is required"),
                    arguments("reason null", (ThrowingCallable) () -> valido().reason(null).build(),
                            "reason is required"),
                    arguments("reason en blanco",
                            (ThrowingCallable) () -> valido().reason("   ").build(),
                            "reason is required"),
                    arguments("reason de 2001 chars",
                            (ThrowingCallable) () -> valido().reason("x".repeat(2001)).build(),
                            "reason must be 2000 chars or less"),
                    arguments("details null",
                            (ThrowingCallable) () -> valido().details(null).build(),
                            "details is required"),
                    arguments("details en blanco",
                            (ThrowingCallable) () -> valido().details("   ").build(),
                            "details is required"),
                    arguments("details de 2001 chars",
                            (ThrowingCallable) () -> valido().details("x".repeat(2001)).build(),
                            "details must be 2000 chars or less"),
                    arguments("observations null",
                            (ThrowingCallable) () -> valido().observations(null).build(),
                            "observations is required"),
                    arguments("observations en blanco",
                            (ThrowingCallable) () -> valido().observations("   ").build(),
                            "observations is required"),
                    arguments("observations de 2001 chars",
                            (ThrowingCallable) () -> valido().observations("x".repeat(2001))
                                    .build(),
                            "observations must be 2000 chars or less"),
                    arguments("status null", (ThrowingCallable) () -> valido().status(null).build(),
                            "status is required"),
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
        @DisplayName("reason de 2000 chars exactos se acepta")
        void reason_de_2000_chars_se_acepta() {
            assertThatCode(() -> valido().reason("x".repeat(2000)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("details de 2000 chars exactos se acepta")
        void details_de_2000_chars_se_acepta() {
            assertThatCode(() -> valido().details("x".repeat(2000)).build())
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
        @DisplayName("reemplaza los campos mutables y conserva id, status y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_status_y_created_date() {
            Spa spa = valido().build();

            valido().date(LocalDate.of(2026, 3, 1)).spaType(SpaMother.CORTE_DE_PELO)
                    .reason("Corte de verano").details("Tijera").observations("Nervioso")
                    .animal(SpaMother.MICHI).company(SpaMother.OTRA_CLINICA).applyTo(spa);

            assertThat(spa.getDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(spa.getSpaType()).isEqualTo(SpaMother.CORTE_DE_PELO);
            assertThat(spa.getReason()).isEqualTo("Corte de verano");
            assertThat(spa.getDetails()).isEqualTo("Tijera");
            assertThat(spa.getObservations()).isEqualTo("Nervioso");
            assertThat(spa.getAnimal()).isEqualTo(SpaMother.MICHI);
            assertThat(spa.getCompany()).isEqualTo(SpaMother.OTRA_CLINICA);
            assertThat(spa.getId()).isEqualTo(5L);
            assertThat(spa.getStatus()).isEqualTo(SpaStatus.AGENDADA);
            assertThat(spa.getCreatedDate()).isEqualTo(SpaMother.CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            Spa spa = valido().build();

            // El spaType es valido y la fecha no: si validate() no corriera ANTES de
            // asignar, el spa se quedaria con el spaType nuevo y la fecha vieja.
            assertThatThrownBy(
                    () -> valido().spaType(SpaMother.CORTE_DE_PELO).date(null).applyTo(spa))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(spa.getSpaType()).isEqualTo(SpaMother.BANO_BASICO);
            assertThat(spa.getDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        }

        @Test
        @DisplayName("no toca el estado de agenda ni el de habilitacion")
        void no_toca_el_estado_de_agenda_ni_el_de_habilitacion() {
            Spa spa = valido().build();
            spa.changeStatus(SpaStatus.COMPLETADO);
            spa.disable();

            valido().details("Nuevos detalles").applyTo(spa);

            assertThat(spa.getStatus()).isEqualTo(SpaStatus.COMPLETADO);
            assertThat(spa.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class CambioDeEstado {

        @ParameterizedTest
        @EnumSource(SpaStatus.class)
        @DisplayName("acepta cualquier estado del enum")
        void acepta_cualquier_estado_del_enum(SpaStatus status) {
            Spa spa = valido().build();

            spa.changeStatus(status);

            assertThat(spa.getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("rechaza un estado nulo sin tocar el estado actual")
        void rechaza_un_estado_nulo_sin_tocar_el_estado_actual() {
            Spa spa = valido().build();

            assertThatThrownBy(() -> spa.changeStatus(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status is required");

            assertThat(spa.getStatus()).isEqualTo(SpaStatus.AGENDADA);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Spa spa = valido().build();

            spa.disable();
            assertThat(spa.isEnabled()).isFalse();
            spa.disable();
            assertThat(spa.isEnabled()).isFalse();

            spa.enable();
            assertThat(spa.isEnabled()).isTrue();
            spa.enable();
            assertThat(spa.isEnabled()).isTrue();
        }
    }
}
