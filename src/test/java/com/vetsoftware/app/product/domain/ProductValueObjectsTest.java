package com.vetsoftware.app.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Companion VOs del modulo product. Cada uno defiende sus propias invariantes:
 * si dejan pasar un nulo o un blanco, el agregado se construye con una
 * referencia rota y el fallo aparece mucho mas tarde, en la vista.
 */
@DisplayName("Value objects de product")
class ProductValueObjectsTest {

    @Nested
    @DisplayName("CompanyRef")
    class Company {

        @Test
        @DisplayName("exige id")
        void exige_id() {
            assertThatThrownBy(() -> new CompanyRef(null, "Clinica", "NIT-900"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company id is required");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("exige nombre con contenido")
        void exige_nombre(String nombre) {
            assertThatThrownBy(() -> new CompanyRef(1L, nombre, "NIT-900"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company name is required");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("exige identificador con contenido")
        void exige_identificador(String identificador) {
            assertThatThrownBy(() -> new CompanyRef(1L, "Clinica", identificador))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company identifier is required");
        }

        @Test
        @DisplayName("acepta la terna completa y expone sus componentes")
        void acepta_la_terna_completa() {
            CompanyRef ref = new CompanyRef(9L, "Clinica Norte", "NIT-900");

            assertThat(ref.id()).isEqualTo(9L);
            assertThat(ref.name()).isEqualTo("Clinica Norte");
            assertThat(ref.identifier()).isEqualTo("NIT-900");
        }
    }

    @Nested
    @DisplayName("ProductCategoryRef")
    class Categoria {

        @Test
        @DisplayName("exige id")
        void exige_id() {
            assertThatThrownBy(() -> new ProductCategoryRef(null, "Alimentos"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("productCategory id is required");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "  "})
        @DisplayName("exige nombre con contenido")
        void exige_nombre(String nombre) {
            assertThatThrownBy(() -> new ProductCategoryRef(3L, nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("productCategory name is required");
        }

        @Test
        @DisplayName("acepta id y nombre")
        void acepta_id_y_nombre() {
            ProductCategoryRef ref = new ProductCategoryRef(3L, "Alimentos");

            assertThat(ref.id()).isEqualTo(3L);
            assertThat(ref.name()).isEqualTo("Alimentos");
        }
    }

    @Nested
    @DisplayName("SupplierRef")
    class Proveedor {

        @Test
        @DisplayName("exige id")
        void exige_id() {
            assertThatThrownBy(() -> new SupplierRef(null, "Distribuidora"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("supplier id is required");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", " "})
        @DisplayName("exige nombre con contenido")
        void exige_nombre(String nombre) {
            assertThatThrownBy(() -> new SupplierRef(6L, nombre))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("supplier name is required");
        }

        @Test
        @DisplayName("dos refs con los mismos componentes son iguales")
        void igualdad_por_valor() {
            assertThat(new SupplierRef(6L, "Distribuidora"))
                    .isEqualTo(new SupplierRef(6L, "Distribuidora"));
        }
    }

    @Nested
    @DisplayName("TaxRef")
    class Impuesto {

        @Test
        @DisplayName("exige id")
        void exige_id() {
            assertThatThrownBy(() -> new TaxRef(null, "IVA", BigDecimal.ONE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tax id is required");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "  "})
        @DisplayName("exige nombre con contenido")
        void exige_nombre(String nombre) {
            assertThatThrownBy(() -> new TaxRef(4L, nombre, BigDecimal.ONE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tax name is required");
        }

        @Test
        @DisplayName("exige porcentaje")
        void exige_porcentaje() {
            assertThatThrownBy(() -> new TaxRef(4L, "IVA", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tax percentage is required");
        }

        @Test
        @DisplayName("rechaza un porcentaje negativo")
        void rechaza_porcentaje_negativo() {
            assertThatThrownBy(() -> new TaxRef(4L, "IVA", new BigDecimal("-0.01")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tax percentage cannot be negative");
        }

        @Test
        @DisplayName("acepta porcentaje cero: existe el impuesto tarifa 0")
        void acepta_porcentaje_cero() {
            TaxRef ref = new TaxRef(4L, "IVA 0%", BigDecimal.ZERO);

            assertThat(ref.percentage()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("excepciones de dominio")
    class Excepciones {

        @Test
        @DisplayName("ProductNotFoundException lleva el id en el mensaje")
        void not_found_lleva_el_id() {
            assertThat(new ProductNotFoundException(55L)).hasMessage("Product not found: 55");
        }

        @Test
        @DisplayName("ProductCodeAlreadyExistsException nombra el codigo duplicado")
        void code_duplicado_nombra_el_codigo() {
            assertThat(new ProductCodeAlreadyExistsException("P-001"))
                    .hasMessageContaining("'P-001'")
                    .hasMessageContaining("Ya existe un producto activo");
        }

        @Test
        @DisplayName("ProductNameAlreadyExistsException nombra el nombre duplicado")
        void nombre_duplicado_nombra_el_nombre() {
            assertThat(new ProductNameAlreadyExistsException("Concentrado"))
                    .hasMessageContaining("'Concentrado'")
                    .hasMessageContaining("Ya existe un producto activo");
        }
    }
}
