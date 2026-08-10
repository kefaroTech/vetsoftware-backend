package com.vetsoftware.app.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.product.testsupport.ProductMother;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Invariantes de la ficha de catalogo {@link Product}: obligatorios, topes de
 * longitud exactos, precio no negativo y la matriz de tratamiento fiscal
 * (GRAVADO/INC exigen impuesto; EXENTO/EXCLUIDO lo prohiben).
 */
@DisplayName("Product — ficha de catalogo")
class ProductTest {

    private static final CompanyRef CLINICA = ProductMother.CLINICA;
    private static final ProductCategoryRef CATEGORIA = ProductMother.CATEGORIA;
    private static final TaxRef IVA = ProductMother.IVA_19;

    /** Constructor completo con los campos validables como parametros. */
    private static Product producto(String name, String code, BigDecimal salePrice, String unidad,
            String provider, String notes, ProductCategoryRef categoria, CompanyRef company) {
        return new Product(1L, name, code, salePrice, unidad, provider, null, TaxTreatment.EXCLUIDO,
                notes, categoria, null, company, ProductMother.CREADO, null, null, 0L, true);
    }

    private static Product valido() {
        return producto("Concentrado", "P-001", new BigDecimal("100.00"), "94", null, null,
                CATEGORIA, CLINICA);
    }

