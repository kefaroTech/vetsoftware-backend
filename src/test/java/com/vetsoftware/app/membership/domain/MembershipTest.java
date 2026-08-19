package com.vetsoftware.app.membership.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.membership.testsupport.MembershipMother;
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
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Membership — invariantes y ciclo de vida del agregado")
class MembershipTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir seis
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 1L;
        private String name = "Plan Oro";
        private MembershipStatus status = MembershipStatus.ACTIVE;
        private boolean mandatory;
        private LocalDateTime createdDate = MembershipMother.CREADO;
        private boolean enabled = true;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder status(MembershipStatus v) {
            this.status = v;
            return this;
        }

        private Builder mandatory(boolean v) {
            this.mandatory = v;
            return this;
        }

        private Membership build() {
            return new Membership(id, name, status, mandatory, createdDate, null, enabled);
        }

        private void applyTo(Membership membership) {
            membership.update(name, status, mandatory);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Membership membership = valido().build();

            assertThat(membership.getId()).isEqualTo(1L);
            assertThat(membership.getName()).isEqualTo("Plan Oro");
            assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
            assertThat(membership.isMandatory()).isFalse();
            assertThat(membership.getCreatedDate()).isEqualTo(MembershipMother.CREADO);
            assertThat(membership.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitada y con la fecha del reloj")
        void create_nace_sin_id_habilitada_y_con_la_fecha_del_reloj() {
            Membership membership = Membership.create("Plan Oro", MembershipStatus.ACTIVE, true);

            assertThat(membership.getId()).isNull();
            assertThat(membership.isEnabled()).isTrue();
            assertThat(membership.isMandatory()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(membership.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null", (ThrowingCallable) () -> valido().name(null).build(),
                            "name is required"),
                    arguments("name vacio", (ThrowingCallable) () -> valido().name("").build(),
                            "name is required"),
                    arguments("name en blanco",
                            (ThrowingCallable) () -> valido().name("   ").build(),
                            "name is required"),
                    arguments("name de 101 chars",
                            (ThrowingCallable) () -> valido().name("x".repeat(101)).build(),
                            "name must be 100 chars or less"),
                    arguments("status null", (ThrowingCallable) () -> valido().status(null).build(),
                            "status is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 100})
        @DisplayName("name en el limite exacto se acepta")
        void name_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().name("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id, createdDate y enabled")
        void reemplaza_los_campos_mutables_y_conserva_id_created_date_y_enabled() {
            Membership membership = valido().build();

            valido().name("Plan Platino").status(MembershipStatus.DEPRECATED).mandatory(true)
                    .applyTo(membership);

            assertThat(membership.getName()).isEqualTo("Plan Platino");
            assertThat(membership.getStatus()).isEqualTo(MembershipStatus.DEPRECATED);
            assertThat(membership.isMandatory()).isTrue();
            assertThat(membership.getId()).isEqualTo(1L);
            assertThat(membership.getCreatedDate()).isEqualTo(MembershipMother.CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            Membership membership = valido().build();

            // El nombre es valido y el estado no: si validate() no corriera ANTES de
            // asignar, la membresia se quedaria con el nombre nuevo y el estado viejo.
            assertThatThrownBy(() -> valido().name("Plan Platino").status(null).applyTo(membership))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(membership.getName()).isEqualTo("Plan Oro");
            assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            Membership membership = valido().build();
            membership.disable();

            valido().name("Plan Platino").applyTo(membership);

            assertThat(membership.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Membership membership = valido().build();

            membership.disable();
            assertThat(membership.isEnabled()).isFalse();
            membership.disable();
            assertThat(membership.isEnabled()).isFalse();

            membership.enable();
            assertThat(membership.isEnabled()).isTrue();
            membership.enable();
            assertThat(membership.isEnabled()).isTrue();
        }
    }
}
