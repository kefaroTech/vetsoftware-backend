package com.vetsoftware.app.basepermission.domain;

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

@DisplayName("BasePermission — invariantes y ciclo de vida del agregado")
class BasePermissionTest {

    private static final SubModuleRef VENTAS = new SubModuleRef(1L, "Ventas", "VEN");
    private static final SubModuleRef INVENTARIO = new SubModuleRef(2L, "Inventario", "INV");
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
        private String name = "Crear factura";
        private String code = "INVOICE_CREATE";
        private SubModuleRef subModule = VENTAS;
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

        private Builder subModule(SubModuleRef v) {
            this.subModule = v;
            return this;
        }

        private BasePermission build() {
            return new BasePermission(id, name, code, subModule, createdDate, null, enabled);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            BasePermission basePermission = valido().build();

            assertThat(basePermission.getId()).isEqualTo(1L);
            assertThat(basePermission.getName()).isEqualTo("Crear factura");
            assertThat(basePermission.getCode()).isEqualTo("INVOICE_CREATE");
            assertThat(basePermission.getSubModule()).isEqualTo(VENTAS);
            assertThat(basePermission.getCreatedDate()).isEqualTo(CREADO);
            assertThat(basePermission.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con el submodulo dado")
        void create_nace_sin_id_habilitado_y_con_el_submodulo_dado() {
            BasePermission basePermission = BasePermission.create("Crear factura", "INVOICE_CREATE",
                    VENTAS);

            assertThat(basePermission.getId()).isNull();
            assertThat(basePermission.getName()).isEqualTo("Crear factura");
            assertThat(basePermission.getCode()).isEqualTo("INVOICE_CREATE");
            assertThat(basePermission.getSubModule()).isEqualTo(VENTAS);
            assertThat(basePermission.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(basePermission.getCreatedDate()).isCloseTo(LocalDateTime.now(),
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
                    arguments("subModule null",
                            (ThrowingCallable) () -> valido().subModule(null).build(),
                            "subModule is required"));
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
                            (Consumer<BasePermission>) p -> p.update(null, "INVOICE_CREATE",
                                    VENTAS),
                            "name is required"),
                    arguments("name vacio",
                            (Consumer<BasePermission>) p -> p.update("", "INVOICE_CREATE", VENTAS),
                            "name is required"),
                    arguments("name de 101 chars",
                            (Consumer<BasePermission>) p -> p.update("x".repeat(101),
                                    "INVOICE_CREATE", VENTAS),
                            "name must be 100 chars or less"),
                    arguments("code null",
                            (Consumer<BasePermission>) p -> p.update("Crear factura", null, VENTAS),
                            "code is required"),
                    arguments("code vacio",
                            (Consumer<BasePermission>) p -> p.update("Crear factura", "", VENTAS),
                            "code is required"),
                    arguments("code de 51 chars",
                            (Consumer<BasePermission>) p -> p.update("Crear factura",
                                    "x".repeat(51), VENTAS),
                            "code must be 50 chars or less"),
                    arguments(
                            "subModule null", (Consumer<BasePermission>) p -> p
                                    .update("Crear factura", "INVOICE_CREATE", null),
                            "subModule is required"));
        }

        @Test
        @DisplayName("reemplaza nombre, codigo y submodulo, conserva id, createdDate y enabled")
        void reemplaza_nombre_codigo_y_submodulo_conserva_el_resto() {
            BasePermission basePermission = valido().build();

            basePermission.update("Editar factura", "INVOICE_UPDATE", INVENTARIO);

            assertThat(basePermission.getName()).isEqualTo("Editar factura");
            assertThat(basePermission.getCode()).isEqualTo("INVOICE_UPDATE");
            assertThat(basePermission.getSubModule()).isEqualTo(INVENTARIO);
            assertThat(basePermission.getId()).isEqualTo(1L);
            assertThat(basePermission.getCreatedDate()).isEqualTo(CREADO);
            assertThat(basePermission.isEnabled()).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza los mismos invariantes que el constructor y no deja el agregado a medias")
        void rechaza_invariantes_sin_dejar_el_agregado_a_medias(String caso,
                Consumer<BasePermission> actualizacion, String mensaje) {
            BasePermission basePermission = valido().build();

            assertThatThrownBy(() -> actualizacion.accept(basePermission))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);

            assertThat(basePermission.getName()).isEqualTo("Crear factura");
            assertThat(basePermission.getCode()).isEqualTo("INVOICE_CREATE");
            assertThat(basePermission.getSubModule()).isEqualTo(VENTAS);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            BasePermission basePermission = valido().build();

            basePermission.disable();
            assertThat(basePermission.isEnabled()).isFalse();
            basePermission.disable();
            assertThat(basePermission.isEnabled()).isFalse();

            basePermission.enable();
            assertThat(basePermission.isEnabled()).isTrue();
            basePermission.enable();
            assertThat(basePermission.isEnabled()).isTrue();
        }
    }
}