    static Stream<Arguments> camposInvalidos() {
        BigDecimal precio = new BigDecimal("100.00");
        return Stream.of(
                Arguments.of("nombre nulo", null, "P-001", precio, "94", null, null, CATEGORIA,
                        CLINICA, "name is required"),
                Arguments.of("nombre en blanco", "   ", "P-001", precio, "94", null, null,
                        CATEGORIA, CLINICA, "name is required"),
                Arguments.of("nombre de 101 chars", "n".repeat(101), "P-001", precio, "94", null,
                        null, CATEGORIA, CLINICA, "name must be 100 chars or less"),
                Arguments.of("codigo nulo", "Concentrado", null, precio, "94", null, null,
                        CATEGORIA, CLINICA, "code is required"),
                Arguments.of("codigo en blanco", "Concentrado", " ", precio, "94", null, null,
                        CATEGORIA, CLINICA, "code is required"),
                Arguments.of("codigo de 51 chars", "Concentrado", "c".repeat(51), precio, "94",
                        null, null, CATEGORIA, CLINICA, "code must be 50 chars or less"),
                Arguments.of("precio nulo", "Concentrado", "P-001", null, "94", null, null,
                        CATEGORIA, CLINICA, "salePrice is required"),
                Arguments.of("precio negativo", "Concentrado", "P-001", new BigDecimal("-0.01"),
                        "94", null, null, CATEGORIA, CLINICA, "salePrice cannot be negative"),
                Arguments.of("unidad base nula", "Concentrado", "P-001", precio, null, null, null,
                        CATEGORIA, CLINICA, "baseUnitMeasureCode is required"),
                Arguments.of("unidad base en blanco", "Concentrado", "P-001", precio, "  ", null,
                        null, CATEGORIA, CLINICA, "baseUnitMeasureCode is required"),
                Arguments.of("unidad base de 11 chars", "Concentrado", "P-001", precio,
                        "u".repeat(11), null, null, CATEGORIA, CLINICA,
                        "baseUnitMeasureCode must be 10 chars or less"),
                Arguments.of("proveedor de 151 chars", "Concentrado", "P-001", precio, "94",
                        "p".repeat(151), null, CATEGORIA, CLINICA,
                        "provider must be 150 chars or less"),
                Arguments.of("notas de 501 chars", "Concentrado", "P-001", precio, "94", null,
                        "o".repeat(501), CATEGORIA, CLINICA, "notes must be 500 chars or less"),
                Arguments.of("categoria nula", "Concentrado", "P-001", precio, "94", null, null,
                        null, CLINICA, "productCategory is required"),
                Arguments.of("empresa nula", "Concentrado", "P-001", precio, "94", null, null,
                        CATEGORIA, null, "company is required"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("camposInvalidos")
    @DisplayName("rechaza los datos que violan una invariante")
    void rechaza_los_datos_invalidos(String caso, String name, String code, BigDecimal salePrice,
            String unidad, String provider, String notes, ProductCategoryRef categoria,
            CompanyRef company, String mensaje) {
        assertThatThrownBy(
                () -> producto(name, code, salePrice, unidad, provider, notes, categoria, company))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(mensaje);
    }

    @Nested
    @DisplayName("limites exactos aceptados")
    class LimitesExactos {

        @Test
        @DisplayName("nombre de exactamente 100 chars es valido")
        void nombre_de_100_chars_es_valido() {
            Product product = producto("n".repeat(100), "P-001", BigDecimal.ONE, "94", null, null,
                    CATEGORIA, CLINICA);

            assertThat(product.getName()).hasSize(100);
        }

        @Test
        @DisplayName("codigo de exactamente 50 chars es valido")
        void codigo_de_50_chars_es_valido() {
            Product product = producto("Concentrado", "c".repeat(50), BigDecimal.ONE, "94", null,
                    null, CATEGORIA, CLINICA);

            assertThat(product.getCode()).hasSize(50);
        }

        @Test
        @DisplayName("unidad base de exactamente 10 chars es valida")
        void unidad_de_10_chars_es_valida() {
            Product product = producto("Concentrado", "P-001", BigDecimal.ONE, "u".repeat(10), null,
                    null, CATEGORIA, CLINICA);

            assertThat(product.getBaseUnitMeasureCode()).hasSize(10);
        }

        @Test
        @DisplayName("proveedor de exactamente 150 chars y notas de 500 son validos")
        void proveedor_y_notas_en_el_tope_son_validos() {
            Product product = producto("Concentrado", "P-001", BigDecimal.ONE, "94",
                    "p".repeat(150), "o".repeat(500), CATEGORIA, CLINICA);

            assertThat(product.getProvider()).hasSize(150);
            assertThat(product.getNotes()).hasSize(500);
        }

        @Test
        @DisplayName("precio cero es valido: hay productos de obsequio")
        void precio_cero_es_valido() {
            Product product = producto("Muestra", "P-000", BigDecimal.ZERO, "94", null, null,
                    CATEGORIA, CLINICA);

            assertThat(product.getSalePrice()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("matriz de tratamiento fiscal")
    class TratamientoFiscal {

        private Product conTratamiento(TaxTreatment treatment, TaxRef tax) {
            return new Product(1L, "Concentrado", "P-001", new BigDecimal("100.00"), "94", null,
                    null, treatment, null, CATEGORIA, tax, CLINICA, ProductMother.CREADO, null,
                    null, 0L, true);
        }

        @Test
        @DisplayName("el tratamiento fiscal es obligatorio")
        void el_tratamiento_fiscal_es_obligatorio() {
            assertThatThrownBy(() -> conTratamiento(null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxTreatment is required");
        }

        @ParameterizedTest
        @EnumSource(value = TaxTreatment.class, names = {"GRAVADO", "INC"})
        @DisplayName("GRAVADO e INC exigen impuesto")
        void gravado_e_inc_exigen_impuesto(TaxTreatment treatment) {
            assertThatThrownBy(() -> conTratamiento(treatment, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxTreatment " + treatment + " requires a tax");
        }

        @ParameterizedTest
        @EnumSource(value = TaxTreatment.class, names = {"GRAVADO", "INC"})
        @DisplayName("GRAVADO e INC exigen un porcentaje mayor que cero")
        void gravado_e_inc_exigen_porcentaje_positivo(TaxTreatment treatment) {
            TaxRef exentoDeFacto = new TaxRef(4L, "IVA 0%", BigDecimal.ZERO);

            assertThatThrownBy(() -> conTratamiento(treatment, exentoDeFacto))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "taxTreatment " + treatment + " requires a tax percentage greater");
        }

        @ParameterizedTest
        @EnumSource(value = TaxTreatment.class, names = {"GRAVADO", "INC"})
        @DisplayName("GRAVADO e INC con impuesto positivo se aceptan")
        void gravado_e_inc_con_impuesto_positivo_se_aceptan(TaxTreatment treatment) {
            Product product = conTratamiento(treatment, IVA);

            assertThat(product.getTaxTreatment()).isEqualTo(treatment);
            assertThat(product.getTax()).isEqualTo(IVA);
        }

        @ParameterizedTest
        @EnumSource(value = TaxTreatment.class, names = {"EXENTO", "EXCLUIDO"})
        @DisplayName("EXENTO y EXCLUIDO prohiben el impuesto")
        void exento_y_excluido_prohiben_el_impuesto(TaxTreatment treatment) {
            assertThatThrownBy(() -> conTratamiento(treatment, IVA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taxTreatment " + treatment + " must not have a tax");
        }

        @ParameterizedTest
        @EnumSource(value = TaxTreatment.class, names = {"EXENTO", "EXCLUIDO"})
        @DisplayName("EXENTO y EXCLUIDO sin impuesto se aceptan")
        void exento_y_excluido_sin_impuesto_se_aceptan(TaxTreatment treatment) {
            Product product = conTratamiento(treatment, null);

            assertThat(product.getTaxTreatment()).isEqualTo(treatment);
            assertThat(product.getTax()).isNull();
        }
    }

    @Nested
    @DisplayName("factory create")
    class Creacion {

        @Test
        @DisplayName("nace sin id, habilitado, sin version y sin rastro de actualizacion")
        void nace_sin_id_y_habilitado() {
            Product product = Product.create("Concentrado", "P-001", new BigDecimal("100.00"), "94",
                    "Proveedor texto", ProductMother.PROVEEDOR, TaxTreatment.GRAVADO, "notas",
                    CATEGORIA, IVA, CLINICA);

            assertThat(product.getId()).isNull();
            assertThat(product.isEnabled()).isTrue();
            assertThat(product.getVersion()).isNull();
            assertThat(product.getUpdatedDate()).isNull();
            assertThat(product.getUpdatedBy()).isNull();
            assertThat(product.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("conserva todos los campos de catalogo tal como se le pasan")
        void conserva_todos_los_campos() {
            Product product = Product.create("Concentrado", "P-001", new BigDecimal("100.00"), "94",
                    "Proveedor texto", ProductMother.PROVEEDOR, TaxTreatment.GRAVADO, "notas",
                    CATEGORIA, IVA, CLINICA);

            assertThat(product.getName()).isEqualTo("Concentrado");
            assertThat(product.getCode()).isEqualTo("P-001");
            assertThat(product.getSalePrice()).isEqualByComparingTo("100.00");
            assertThat(product.getBaseUnitMeasureCode()).isEqualTo("94");
            assertThat(product.getProvider()).isEqualTo("Proveedor texto");
            assertThat(product.getSupplier()).isEqualTo(ProductMother.PROVEEDOR);
            assertThat(product.getNotes()).isEqualTo("notas");
            assertThat(product.getProductCategory()).isEqualTo(CATEGORIA);
            assertThat(product.getTax()).isEqualTo(IVA);
            assertThat(product.getCompany()).isEqualTo(CLINICA);
        }

        @Test
        @DisplayName("valida antes de construir: un nombre vacio no llega a existir")
        void valida_antes_de_construir() {
            assertThatThrownBy(() -> Product.create("  ", "P-001", new BigDecimal("100.00"), "94",
                    null, null, TaxTreatment.EXCLUIDO, null, CATEGORIA, null, CLINICA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }
    }

    @Nested
    @DisplayName("update")
    class Actualizacion {

        @Test
        @DisplayName("reemplaza los campos de catalogo y sella quien y cuando")
        void reemplaza_los_campos_y_sella_la_actualizacion() {
            Product product = ProductMother.gravado();

            product.update("Concentrado senior", "P-009", new BigDecimal("18000.00"), "KGM",
                    "Otro proveedor", ProductMother.OTRO_PROVEEDOR, TaxTreatment.GRAVADO, "nuevas",
                    ProductMother.OTRA_CATEGORIA, ProductMother.IVA_5, CLINICA, 77L, 3L);

            assertThat(product.getName()).isEqualTo("Concentrado senior");
            assertThat(product.getCode()).isEqualTo("P-009");
            assertThat(product.getSalePrice()).isEqualByComparingTo("18000.00");
            assertThat(product.getBaseUnitMeasureCode()).isEqualTo("KGM");
            assertThat(product.getProvider()).isEqualTo("Otro proveedor");
            assertThat(product.getSupplier()).isEqualTo(ProductMother.OTRO_PROVEEDOR);
            assertThat(product.getNotes()).isEqualTo("nuevas");
            assertThat(product.getProductCategory()).isEqualTo(ProductMother.OTRA_CATEGORIA);
            assertThat(product.getTax()).isEqualTo(ProductMother.IVA_5);
            assertThat(product.getUpdatedBy()).isEqualTo(77L);
            assertThat(product.getVersion()).isEqualTo(3L);
            assertThat(product.getUpdatedDate()).isNotNull();
        }

        @Test
        @DisplayName("no toca el id ni la fecha de creacion")
        void no_toca_el_id_ni_la_fecha_de_creacion() {
            Product product = ProductMother.gravado();
            LocalDateTime creado = product.getCreatedDate();

            product.update("Otro", "P-009", BigDecimal.ONE, "94", null, null, TaxTreatment.EXCLUIDO,
                    null, CATEGORIA, null, CLINICA, 77L, 3L);

            assertThat(product.getId()).isEqualTo(ProductMother.PRODUCT_ID);
            assertThat(product.getCreatedDate()).isEqualTo(creado);
        }

        @Test
        @DisplayName("rechaza el cambio que rompe una invariante y no muta el estado")
        void rechaza_el_cambio_invalido_sin_mutar() {
            Product product = ProductMother.gravado();

            assertThatThrownBy(() -> product.update("", "P-009", BigDecimal.ONE, "94", null, null,
                    TaxTreatment.EXCLUIDO, null, CATEGORIA, null, CLINICA, 77L, 3L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");

            assertThat(product.getName()).isEqualTo("Concentrado adulto");
            assertThat(product.getUpdatedBy()).isNull();
        }

        @Test
        @DisplayName("rechaza pasar a GRAVADO quitando el impuesto")
        void rechaza_gravado_sin_impuesto() {
            Product product = ProductMother.gravado();

            assertThatThrownBy(() -> product.update("Concentrado", "P-001", BigDecimal.ONE, "94",
                    null, null, TaxTreatment.GRAVADO, null, CATEGORIA, null, CLINICA, 77L, 3L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("requires a tax");
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable apaga el producto y enable lo devuelve al catalogo")
        void disable_y_enable_alternan_el_estado() {
            Product product = valido();

            product.disable();
            assertThat(product.isEnabled()).isFalse();

            product.enable();
            assertThat(product.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("enable sobre uno ya habilitado es idempotente")
        void enable_es_idempotente() {
            Product product = valido();

            assertThatCode(product::enable).doesNotThrowAnyException();
            assertThat(product.isEnabled()).isTrue();
        }
    }
}
