package com.vetsoftware.app.quote.domain;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.modulo;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.precioConIncluidas;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.precioExcluido;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.precioGravado;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.usuarioExtra;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("QuoteLine: la linea congelada y su aritmetica")
class QuoteLineTest {

    @Nested
    @DisplayName("Congelacion")
    class Congelacion {

        @Test
        @DisplayName("copia codigo, nombre y tipo del articulo en vez de referenciarlos")
        void copia_los_datos_del_articulo() {
            QuoteLine linea = QuoteLine.freeze(1, modulo(), precioGravado("100000.00"), 1,
                    BigDecimal.ZERO, AHORA);

            assertThat(linea.getCatalogItemId()).isEqualTo(modulo().id());
            assertThat(linea.getItemCode()).isEqualTo("CLINICAL_HISTORY");
            assertThat(linea.getItemName()).isEqualTo("Historia clinica");
            assertThat(linea.getItemType()).isEqualTo(QuoteItemType.MODULE);
        }

        @Test
        @DisplayName("congela precio unitario, tarifa y tratamiento fiscal de la tarifa")
        void congela_el_precio_y_el_iva() {
            QuoteLine linea = QuoteLine.freeze(1, modulo(), precioGravado("100000.00"), 1,
                    BigDecimal.ZERO, AHORA);

            assertThat(linea.getUnitAmount()).isEqualByComparingTo("100000.00");
            assertThat(linea.getTaxRate()).isEqualByComparingTo("19.00");
            assertThat(linea.getTaxTreatment()).isEqualTo(TaxTreatment.TAXED);
        }
    }

    @Nested
    @DisplayName("Aritmetica")
    class Aritmetica {

        @Test
        @DisplayName("sin descuento el total es base mas IVA")
        void sin_descuento_el_total_es_base_mas_iva() {
            QuoteLine linea = QuoteLine.freeze(1, modulo(), precioGravado("100000.00"), 1,
                    BigDecimal.ZERO, AHORA);

            assertThat(linea.grossAmount()).isEqualByComparingTo("100000.00");
            assertThat(linea.getDiscountAmount()).isEqualByComparingTo("0.00");
            assertThat(linea.getTaxAmount()).isEqualByComparingTo("19000.00");
            assertThat(linea.getLineTotal()).isEqualByComparingTo("119000.00");
        }

        @Test
        @DisplayName("el bruto multiplica el precio unitario por la cantidad")
        void el_bruto_multiplica_por_la_cantidad() {
            QuoteLine linea = QuoteLine.freeze(1, modulo(), precioGravado("12000.00"), 3,
                    BigDecimal.ZERO, AHORA);

            assertThat(linea.grossAmount()).isEqualByComparingTo("36000.00");
            assertThat(linea.getLineTotal()).isEqualByComparingTo("42840.00");
        }

        @Test
        @DisplayName("el IVA se calcula sobre la base YA descontada, no sobre el bruto")
        void el_iva_va_sobre_la_base_descontada() {
            QuoteLine linea = QuoteLine.freeze(1, modulo(), precioGravado("100000.00"), 1,
                    new BigDecimal("10.00"), AHORA);

            assertThat(linea.getDiscountAmount()).isEqualByComparingTo("10000.00");
            assertThat(linea.getTaxAmount()).isEqualByComparingTo("17100.00");
            assertThat(linea.getLineTotal()).isEqualByComparingTo("107100.00");
        }

        @Test
        @DisplayName("guarda el descuento en porcentaje y en pesos, y los dos concuerdan")
        void guarda_el_descuento_en_las_dos_formas() {
            QuoteLine linea = QuoteLine.freeze(1, modulo(), precioGravado("99999.00"), 1,
                    new BigDecimal("7.50"), AHORA);

            assertThat(linea.getDiscountPercent()).isEqualByComparingTo("7.50");
            assertThat(linea.getDiscountAmount()).isEqualByComparingTo("7499.93");
        }

