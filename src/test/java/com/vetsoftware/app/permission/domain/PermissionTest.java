package com.vetsoftware.app.permission.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.permission.testsupport.PermissionMother;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Permission — invariantes y ciclo de vida")
class PermissionTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = 1L;
        private String name = "Crear factura";
        private String code = "billing.create";
        private CompanyRef company = PermissionMother.CLINICA;
        private SubModuleRef subModule = PermissionMother.INVENTARIO;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder code(String v) {
            this.code = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Builder subModule(SubModuleRef v) {
            this.subModule = v;
            return this;
        }

        private Permission build() {
            return new Permission(id, name, code, company, subModule, CREADO, true);
        }

        private void applyTo(Permission permission) {
            permission.update(name, code, company, subModule);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Permission permission = valido().build();

            assertThat(permission.getId()).isEqualTo(1L);
            assertThat(permission.getName()).isEqualTo("Crear factura");
            assertThat(permission.getCode()).isEqualTo("billing.create");
            assertThat(permission.getCompany()).isEqualTo(PermissionMother.CLINICA);
            assertThat(permission.getSubModule()).isEqualTo(PermissionMother.INVENTARIO);
            assertThat(permission.getCreatedDate()).isEqualTo(CREADO);
            assertThat(permission.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("create() nace sin id y habilitado")
        void create_nace_sin_id_y_habilitado() {
            Permission permission = Permission.create("Crear factura", "billing.create",
                    PermissionMother.CLINICA, PermissionMother.INVENTARIO);

            assertThat(permission.getId()).isNull();
            assertThat(permission.isEnabled()).isTrue();
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
                    arguments("company null",
                            (ThrowingCallable) () -> valido().company(null).build(),
                            "company is required"),
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

        @ParameterizedTest(name = "longitud {0}")
        @ValueSource(ints = {1, 100})
        @DisplayName("name en el limite exacto se acepta")
        void name_en_el_limite_exacto_se_acepta(int longitud) {
            assertThatCode(() -> valido().name("x".repeat(longitud)).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("code de 50 chars se acepta")
        void code_de_50_chars_se_acepta() {
            assertThatCode(() -> valido().code("x".repeat(50)).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("reemplaza los campos mutables y conserva id y createdDate")
        void reemplaza_los_campos_mutables_y_conserva_id_y_created_date() {
            Permission permission = valido().build();

            valido().name("Editar factura").code("billing.update")
                    .company(PermissionMother.OTRA_CLINICA).subModule(PermissionMother.FACTURACION)
                    .applyTo(permission);

            assertThat(permission.getName()).isEqualTo("Editar factura");
            assertThat(permission.getCode()).isEqualTo("billing.update");
            assertThat(permission.getCompany()).isEqualTo(PermissionMother.OTRA_CLINICA);
            assertThat(permission.getSubModule()).isEqualTo(PermissionMother.FACTURACION);
            assertThat(permission.getId()).isEqualTo(1L);
            assertThat(permission.getCreatedDate()).isEqualTo(CREADO);
        }

        @Test
        @DisplayName("un update invalido no deja el agregado a medias")
        void un_update_invalido_no_deja_el_agregado_a_medias() {
            Permission permission = valido().build();

            // El nombre es valido y el code no: si validate() no corriera ANTES de
            // asignar, el permiso se quedaria con el nombre nuevo y el code viejo.
            assertThatThrownBy(() -> valido().name("Editar factura").code(null).applyTo(permission))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(permission.getName()).isEqualTo("Crear factura");
            assertThat(permission.getCode()).isEqualTo("billing.create");
        }

        @Test
        @DisplayName("no toca el estado de habilitacion")
        void no_toca_el_estado_de_habilitacion() {
            Permission permission = valido().build();
            permission.disable();

            valido().name("Editar factura").applyTo(permission);

            assertThat(permission.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan el estado y son idempotentes")
        void disable_y_enable_alternan_el_estado_y_son_idempotentes() {
            Permission permission = valido().build();

            permission.disable();
            assertThat(permission.isEnabled()).isFalse();
            permission.disable();
            assertThat(permission.isEnabled()).isFalse();

            permission.enable();
            assertThat(permission.isEnabled()).isTrue();
            permission.enable();
            assertThat(permission.isEnabled()).isTrue();
        }
    }
}
