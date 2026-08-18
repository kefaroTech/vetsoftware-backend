package com.vetsoftware.app.supplier.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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

/**
 * Nota de campaña de cobertura: el brief de esta tarea asumia que {@code taxId}
 * llevaba un calculo de digito verificador de NIT colombiano. Leido el fuente
 * via CodeGraph, {@code Supplier.validate} no hace ninguna validacion de
 * formato de {@code taxId} mas alla del limite de longitud (30 caracteres); es
 * un String libre. No hay tal calculo que cubrir hoy — esta clase prueba las
 * invariantes que realmente existen en el constructor.
 */
@DisplayName("Supplier — invariantes y ciclo de vida del agregado")
class SupplierTest {

    private static final CompanyRef CLINICA = new CompanyRef(10L, "Clinica Norte", "900123456");
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static Builder valido() {
        return new Builder();
    }

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir quince
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static final class Builder {
        private Long id = 1L;
        private String name = "Distribuidora Sur";
        private String taxId = "901555444-1";
        private String contactName = "Marta Gil";
        private String phone = "3001234567";
        private String email = "compras@sur.test";
        private String address = "Calle 10 # 5-20";
        private Integer paymentTermsDays = 30;
        private String notes = "Entrega los martes";
        private CompanyRef company = CLINICA;
        private LocalDateTime createdDate = CREADO;
        private LocalDateTime updatedDate;
        private Long updatedBy;
        private Long version = 0L;
        private boolean enabled = true;

        private Builder name(String v) {
            this.name = v;
            return this;
        }

        private Builder taxId(String v) {
            this.taxId = v;
            return this;
        }

        private Builder contactName(String v) {
            this.contactName = v;
            return this;
        }

        private Builder phone(String v) {
            this.phone = v;
            return this;
        }

        private Builder email(String v) {
            this.email = v;
            return this;
        }

        private Builder address(String v) {
            this.address = v;
            return this;
        }

        private Builder paymentTermsDays(Integer v) {
            this.paymentTermsDays = v;
            return this;
        }

        private Builder notes(String v) {
            this.notes = v;
            return this;
        }

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Supplier build() {
            return new Supplier(id, name, taxId, contactName, phone, email, address,
                    paymentTermsDays, notes, company, createdDate, updatedDate, updatedBy, version,
                    enabled);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            Supplier supplier = valido().build();

            assertThat(supplier.getId()).isEqualTo(1L);
            assertThat(supplier.getName()).isEqualTo("Distribuidora Sur");
            assertThat(supplier.getTaxId()).isEqualTo("901555444-1");
            assertThat(supplier.getContactName()).isEqualTo("Marta Gil");
            assertThat(supplier.getPhone()).isEqualTo("3001234567");
            assertThat(supplier.getEmail()).isEqualTo("compras@sur.test");
            assertThat(supplier.getAddress()).isEqualTo("Calle 10 # 5-20");
            assertThat(supplier.getPaymentTermsDays()).isEqualTo(30);
            assertThat(supplier.getNotes()).isEqualTo("Entrega los martes");
            assertThat(supplier.getCompany()).isEqualTo(CLINICA);
            assertThat(supplier.getCreatedDate()).isEqualTo(CREADO);
            assertThat(supplier.getUpdatedDate()).isNull();
            assertThat(supplier.getUpdatedBy()).isNull();
            assertThat(supplier.getVersion()).isZero();
            assertThat(supplier.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("todos los campos opcionales en null son validos")
        void todos_los_campos_opcionales_en_null_son_validos() {
            assertThatCode(() -> valido().taxId(null).contactName(null).phone(null).email(null)
                    .address(null).paymentTermsDays(null).notes(null).build())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("create() nace sin id, habilitado, sin auditoria de actualizacion y con la fecha de hoy")
        void create_nace_sin_id_habilitado_y_con_la_fecha_de_hoy() {
            Supplier supplier = Supplier.create("Distribuidora Sur", "901555444-1", "Marta Gil",
                    "3001234567", "compras@sur.test", "Calle 10 # 5-20", 30, "Entrega los martes",
                    CLINICA);

            assertThat(supplier.getId()).isNull();
            assertThat(supplier.isEnabled()).isTrue();
            assertThat(supplier.getUpdatedDate()).isNull();
            assertThat(supplier.getUpdatedBy()).isNull();
            assertThat(supplier.getVersion()).isNull();
            // create() llama a LocalDateTime.now() directamente: no hay Clock inyectable,
            // asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md — Supplier no esta en la lista explicita pero
            // el patron es el mismo que Animal.create.
            assertThat(supplier.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas por el constructor")
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
                    arguments("name de 151 chars",
                            (ThrowingCallable) () -> valido().name("x".repeat(151)).build(),
                            "name must be 150 chars or less"),
                    arguments("taxId de 31 chars",
                            (ThrowingCallable) () -> valido().taxId("1".repeat(31)).build(),
                            "taxId must be 30 chars or less"),
                    arguments("contactName de 101 chars",
                            (ThrowingCallable) () -> valido().contactName("x".repeat(101)).build(),
                            "contactName must be 100 chars or less"),
                    arguments("phone de 31 chars",
                            (ThrowingCallable) () -> valido().phone("1".repeat(31)).build(),
                            "phone must be 30 chars or less"),
                    arguments("email de 151 chars",
                            (ThrowingCallable) () -> valido().email("x".repeat(151)).build(),
                            "email must be 150 chars or less"),
                    arguments("email sin arroba",
                            (ThrowingCallable) () -> valido().email("no-es-un-correo").build(),
                            "email must be a valid email address"),
                    arguments("address de 201 chars",
                            (ThrowingCallable) () -> valido().address("x".repeat(201)).build(),
                            "address must be 200 chars or less"),
                    arguments("paymentTermsDays negativo",
                            (ThrowingCallable) () -> valido().paymentTermsDays(-1).build(),
                            "paymentTermsDays cannot be negative"),
                    arguments("notes de 501 chars",
                            (ThrowingCallable) () -> valido().notes("x".repeat(501)).build(),
                            "notes must be 500 chars or less"),
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

        static Stream<Arguments> casosEnElLimiteExacto() {
            return Stream.of(
                    arguments("name",
                            (ThrowingCallable) () -> valido().name("x".repeat(150)).build()),
                    arguments("taxId",
                            (ThrowingCallable) () -> valido().taxId("1".repeat(30)).build()),
                    arguments("contactName",
                            (ThrowingCallable) () -> valido().contactName("x".repeat(100)).build()),
                    arguments("phone",
                            (ThrowingCallable) () -> valido().phone("1".repeat(30)).build()),
                    arguments("email",
                            (ThrowingCallable) () -> valido().email("a@" + "x".repeat(145) + ".co")
                                    .build()),
                    arguments("address",
                            (ThrowingCallable) () -> valido().address("x".repeat(200)).build()),
                    arguments("notes",
                            (ThrowingCallable) () -> valido().notes("x".repeat(500)).build()),
                    arguments("paymentTermsDays en cero",
                            (ThrowingCallable) () -> valido().paymentTermsDays(0).build()));
        }

        @ParameterizedTest(name = "{0} en el limite exacto se acepta")
        @MethodSource("casosEnElLimiteExacto")
        @DisplayName("el limite exacto de cada campo se acepta")
        void el_limite_exacto_se_acepta(String caso, ThrowingCallable construccion) {
            assertThatCode(construccion).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("update()")
    class Actualizacion {

        @Test
        @DisplayName("reemplaza cada campo editable y fija la auditoria de actualizacion")
        void reemplaza_cada_campo_editable() {
            Supplier supplier = valido().build();
            CompanyRef otraEmpresa = new CompanyRef(20L, "Clinica Sur", "800111222");

            supplier.update("Insumos Norte", "800111222-3", "Carlos Ruiz", "3009876543",
                    "compras@norte.test", "Carrera 7 # 8-9", 45, "Entrega los viernes", otraEmpresa,
                    99L, 5L);

            assertThat(supplier.getName()).isEqualTo("Insumos Norte");
            assertThat(supplier.getTaxId()).isEqualTo("800111222-3");
            assertThat(supplier.getContactName()).isEqualTo("Carlos Ruiz");
            assertThat(supplier.getPhone()).isEqualTo("3009876543");
            assertThat(supplier.getEmail()).isEqualTo("compras@norte.test");
            assertThat(supplier.getAddress()).isEqualTo("Carrera 7 # 8-9");
            assertThat(supplier.getPaymentTermsDays()).isEqualTo(45);
            assertThat(supplier.getNotes()).isEqualTo("Entrega los viernes");
            assertThat(supplier.getCompany()).isEqualTo(otraEmpresa);
            assertThat(supplier.getUpdatedBy()).isEqualTo(99L);
            assertThat(supplier.getVersion()).isEqualTo(5L);
            // update() tambien llama a LocalDateTime.now() directamente: misma deuda que
            // create().
            assertThat(supplier.getUpdatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("no toca id ni createdDate: esos campos son inmutables tras crear")
        void no_toca_id_ni_created_date() {
            Supplier supplier = valido().build();

            supplier.update("Insumos Norte", null, null, null, null, null, null, null, CLINICA, 1L,
                    1L);

            assertThat(supplier.getId()).isEqualTo(1L);
            assertThat(supplier.getCreatedDate()).isEqualTo(CREADO);
        }

        @Test
        @DisplayName("rechaza los mismos datos invalidos que el constructor")
        void rechaza_los_mismos_datos_invalidos_que_el_constructor() {
            Supplier supplier = valido().build();

            assertThatThrownBy(() -> supplier.update("", "901555444-1", "Marta Gil", "3001234567",
                    "compras@sur.test", "Calle 10 # 5-20", 30, "Entrega los martes", CLINICA, 1L,
                    1L)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }
    }

    @Nested
    @DisplayName("enable() / disable()")
    class Habilitacion {

        @Test
        @DisplayName("disable() apaga el flag sin tocar el resto del agregado")
        void disable_apaga_el_flag() {
            Supplier supplier = valido().build();

            supplier.disable();

            assertThat(supplier.isEnabled()).isFalse();
            assertThat(supplier.getName()).isEqualTo("Distribuidora Sur");
        }

        @Test
        @DisplayName("enable() reactiva un proveedor pausado")
        void enable_reactiva_un_proveedor_pausado() {
            Supplier supplier = valido().build();
            supplier.disable();

            supplier.enable();

            assertThat(supplier.isEnabled()).isTrue();
        }
    }
}
