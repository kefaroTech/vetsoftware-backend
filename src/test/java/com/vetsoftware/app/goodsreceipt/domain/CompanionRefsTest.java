package com.vetsoftware.app.goodsreceipt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Los companion VO son la unica frontera con las otras features: si dejan pasar
 * un id nulo o un nombre en blanco, el agregado se construye y el defecto
 * aparece en el PDF o en el kardex, no aqui.
 */
@DisplayName("Companion VOs de goodsreceipt — invariantes de las referencias cruzadas")
class CompanionRefsTest {

    @Nested
    @DisplayName("CompanyRef")
    class Company {

        static Stream<Arguments> invalidos() {
            return Stream.of(
                    arguments("id nulo",
                            (ThrowingCallable) () -> new CompanyRef(null, "Clinica", "NIT-900"),
                            "company id is required"),
                    arguments("nombre nulo",
                            (ThrowingCallable) () -> new CompanyRef(9L, null, "NIT-900"),
                            "company name is required"),
                    arguments("nombre vacio",
                            (ThrowingCallable) () -> new CompanyRef(9L, "", "NIT-900"),
                            "company name is required"),
                    arguments("nombre en blanco",
                            (ThrowingCallable) () -> new CompanyRef(9L, "   ", "NIT-900"),
                            "company name is required"),
                    arguments("identificador nulo",
                            (ThrowingCallable) () -> new CompanyRef(9L, "Clinica", null),
                            "company identifier is required"),
                    arguments("identificador en blanco",
                            (ThrowingCallable) () -> new CompanyRef(9L, "Clinica", "  "),
                            "company identifier is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidos")
        @DisplayName("rechaza la referencia incompleta a la empresa")
        void rechaza_referencias_incompletas(String caso, ThrowingCallable construccion,
                String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("conserva id, nombre e identificador")
        void conserva_los_campos() {
            CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900123");

            assertThat(ref.id()).isEqualTo(9L);
            assertThat(ref.name()).isEqualTo("Clinica Norte");
            assertThat(ref.identifier()).isEqualTo("NIT-900123");
        }

        @Test
        @DisplayName("dos referencias con los mismos datos son iguales")
        void igualdad_por_valor() {
            assertThat(new CompanyRef(9L, "Clinica Norte", "NIT-900123"))
                    .isEqualTo(new CompanyRef(9L, "Clinica Norte", "NIT-900123"));
        }
    }

    @Nested
    @DisplayName("BranchRef")
    class Branch {

        static Stream<Arguments> invalidos() {
            return Stream.of(
                    arguments("id nulo", (ThrowingCallable) () -> new BranchRef(null, "Sede"),
                            "branch id is required"),
                    arguments("nombre nulo", (ThrowingCallable) () -> new BranchRef(4L, null),
                            "branch name is required"),
                    arguments("nombre vacio", (ThrowingCallable) () -> new BranchRef(4L, ""),
                            "branch name is required"),
                    arguments("nombre en blanco", (ThrowingCallable) () -> new BranchRef(4L, "\t"),
                            "branch name is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidos")
        @DisplayName("rechaza la referencia incompleta a la sede")
        void rechaza_referencias_incompletas(String caso, ThrowingCallable construccion,
                String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("conserva id y nombre")
        void conserva_los_campos() {
            BranchRef ref = new BranchRef(4L, "Sede Norte");

            assertThat(ref.id()).isEqualTo(4L);
            assertThat(ref.name()).isEqualTo("Sede Norte");
        }
    }

    @Nested
    @DisplayName("SupplierRef")
    class Supplier {

        static Stream<Arguments> invalidos() {
            return Stream.of(
                    arguments("id nulo",
                            (ThrowingCallable) () -> new SupplierRef(null, "Distribuidora"),
                            "supplier id is required"),
                    arguments("nombre nulo", (ThrowingCallable) () -> new SupplierRef(7L, null),
                            "supplier name is required"),
                    arguments("nombre en blanco",
                            (ThrowingCallable) () -> new SupplierRef(7L, "  "),
                            "supplier name is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidos")
        @DisplayName("rechaza la referencia incompleta al proveedor")
        void rechaza_referencias_incompletas(String caso, ThrowingCallable construccion,
                String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("conserva id y nombre")
        void conserva_los_campos() {
            SupplierRef ref = new SupplierRef(7L, "Distribuidora Vet");

            assertThat(ref.id()).isEqualTo(7L);
            assertThat(ref.name()).isEqualTo("Distribuidora Vet");
        }
    }

    @Nested
    @DisplayName("ProductRef")
    class Product {

        static Stream<Arguments> invalidos() {
            return Stream.of(
                    arguments("id nulo",
                            (ThrowingCallable) () -> new ProductRef(null, "Vacuna", "P-021"),
                            "product id is required"),
                    arguments("nombre nulo",
                            (ThrowingCallable) () -> new ProductRef(21L, null, "P-021"),
                            "product name is required"),
                    arguments("nombre en blanco",
                            (ThrowingCallable) () -> new ProductRef(21L, " ", "P-021"),
                            "product name is required"),
                    arguments("codigo nulo",
                            (ThrowingCallable) () -> new ProductRef(21L, "Vacuna", null),
                            "product code is required"),
                    arguments("codigo en blanco",
                            (ThrowingCallable) () -> new ProductRef(21L, "Vacuna", ""),
                            "product code is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidos")
        @DisplayName("rechaza la referencia incompleta al producto")
        void rechaza_referencias_incompletas(String caso, ThrowingCallable construccion,
                String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("conserva id, nombre y codigo")
        void conserva_los_campos() {
            ProductRef ref = new ProductRef(21L, "Vacuna triple", "P-021");

            assertThat(ref.id()).isEqualTo(21L);
            assertThat(ref.name()).isEqualTo("Vacuna triple");
            assertThat(ref.code()).isEqualTo("P-021");
        }
    }
}
