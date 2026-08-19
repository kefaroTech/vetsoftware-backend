package com.vetsoftware.app.submodule.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SubModule — invariantes y ciclo de vida del agregado")
class SubModuleTest {

    private static final ModuleRef FACTURACION = new ModuleRef(1L, "Facturacion", "FACT");
    private static final ModuleRef INVENTARIO = new ModuleRef(2L, "Inventario", "INV");
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static Builder valido() {
        return new Builder();
    }

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir los
     * seis argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static final class Builder {
        private Long id = 1L;
        private String name = "Reportes";
        private String code = "REP";
        private ModuleRef module = FACTURACION;
        private LocalDateTime createdDate = CREADO;
        private boolean enabled = true;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder code(String v) {
            this.code = v;
            return this;
        }

        private Builder module(ModuleRef v) {
            this.module = v;
            return this;
        }

        private SubModule build() {
            return new SubModule(id, name, code, module, createdDate, null, enabled);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            SubModule subModule = valido().build();

            assertThat(subModule.getId()).isEqualTo(1L);
            assertThat(subModule.getName()).isEqualTo("Reportes");
            assertThat(subModule.getCode()).isEqualTo("REP");
            assertThat(subModule.getModule()).isEqualTo(FACTURACION);
            assertThat(subModule.getCreatedDate()).isEqualTo(CREADO);
            assertThat(subModule.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con el modulo dado")
        void create_nace_sin_id_habilitado_y_con_el_modulo_dado() {
            SubModule subModule = SubModule.create("Reportes", "REP", FACTURACION);

            assertThat(subModule.getId()).isNull();
            assertThat(subModule.getName()).isEqualTo("Reportes");
            assertThat(subModule.getCode()).isEqualTo("REP");
            assertThat(subModule.getModule()).isEqualTo(FACTURACION);
            assertThat(subModule.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(subModule.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 100})
        @DisplayName("name en el limite exacto se acepta")
        void name_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().name("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 50})
        @DisplayName("code en el limite exacto se acepta")
        void code_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().code("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
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
                    arguments("code null", (ThrowingCallable) () -> valido().code(null).build(),
                            "code is required"),
                    arguments("code vacio", (ThrowingCallable) () -> valido().code("").build(),
                            "code is required"),
                    arguments("code en blanco",
                            (ThrowingCallable) () -> valido().code("   ").build(),
                            "code is required"),
                    arguments("code de 51 chars",
                            (ThrowingCallable) () -> valido().code("x".repeat(51)).build(),
                            "code must be 50 chars or less"),
                    arguments("module null", (ThrowingCallable) () -> valido().module(null).build(),
                            "module is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("name null",
                            (Consumer<SubModule>) s -> s.update(null, "REP", FACTURACION),
                            "name is required"),
                    arguments("name vacio",
                            (Consumer<SubModule>) s -> s.update("", "REP", FACTURACION),
                            "name is required"),
                    arguments("name de 101 chars",
                            (Consumer<SubModule>) s -> s.update("x".repeat(101), "REP",
                                    FACTURACION),
                            "name must be 100 chars or less"),
                    arguments("code null",
                            (Consumer<SubModule>) s -> s.update("Reportes", null, FACTURACION),
                            "code is required"),
                    arguments("code vacio",
                            (Consumer<SubModule>) s -> s.update("Reportes", "", FACTURACION),
                            "code is required"),
                    arguments("code de 51 chars",
                            (Consumer<SubModule>) s -> s.update("Reportes", "x".repeat(51),
                                    FACTURACION),
                            "code must be 50 chars or less"),
                    arguments("module null",
                            (Consumer<SubModule>) s -> s.update("Reportes", "REP", null),
                            "module is required"));
        }

        @Test
        @DisplayName("reemplaza nombre, codigo y modulo, conserva id, createdDate y enabled")
        void reemplaza_nombre_codigo_y_modulo_conserva_el_resto() {
            SubModule subModule = valido().build();

            subModule.update("Facturacion Avanzada", "FACT-AV", INVENTARIO);

            assertThat(subModule.getName()).isEqualTo("Facturacion Avanzada");
            assertThat(subModule.getCode()).isEqualTo("FACT-AV");
            assertThat(subModule.getModule()).isEqualTo(INVENTARIO);
            assertThat(subModule.getId()).isEqualTo(1L);
            assertThat(subModule.getCreatedDate()).isEqualTo(CREADO);
            assertThat(subModule.isEnabled()).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza los mismos invariantes que el constructor y no deja el agregado a medias")
        void rechaza_invariantes_sin_dejar_el_agregado_a_medias(String caso,
                Consumer<SubModule> actualizacion, String mensaje) {
            SubModule subModule = valido().build();

            assertThatThrownBy(() -> actualizacion.accept(subModule))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);

            assertThat(subModule.getName()).isEqualTo("Reportes");
            assertThat(subModule.getCode()).isEqualTo("REP");
            assertThat(subModule.getModule()).isEqualTo(FACTURACION);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            SubModule subModule = valido().build();

            subModule.disable();
            assertThat(subModule.isEnabled()).isFalse();
            subModule.disable();
            assertThat(subModule.isEnabled()).isFalse();

            subModule.enable();
            assertThat(subModule.isEnabled()).isTrue();
            subModule.enable();
            assertThat(subModule.isEnabled()).isTrue();
        }
    }
}