        @Test
        @DisplayName("un descuento del 100 por ciento deja la linea en cero, no en negativo")
        void descuento_total_deja_la_linea_en_cero() {
            QuoteLine linea = QuoteLine.freeze(1, modulo(), precioGravado("100000.00"), 1,
                    new BigDecimal("100.00"), AHORA);

            assertThat(linea.getDiscountAmount()).isEqualByComparingTo("100000.00");
            assertThat(linea.getTaxAmount()).isEqualByComparingTo("0.00");
            assertThat(linea.getLineTotal()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("una linea excluida de IVA no suma impuesto")
        void una_linea_excluida_no_suma_iva() {
            QuoteLine linea = QuoteLine.freeze(1, modulo(), precioExcluido("50000.00"), 2,
                    BigDecimal.ZERO, AHORA);

            assertThat(linea.getTaxAmount()).isEqualByComparingTo("0.00");
            assertThat(linea.getLineTotal()).isEqualByComparingTo("100000.00");
        }

        @Test
        @DisplayName("redondea a centavos con HALF_UP, sin arrastrar decimales")
        void redondea_a_centavos() {
            QuoteLine linea = QuoteLine.freeze(1, modulo(), precioGravado("333.33"), 3,
                    new BigDecimal("3.33"), AHORA);

            assertThat(linea.grossAmount()).isEqualByComparingTo("999.99");
            assertThat(linea.getDiscountAmount()).isEqualByComparingTo("33.30");
            assertThat(linea.getTaxAmount()).isEqualByComparingTo("183.67");
            assertThat(linea.getLineTotal()).isEqualByComparingTo("1150.36");
        }
    }

    @Nested
    @DisplayName("R15: lo incluido se resta antes de fijar la cantidad")
    class UnidadesIncluidas {

        @ParameterizedTest(name = "contratadas {0} - incluidas {1} = cobradas {2}")
        @CsvSource({"3, 2, 1", "1, 2, 0", "2, 2, 0", "10, 0, 10", "15, 3, 12"})
        @DisplayName("la cantidad facturable de una CAPACITY nunca baja de cero")
        void la_cantidad_facturable_nunca_es_negativa(int contratadas, int incluidas,
                int esperadas) {
            int cobradas = QuoteLine.billableQuantity(QuoteItemType.CAPACITY, contratadas,
                    incluidas);

            assertThat(cobradas).isEqualTo(esperadas);
        }

        @ParameterizedTest(name = "tipo {0} no resta lo incluido")
        @CsvSource({"MODULE", "ONE_TIME", "BUNDLE"})
        @DisplayName("a lo que no es capacidad no se le resta nada: restarlo lo borraria")
        void a_lo_que_no_es_capacidad_no_se_le_resta(QuoteItemType tipo) {
            assertThat(QuoteLine.billableQuantity(tipo, 1, 5)).isEqualTo(1);
        }

        @Test
        @DisplayName("cobra solo la unidad que excede a las incluidas por la tarifa")
        void cobra_solo_lo_que_excede_lo_incluido() {
            QuoteLine linea = QuoteLine.freeze(1, usuarioExtra(), precioConIncluidas("12000.00", 2),
                    3, BigDecimal.ZERO, AHORA);

            assertThat(linea.getContractedQuantity()).isEqualTo(3);
            assertThat(linea.getIncludedQuantity()).isEqualTo(2);
            assertThat(linea.getQuantity()).isEqualTo(1);
            assertThat(linea.grossAmount()).isEqualByComparingTo("12000.00");
        }

        @Test
        @DisplayName("congela las unidades incluidas: no se releen de la tarifa despues")
        void congela_las_unidades_incluidas() {
            QuoteLine linea = QuoteLine.freeze(1, usuarioExtra(), precioConIncluidas("12000.00", 2),
                    5, BigDecimal.ZERO, AHORA);

            assertThat(linea.getIncludedQuantity()).isEqualTo(2);
            assertThat(linea.getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("si lo contratado no supera lo incluido no hay linea que emitir")
        void sin_nada_que_cobrar_no_se_puede_construir_la_linea() {
            assertThatThrownBy(() -> QuoteLine.freeze(1, usuarioExtra(),
                    precioConIncluidas("12000.00", 2), 1, BigDecimal.ZERO, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be positive");
        }
    }

    @Nested
    @DisplayName("Verificacion al leer")
    class VerificacionAlLeer {

        @Test
        @DisplayName("un total de linea manipulado se delata al reconstruirla")
        void un_total_manipulado_se_delata() {
            assertThatThrownBy(() -> new QuoteLine(9L, 1, 1L, "CODE", "Nombre",
                    QuoteItemType.MODULE, 1, 0, 1, new BigDecimal("100000.00"), BigDecimal.ZERO,
                    new BigDecimal("0.00"), new BigDecimal("19.00"), TaxTreatment.TAXED,
                    new BigDecimal("19000.00"), new BigDecimal("1.00"), AHORA, true))
                    .isInstanceOf(QuoteLineArithmeticException.class)
                    .hasMessageContaining("lineTotal");
        }

        @Test
        @DisplayName("un IVA que no sale de la base se delata al reconstruirla")
        void un_iva_manipulado_se_delata() {
            assertThatThrownBy(() -> new QuoteLine(9L, 1, 1L, "CODE", "Nombre",
                    QuoteItemType.MODULE, 1, 0, 1, new BigDecimal("100000.00"), BigDecimal.ZERO,
                    new BigDecimal("0.00"), new BigDecimal("19.00"), TaxTreatment.TAXED,
                    new BigDecimal("1.00"), new BigDecimal("100001.00"), AHORA, true))
                    .isInstanceOf(QuoteLineArithmeticException.class)
                    .hasMessageContaining("taxAmount");
        }

        @Test
        @DisplayName("una cantidad cobrada que no cuadra con la resta de R15 se delata")
        void una_cantidad_manipulada_se_delata() {
            assertThatThrownBy(() -> new QuoteLine(9L, 1, 2L, "EXTRA_USER", "Usuario",
                    QuoteItemType.CAPACITY, 3, 2, 3, new BigDecimal("12000.00"), BigDecimal.ZERO,
                    new BigDecimal("0.00"), new BigDecimal("19.00"), TaxTreatment.TAXED,
                    new BigDecimal("6840.00"), new BigDecimal("42840.00"), AHORA, true))
                    .isInstanceOf(QuoteLineArithmeticException.class)
                    .hasMessageContaining("quantity");
        }

        @Test
        @DisplayName("un descuento que no coincide con su porcentaje se delata")
        void un_descuento_incoherente_se_delata() {
            assertThatThrownBy(
                    () -> new QuoteLine(9L, 1, 1L, "CODE", "Nombre", QuoteItemType.MODULE, 1, 0, 1,
                            new BigDecimal("100000.00"), new BigDecimal("10.00"),
                            new BigDecimal("5000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED,
                            new BigDecimal("18050.00"), new BigDecimal("113050.00"), AHORA, true))
                    .isInstanceOf(QuoteLineArithmeticException.class)
                    .hasMessageContaining("discountAmount");
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una linea gravada exige tarifa positiva")
        void gravada_exige_tarifa_positiva() {
            assertThatThrownBy(() -> new QuoteLine(null, 1, 1L, "CODE", "Nombre",
                    QuoteItemType.MODULE, 1, 0, 1, new BigDecimal("100.00"), BigDecimal.ZERO,
                    new BigDecimal("0.00"), BigDecimal.ZERO, TaxTreatment.TAXED,
                    new BigDecimal("0.00"), new BigDecimal("100.00"), AHORA, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TAXED line requires a positive taxRate");
        }

        @Test
        @DisplayName("una linea excluida con tarifa 19 se rechaza: es IVA sobre base que no lo lleva")
        void excluida_con_tarifa_se_rechaza() {
            assertThatThrownBy(() -> new QuoteLine(null, 1, 1L, "CODE", "Nombre",
                    QuoteItemType.MODULE, 1, 0, 1, new BigDecimal("100.00"), BigDecimal.ZERO,
                    new BigDecimal("0.00"), new BigDecimal("19.00"), TaxTreatment.EXCLUDED,
                    new BigDecimal("0.00"), new BigDecimal("100.00"), AHORA, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non TAXED line requires taxRate = 0");
        }

        @Test
        @DisplayName("el numero de linea tiene que ser positivo: es el orden de impresion")
        void el_numero_de_linea_es_positivo() {
            assertThatThrownBy(() -> QuoteLine.freeze(0, modulo(), precioGravado("100.00"), 1,
                    BigDecimal.ZERO, AHORA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lineNumber must be positive");
        }

        @Test
        @DisplayName("sin articulo no hay nada que congelar")
        void sin_articulo_no_hay_linea() {
            assertThatThrownBy(() -> QuoteLine.freeze(1, null, precioGravado("100.00"), 1,
                    BigDecimal.ZERO, AHORA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("catalog item is required");
        }

        @Test
        @DisplayName("sin precio no hay nada que congelar")
        void sin_precio_no_hay_linea() {
            assertThatThrownBy(() -> QuoteLine.freeze(1, modulo(), null, 1, BigDecimal.ZERO, AHORA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("catalog price is required");
        }
    }
}
