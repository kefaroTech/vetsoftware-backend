package com.vetsoftware.app.baserolepermission.domain;

import static org.assertj.core.api.Assertions.assertThat;
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

@DisplayName("BaseRolePermission — invariantes y ciclo de vida del agregado")
class BaseRolePermissionTest {

    private static final BaseRoleRef VETERINARIO = new BaseRoleRef(1L, "Veterinario", "VET");
    private static final BaseRoleRef ADMINISTRADOR = new BaseRoleRef(2L, "Administrador", "ADMIN");
    private static final BasePermissionRef CREAR_CONSULTA = new BasePermissionRef(10L,
            "Crear consulta", "CONSULTA_CREATE");
    private static final BasePermissionRef EDITAR_CONSULTA = new BasePermissionRef(11L,
            "Editar consulta", "CONSULTA_UPDATE");
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static Builder valido() {
        return new Builder();
    }

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir los
     * cinco argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static final class Builder {
        private Long id = 1L;
        private BaseRoleRef baseRole = VETERINARIO;
        private BasePermissionRef basePermission = CREAR_CONSULTA;
        private LocalDateTime createdDate = CREADO;
        private boolean enabled = true;

        private Builder baseRole(BaseRoleRef v) {
            this.baseRole = v;
            return this;
        }

        private Builder basePermission(BasePermissionRef v) {
            this.basePermission = v;
            return this;
        }

        private BaseRolePermission build() {
            return new BaseRolePermission(id, baseRole, basePermission, createdDate, enabled);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            BaseRolePermission vinculo = valido().build();

            assertThat(vinculo.getId()).isEqualTo(1L);
            assertThat(vinculo.getBaseRole()).isEqualTo(VETERINARIO);
            assertThat(vinculo.getBasePermission()).isEqualTo(CREAR_CONSULTA);
            assertThat(vinculo.getCreatedDate()).isEqualTo(CREADO);
            assertThat(vinculo.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado y con el rol y permiso dados")
        void create_nace_sin_id_habilitado_y_con_el_rol_y_permiso_dados() {
            BaseRolePermission vinculo = BaseRolePermission.create(VETERINARIO, CREAR_CONSULTA);

            assertThat(vinculo.getId()).isNull();
            assertThat(vinculo.getBaseRole()).isEqualTo(VETERINARIO);
            assertThat(vinculo.getBasePermission()).isEqualTo(CREAR_CONSULTA);
            assertThat(vinculo.isEnabled()).isTrue();
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(vinculo.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("baseRole null",
                            (ThrowingCallable) () -> valido().baseRole(null).build(),
                            "baseRole is required"),
                    arguments("basePermission null",
                            (ThrowingCallable) () -> valido().basePermission(null).build(),
                            "basePermission is required"));
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
                    arguments("baseRole null",
                            (Consumer<BaseRolePermission>) v -> v.update(null, EDITAR_CONSULTA),
                            "baseRole is required"),
                    arguments("basePermission null",
                            (Consumer<BaseRolePermission>) v -> v.update(ADMINISTRADOR, null),
                            "basePermission is required"));
        }

        @Test
        @DisplayName("reemplaza rol y permiso, conserva id, createdDate y enabled")
        void reemplaza_rol_y_permiso_conserva_el_resto() {
            BaseRolePermission vinculo = valido().build();

            vinculo.update(ADMINISTRADOR, EDITAR_CONSULTA);

            assertThat(vinculo.getBaseRole()).isEqualTo(ADMINISTRADOR);
            assertThat(vinculo.getBasePermission()).isEqualTo(EDITAR_CONSULTA);
            assertThat(vinculo.getId()).isEqualTo(1L);
            assertThat(vinculo.getCreatedDate()).isEqualTo(CREADO);
            assertThat(vinculo.isEnabled()).isTrue();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza los mismos invariantes que el constructor y no deja el agregado a medias")
        void rechaza_invariantes_sin_dejar_el_agregado_a_medias(String caso,
                Consumer<BaseRolePermission> actualizacion, String mensaje) {
            BaseRolePermission vinculo = valido().build();

            assertThatThrownBy(() -> actualizacion.accept(vinculo))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);

            assertThat(vinculo.getBaseRole()).isEqualTo(VETERINARIO);
            assertThat(vinculo.getBasePermission()).isEqualTo(CREAR_CONSULTA);
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            BaseRolePermission vinculo = valido().build();

            vinculo.disable();
            assertThat(vinculo.isEnabled()).isFalse();
            vinculo.disable();
            assertThat(vinculo.isEnabled()).isFalse();

            vinculo.enable();
            assertThat(vinculo.isEnabled()).isTrue();
            vinculo.enable();
            assertThat(vinculo.isEnabled()).isTrue();
        }
    }
}
