package com.vetsoftware.app.productchargeopenaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.productchargeopenaccount.testsupport.ProductChargeOpenAccountMother;
import java.math.BigDecimal;
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

@DisplayName("ProductChargeOpenAccount — invariantes, dinero y anulacion")
class ProductChargeOpenAccountTest {

    /** Constructor de fixtures con un campo variable por caso. */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = ProductChargeOpenAccountMother.CHARGE_ID;
        private AnimalRef animal = ProductChargeOpenAccountMother.ANIMAL;
        private ProductRef product = ProductChargeOpenAccountMother.PRODUCTO;
        private BigDecimal unitPrice = new BigDecimal("11900");
        private int quantity = 1;
        private OpenAccountRef openAccount = ProductChargeOpenAccountMother.CUENTA;

        private Builder animal(AnimalRef v) {
            this.animal = v;
            return this;
        }

        private Builder product(ProductRef v) {
            this.product = v;
            return this;
        }

        private Builder unitPrice(BigDecimal v) {
            this.unitPrice = v;
            return this;
        }

        private Builder quantity(int v) {
            this.quantity = v;
            return this;
        }

        private Builder openAccount(OpenAccountRef v) {
            this.openAccount = v;
            return this;
        }

        private ProductChargeOpenAccount build() {
            return new ProductChargeOpenAccount(id, animal, product, unitPrice, quantity,
                    ProductChargeOpenAccountMother.IVA_19, true, new BigDecimal("19.00"), "IVA 19%",
                    "IVA", "GRAVADO", new BigDecimal("10000.00"), new BigDecimal("1900.00"),
                    new BigDecimal("11900.00"), openAccount,
                    ProductChargeOpenAccountMother.EMPLEADO, ProductChargeOpenAccountMother.CREADO,
                    null, true, false, null, null, null, "req-1");
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            ProductChargeOpenAccount charge = valido().build();

            assertThat(charge.getId()).isEqualTo(ProductChargeOpenAccountMother.CHARGE_ID);
            assertThat(charge.getAnimal()).isEqualTo(ProductChargeOpenAccountMother.ANIMAL);
            assertThat(charge.getProduct()).isEqualTo(ProductChargeOpenAccountMother.PRODUCTO);
            assertThat(charge.getUnitPrice()).isEqualByComparingTo("11900");
            assertThat(charge.getQuantity()).isEqualTo(1);
            assertThat(charge.getTax()).isEqualTo(ProductChargeOpenAccountMother.IVA_19);
            assertThat(charge.isHasTax()).isTrue();
            assertThat(charge.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(charge.getTaxName()).isEqualTo("IVA 19%");
            assertThat(charge.getTaxScheme()).isEqualTo("IVA");
            assertThat(charge.getTaxTreatment()).isEqualTo("GRAVADO");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("1900.00");
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("11900.00");
            assertThat(charge.getOpenAccount()).isEqualTo(ProductChargeOpenAccountMother.CUENTA);
            assertThat(charge.getCreatedBy()).isEqualTo(ProductChargeOpenAccountMother.EMPLEADO);
            assertThat(charge.getCreatedDate()).isEqualTo(ProductChargeOpenAccountMother.CREADO);
            assertThat(charge.isEnabled()).isTrue();
            assertThat(charge.isVoided()).isFalse();
            assertThat(charge.getVoidedBy()).isNull();
            assertThat(charge.getVoidedAt()).isNull();
            assertThat(charge.getVoidReason()).isNull();
            assertThat(charge.getClientRequestId()).isEqualTo("req-1");
        }

        @Test
        @DisplayName("el constructor de compat sin impuesto deja base = total y iva en cero")
        void constructor_de_compat_sin_impuesto() {
            ProductChargeOpenAccount charge = new ProductChargeOpenAccount(1L,
                    ProductChargeOpenAccountMother.ANIMAL, ProductChargeOpenAccountMother.PRODUCTO,
                    new BigDecimal("5000.5"), ProductChargeOpenAccountMother.CUENTA,
                    ProductChargeOpenAccountMother.EMPLEADO, ProductChargeOpenAccountMother.CREADO,
                    true, false, null, null, null);

            assertThat(charge.getQuantity()).isEqualTo(1);
            assertThat(charge.isHasTax()).isFalse();
            assertThat(charge.getTax()).isNull();
            assertThat(charge.getTaxPercentage()).isNull();
            assertThat(charge.getTaxName()).isNull();
            assertThat(charge.getTaxScheme()).isNull();
            assertThat(charge.getTaxTreatment()).isNull();
            // Money.scaled redondea a 2 decimales HALF_UP; el unitPrice crudo no se toca.
            assertThat(charge.getUnitPrice()).isEqualByComparingTo("5000.5");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("5000.50");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("0.00");
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("5000.50");
            assertThat(charge.getClientRequestId()).isNull();
        }

        @Test
        @DisplayName("el constructor de compat mas corto nace activo y sin anular")
        void constructor_de_compat_mas_corto() {
            ProductChargeOpenAccount charge = new ProductChargeOpenAccount(1L,
                    ProductChargeOpenAccountMother.ANIMAL, ProductChargeOpenAccountMother.PRODUCTO,
                    new BigDecimal("100"), ProductChargeOpenAccountMother.CUENTA,
                    ProductChargeOpenAccountMother.EMPLEADO, ProductChargeOpenAccountMother.CREADO,
                    true);

            assertThat(charge.isEnabled()).isTrue();
            assertThat(charge.isVoided()).isFalse();
            assertThat(charge.getVoidedBy()).isNull();
            assertThat(charge.getVoidReason()).isNull();
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(
                    arguments("animal null", (ThrowingCallable) () -> valido().animal(null).build(),
                            "animal is required"),
                    arguments("product null",
                            (ThrowingCallable) () -> valido().product(null).build(),
                            "product is required"),
                    arguments("openAccount null",
                            (ThrowingCallable) () -> valido().openAccount(null).build(),
                            "openAccount is required"),
                    arguments("unitPrice null",
                            (ThrowingCallable) () -> valido().unitPrice(null).build(),
                            "unitPrice is required"),
                    arguments("unitPrice negativo",
                            (ThrowingCallable) () -> valido().unitPrice(new BigDecimal("-0.01"))
                                    .build(),
                            "unitPrice cannot be negative"),
                    arguments("quantity cero",
                            (ThrowingCallable) () -> valido().quantity(0).build(),
                            "quantity must be at least 1"),
                    arguments("quantity negativa",
                            (ThrowingCallable) () -> valido().quantity(-3).build(),
                            "quantity must be at least 1"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable construccion, String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("unitPrice cero es valido: un cargo gratis no es un error de dominio")
        void unit_price_cero_es_valido() {
            assertThatCode(() -> valido().unitPrice(BigDecimal.ZERO).build())
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "quantity {0}")
        @ValueSource(ints = {1, 2, 1000})
        @DisplayName("quantity desde el limite exacto se acepta")
        void quantity_desde_el_limite_exacto_se_acepta(int cantidad) {
            assertThatCode(() -> valido().quantity(cantidad).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("create — congela precio e impuesto")
    class Create {

        @Test
        @DisplayName("nace sin id, activo, sin anular y con la fecha del momento")
        void nace_sin_id_activo_y_sin_anular() {
            ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(
                    ProductChargeOpenAccountMother.ANIMAL, ProductChargeOpenAccountMother.PRODUCTO,
                    1, ProductChargeOpenAccountMother.CUENTA,
                    ProductChargeOpenAccountMother.EMPLEADO, "req-9");

            assertThat(charge.getId()).isNull();
            assertThat(charge.isEnabled()).isTrue();
            assertThat(charge.isVoided()).isFalse();
            assertThat(charge.getVoidedBy()).isNull();
            assertThat(charge.getVoidedAt()).isNull();
            assertThat(charge.getVoidReason()).isNull();
            assertThat(charge.getClientRequestId()).isEqualTo("req-9");
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana.
            assertThat(charge.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("congela el precio de venta del catalogo como unitPrice")
        void congela_el_precio_de_venta_del_catalogo() {
            ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(
                    ProductChargeOpenAccountMother.ANIMAL, ProductChargeOpenAccountMother.PRODUCTO,
                    1, ProductChargeOpenAccountMother.CUENTA,
                    ProductChargeOpenAccountMother.EMPLEADO, null);

            assertThat(charge.getUnitPrice()).isEqualByComparingTo("11900");
        }

        @Test
        @DisplayName("desglosa el IVA incluido: total = unitPrice x quantity, base = total / 1,19")
        void desglosa_el_iva_incluido() {
            ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(
                    ProductChargeOpenAccountMother.ANIMAL, ProductChargeOpenAccountMother.PRODUCTO,
                    2, ProductChargeOpenAccountMother.CUENTA,
                    ProductChargeOpenAccountMother.EMPLEADO, null);

            assertThat(charge.getTotalAmount()).isEqualByComparingTo("23800.00");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("20000.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("3800.00");
            // base + iva tiene que reconstruir el total al centavo: es la invariante que
            // sostiene el documento electronico del cierre.
            assertThat(charge.getBaseAmount().add(charge.getTaxAmount()))
                    .isEqualByComparingTo(charge.getTotalAmount());
        }

        @Test
        @DisplayName("congela nombre, porcentaje, esquema y tratamiento del impuesto")
        void congela_los_datos_del_impuesto() {
            ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(
                    ProductChargeOpenAccountMother.ANIMAL, ProductChargeOpenAccountMother.PRODUCTO,
                    1, ProductChargeOpenAccountMother.CUENTA,
                    ProductChargeOpenAccountMother.EMPLEADO, null);

            assertThat(charge.isHasTax()).isTrue();
            assertThat(charge.getTax()).isEqualTo(ProductChargeOpenAccountMother.IVA_19);
            assertThat(charge.getTaxPercentage()).isEqualByComparingTo("19.00");
            assertThat(charge.getTaxName()).isEqualTo("IVA 19%");
            assertThat(charge.getTaxScheme()).isEqualTo("IVA");
            assertThat(charge.getTaxTreatment()).isEqualTo("GRAVADO");
        }

        @Test
        @DisplayName("producto sin impuesto: base = total y el impuesto queda en cero")
        void producto_sin_impuesto() {
            ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(
                    ProductChargeOpenAccountMother.ANIMAL,
                    ProductChargeOpenAccountMother.PRODUCTO_SIN_IMPUESTO, 3,
                    ProductChargeOpenAccountMother.CUENTA, ProductChargeOpenAccountMother.EMPLEADO,
                    null);

            assertThat(charge.isHasTax()).isFalse();
            assertThat(charge.getTax()).isNull();
            assertThat(charge.getTaxPercentage()).isNull();
            assertThat(charge.getTaxName()).isNull();
            assertThat(charge.getTaxScheme()).isNull();
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("15000.00");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("15000.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("producto marcado con impuesto pero sin TaxRef no aplica impuesto")
        void producto_marcado_con_impuesto_pero_sin_tax_ref() {
            ProductRef incoherente = new ProductRef(2L, "Alimento", "P-001",
                    new BigDecimal("11900"), true, null, "GRAVADO");

            ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(
                    ProductChargeOpenAccountMother.ANIMAL, incoherente, 1,
                    ProductChargeOpenAccountMother.CUENTA, ProductChargeOpenAccountMother.EMPLEADO,
                    null);

            assertThat(charge.isHasTax()).isFalse();
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("11900.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("producto con TaxRef pero hasTax en false no aplica impuesto")
        void producto_con_tax_ref_pero_has_tax_en_false() {
            ProductRef sinAplicar = new ProductRef(2L, "Alimento", "P-001", new BigDecimal("11900"),
                    false, ProductChargeOpenAccountMother.IVA_19, "EXCLUIDO");

            ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(
                    ProductChargeOpenAccountMother.ANIMAL, sinAplicar, 1,
                    ProductChargeOpenAccountMother.CUENTA, ProductChargeOpenAccountMother.EMPLEADO,
                    null);

            assertThat(charge.isHasTax()).isFalse();
            assertThat(charge.getTax()).isNull();
            // El tratamiento se congela igual: distingue EXCLUIDO de EXENTO en el cierre.
            assertThat(charge.getTaxTreatment()).isEqualTo("EXCLUIDO");
        }

        @Test
        @DisplayName("impuesto al 0 % (exento) no mueve dinero pero deja el rastro tributario")
        void impuesto_al_cero_por_ciento() {
            ProductRef exento = new ProductRef(2L, "Leche", "P-004", new BigDecimal("10000"), true,
                    ProductChargeOpenAccountMother.IVA_0, "EXENTO");

            ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(
                    ProductChargeOpenAccountMother.ANIMAL, exento, 1,
                    ProductChargeOpenAccountMother.CUENTA, ProductChargeOpenAccountMother.EMPLEADO,
                    null);

            assertThat(charge.isHasTax()).isTrue();
            assertThat(charge.getTaxPercentage()).isEqualByComparingTo("0");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("10000.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("0.00");
            assertThat(charge.getTaxTreatment()).isEqualTo("EXENTO");
        }

        @Test
        @DisplayName("producto sin precio se cobra en cero, no revienta")
        void producto_sin_precio_se_cobra_en_cero() {
            ProductRef sinPrecio = new ProductRef(2L, "Muestra", "P-005", null);

            ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(
                    ProductChargeOpenAccountMother.ANIMAL, sinPrecio, 4,
                    ProductChargeOpenAccountMother.CUENTA, ProductChargeOpenAccountMother.EMPLEADO,
                    null);

            assertThat(charge.getUnitPrice()).isEqualByComparingTo("0");
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("0.00");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("0.00");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("el total se redondea a 2 decimales con HALF_UP")
        void el_total_se_redondea_half_up() {
            ProductRef conFraccion = new ProductRef(2L, "Granel", "P-006", new BigDecimal("0.125"));

            ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(
                    ProductChargeOpenAccountMother.ANIMAL, conFraccion, 1,
                    ProductChargeOpenAccountMother.CUENTA, ProductChargeOpenAccountMother.EMPLEADO,
                    null);

            // 0,125 -> HALF_UP -> 0,13 (HALF_EVEN daria 0,12).
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("0.13");
            assertThat(charge.getUnitPrice()).isEqualByComparingTo("0.125");
        }

        @Test
        @DisplayName("la base se redondea a 2 decimales y el impuesto absorbe el resto")
        void la_base_se_redondea_y_el_impuesto_absorbe_el_resto() {
            ProductRef producto = new ProductRef(2L, "Alimento", "P-001", new BigDecimal("1000"),
                    true, ProductChargeOpenAccountMother.IVA_19, "GRAVADO");

            ProductChargeOpenAccount charge = ProductChargeOpenAccount.create(
                    ProductChargeOpenAccountMother.ANIMAL, producto, 3,
                    ProductChargeOpenAccountMother.CUENTA, ProductChargeOpenAccountMother.EMPLEADO,
                    null);

            // 3000 / 1,19 = 2521,008403... -> 2521,01; el iva se calcula por diferencia.
            assertThat(charge.getTotalAmount()).isEqualByComparingTo("3000.00");
            assertThat(charge.getBaseAmount()).isEqualByComparingTo("2521.01");
            assertThat(charge.getTaxAmount()).isEqualByComparingTo("478.99");
        }

        @Test
        @DisplayName("cantidad cero no llega a crear el cargo")
        void cantidad_cero_no_llega_a_crear_el_cargo() {
            assertThatThrownBy(() -> ProductChargeOpenAccount.create(
                    ProductChargeOpenAccountMother.ANIMAL, ProductChargeOpenAccountMother.PRODUCTO,
                    0, ProductChargeOpenAccountMother.CUENTA,
                    ProductChargeOpenAccountMother.EMPLEADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be at least 1");
        }

        @Test
        @DisplayName("sin producto no hay cargo")
        void sin_producto_no_hay_cargo() {
            assertThatThrownBy(
                    () -> ProductChargeOpenAccount.create(ProductChargeOpenAccountMother.ANIMAL,
                            null, 1, ProductChargeOpenAccountMother.CUENTA,
                            ProductChargeOpenAccountMother.EMPLEADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("product is required");
        }
    }

    @Nested
    @DisplayName("anulacion")
    class Anulacion {

        @Test
        @DisplayName("registra quien anula, cuando y por que, sin deshabilitar la fila")
        void registra_quien_anula_cuando_y_por_que() {
            ProductChargeOpenAccount charge = valido().build();

            charge.voidCharge(ProductChargeOpenAccountMother.OTRO_EMPLEADO, "Cobrado por error");

            assertThat(charge.isVoided()).isTrue();
            assertThat(charge.getVoidedBy())
                    .isEqualTo(ProductChargeOpenAccountMother.OTRO_EMPLEADO);
            assertThat(charge.getVoidReason()).isEqualTo("Cobrado por error");
            assertThat(charge.getVoidedAt()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
            // La fila sigue visible: la suma de la cuenta la excluye por voided, no por
            // enabled.
            assertThat(charge.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("no toca el importe congelado del cargo")
        void no_toca_el_importe_congelado() {
            ProductChargeOpenAccount charge = valido().build();

            charge.voidCharge(ProductChargeOpenAccountMother.OTRO_EMPLEADO, "Cobrado por error");

            assertThat(charge.getTotalAmount()).isEqualByComparingTo("11900.00");
        }

        @Test
        @DisplayName("un cargo ya anulado no puede volver a anularse")
        void un_cargo_ya_anulado_no_puede_volver_a_anularse() {
            ProductChargeOpenAccount charge = ProductChargeOpenAccountMother.cargoAnulado();

            assertThatThrownBy(
                    () -> charge.voidCharge(ProductChargeOpenAccountMother.EMPLEADO, "otra vez"))
                    .isInstanceOf(ProductChargeOpenAccountAlreadyVoidedException.class)
                    .hasMessageContaining(
                            "already voided: " + ProductChargeOpenAccountMother.CHARGE_ID);
        }

        @Test
        @DisplayName("sin empleado que anule no hay anulacion")
        void sin_empleado_que_anule_no_hay_anulacion() {
            ProductChargeOpenAccount charge = valido().build();

            assertThatThrownBy(() -> charge.voidCharge(null, "motivo"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("voidedBy is required");

            assertThat(charge.isVoided()).isFalse();
        }

        @ParameterizedTest(name = "motivo [{0}]")
        @ValueSource(strings = {"", "   "})
        @DisplayName("el motivo en blanco se rechaza")
        void el_motivo_en_blanco_se_rechaza(String motivo) {
            ProductChargeOpenAccount charge = valido().build();

            assertThatThrownBy(
                    () -> charge.voidCharge(ProductChargeOpenAccountMother.EMPLEADO, motivo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required to void");
        }

        @Test
        @DisplayName("el motivo null se rechaza")
        void el_motivo_null_se_rechaza() {
            ProductChargeOpenAccount charge = valido().build();

            assertThatThrownBy(
                    () -> charge.voidCharge(ProductChargeOpenAccountMother.EMPLEADO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reason is required to void");

            assertThat(charge.isVoided()).isFalse();
        }
    }
}
