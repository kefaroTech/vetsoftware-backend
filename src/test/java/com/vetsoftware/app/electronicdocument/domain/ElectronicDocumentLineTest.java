package com.vetsoftware.app.electronicdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Linea del documento: la foto congelada de un cargo. Cada campo que la DIAN
 * exige en {@code InvoiceLine} es una invariante del constructor, porque una
 * linea a medio poblar solo se detecta al recibir el rechazo fiscal.
 */
@DisplayName("ElectronicDocumentLine — invariantes de la linea fiscal")
class ElectronicDocumentLineTest {

    private static final BigDecimal MIL = new BigDecimal("1000.00");
    private static final BigDecimal CIENTO_NOVENTA = new BigDecimal("190.00");
    private static final BigDecimal MIL_CIENTO_NOVENTA = new BigDecimal("1190.00");

    private static ElectronicDocumentLine linea(String description, BigDecimal quantity,
            String unitMeasureCode, BigDecimal unitPrice, BigDecimal lineExtensionAmount,
            TaxCategory taxCategory, BigDecimal taxAmount, BigDecimal totalAmount) {
        return new ElectronicDocumentLine(null, 1, description, quantity, unitMeasureCode,
                unitPrice, lineExtensionAmount, taxCategory, TaxScheme.IVA, new BigDecimal("19"),
                taxAmount, totalAmount);
    }

    private static ElectronicDocumentLine lineaValida() {
        return linea("Vacuna antirrabica", BigDecimal.ONE, "94", MIL, MIL, TaxCategory.GRAVADO,
                CIENTO_NOVENTA, MIL_CIENTO_NOVENTA);
    }

    @Nested
    @DisplayName("campos obligatorios")
    class CamposObligatorios {

        static Stream<Arguments> camposAusentes() {
            return Stream.of(
                    Arguments.of("descripcion null",
                            (ThrowingCallable) () -> linea(null, BigDecimal.ONE, "94", MIL, MIL,
                                    TaxCategory.GRAVADO, CIENTO_NOVENTA, MIL_CIENTO_NOVENTA),
                            "line description is required"),
                    Arguments.of("cantidad null",
                            (ThrowingCallable) () -> linea("Vacuna", null, "94", MIL, MIL,
                                    TaxCategory.GRAVADO, CIENTO_NOVENTA, MIL_CIENTO_NOVENTA),
                            "line quantity is required"),
                    Arguments.of("unidad de medida null",
                            (ThrowingCallable) () -> linea("Vacuna", BigDecimal.ONE, null, MIL, MIL,
                                    TaxCategory.GRAVADO, CIENTO_NOVENTA, MIL_CIENTO_NOVENTA),
                            "line unitMeasureCode is required"),
                    Arguments.of("precio unitario null",
                            (ThrowingCallable) () -> linea("Vacuna", BigDecimal.ONE, "94", null,
                                    MIL, TaxCategory.GRAVADO, CIENTO_NOVENTA, MIL_CIENTO_NOVENTA),
                            "line unitPrice is required"),
                    Arguments.of("base gravable null",
                            (ThrowingCallable) () -> linea("Vacuna", BigDecimal.ONE, "94", MIL,
                                    null, TaxCategory.GRAVADO, CIENTO_NOVENTA, MIL_CIENTO_NOVENTA),
                            "line lineExtensionAmount is required"),
                    Arguments.of("categoria tributaria null",
                            (ThrowingCallable) () -> linea("Vacuna", BigDecimal.ONE, "94", MIL, MIL,
                                    null, CIENTO_NOVENTA, MIL_CIENTO_NOVENTA),
                            "line taxCategory is required"),
                    Arguments.of("impuesto null",
                            (ThrowingCallable) () -> linea("Vacuna", BigDecimal.ONE, "94", MIL, MIL,
                                    TaxCategory.GRAVADO, null, MIL_CIENTO_NOVENTA),
                            "line taxAmount is required"),
                    Arguments.of("total null",
                            (ThrowingCallable) () -> linea("Vacuna", BigDecimal.ONE, "94", MIL, MIL,
                                    TaxCategory.GRAVADO, CIENTO_NOVENTA, null),
                            "line totalAmount is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("camposAusentes")
        @DisplayName("rechaza la linea cuando falta un campo obligatorio")
        void rechaza_la_linea_cuando_falta_un_campo(String caso, ThrowingCallable construccion,
                String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        @DisplayName("rechaza una descripcion en blanco")
        void rechaza_una_descripcion_en_blanco(String description) {
            assertThatThrownBy(() -> linea(description, BigDecimal.ONE, "94", MIL, MIL,
                    TaxCategory.GRAVADO, CIENTO_NOVENTA, MIL_CIENTO_NOVENTA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("line description is required");
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "\t"})
        @DisplayName("rechaza una unidad de medida en blanco")
        void rechaza_una_unidad_de_medida_en_blanco(String unitMeasureCode) {
            assertThatThrownBy(() -> linea("Vacuna", BigDecimal.ONE, unitMeasureCode, MIL, MIL,
                    TaxCategory.GRAVADO, CIENTO_NOVENTA, MIL_CIENTO_NOVENTA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("line unitMeasureCode is required");
        }
    }

    @Nested
    @DisplayName("limite de la descripcion")
    class LimiteDescripcion {

        @Test
        @DisplayName("acepta exactamente 255 caracteres")
        void acepta_exactamente_255_caracteres() {
            String limite = "x".repeat(255);

            assertThatCode(() -> linea(limite, BigDecimal.ONE, "94", MIL, MIL, TaxCategory.GRAVADO,
                    CIENTO_NOVENTA, MIL_CIENTO_NOVENTA)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rechaza 256 caracteres: la columna no da mas")
        void rechaza_256_caracteres() {
            String excedido = "x".repeat(256);

            assertThatThrownBy(() -> linea(excedido, BigDecimal.ONE, "94", MIL, MIL,
                    TaxCategory.GRAVADO, CIENTO_NOVENTA, MIL_CIENTO_NOVENTA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("line description must be 255 chars or less");
        }
    }

    @Nested
    @DisplayName("campos opcionales por clasificacion tributaria")
    class CamposOpcionales {

        @Test
        @DisplayName("una linea EXCLUIDA viaja sin esquema ni tarifa")
        void una_linea_excluida_viaja_sin_esquema_ni_tarifa() {
            ElectronicDocumentLine excluida = new ElectronicDocumentLine(null, 1, "Excluido",
                    BigDecimal.ONE, "94", MIL, MIL, TaxCategory.EXCLUIDO, null, null,
                    BigDecimal.ZERO, MIL);

            assertThat(excluida.getTaxScheme()).isNull();
            assertThat(excluida.getTaxRate()).isNull();
            assertThat(excluida.getTaxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("una linea EXENTA conserva esquema IVA a tarifa cero")
        void una_linea_exenta_conserva_esquema_iva_a_tarifa_cero() {
            ElectronicDocumentLine exenta = new ElectronicDocumentLine(null, 1, "Exento",
                    BigDecimal.ONE, "94", MIL, MIL, TaxCategory.EXENTO, TaxScheme.IVA,
                    BigDecimal.ZERO, BigDecimal.ZERO, MIL);

            assertThat(exenta.getTaxScheme()).isEqualTo(TaxScheme.IVA);
            assertThat(exenta.getTaxRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("acepta cantidades fraccionarias: no toda linea se vende por unidades")
        void acepta_cantidades_fraccionarias() {
            ElectronicDocumentLine linea = linea("Alimento a granel", new BigDecimal("2.500"),
                    "KGM", MIL, MIL, TaxCategory.GRAVADO, CIENTO_NOVENTA, MIL_CIENTO_NOVENTA);

            assertThat(linea.getQuantity()).isEqualByComparingTo("2.5");
            assertThat(linea.getUnitMeasureCode()).isEqualTo("KGM");
        }
    }

    @Nested
    @DisplayName("estado expuesto")
    class EstadoExpuesto {

        @Test
        @DisplayName("devuelve cada campo tal como se construyo")
        void devuelve_cada_campo_tal_como_se_construyo() {
            ElectronicDocumentLine linea = new ElectronicDocumentLine(33L, 4, "Vacuna antirrabica",
                    new BigDecimal("2"), "94", new BigDecimal("500.00"), MIL, TaxCategory.GRAVADO,
                    TaxScheme.IVA, new BigDecimal("19"), CIENTO_NOVENTA, MIL_CIENTO_NOVENTA);

            assertThat(linea.getId()).isEqualTo(33L);
            assertThat(linea.getLineNumber()).isEqualTo(4);
            assertThat(linea.getDescription()).isEqualTo("Vacuna antirrabica");
            assertThat(linea.getQuantity()).isEqualByComparingTo("2");
            assertThat(linea.getUnitMeasureCode()).isEqualTo("94");
            assertThat(linea.getUnitPrice()).isEqualByComparingTo("500.00");
            assertThat(linea.getLineExtensionAmount()).isEqualByComparingTo("1000.00");
            assertThat(linea.getTaxCategory()).isEqualTo(TaxCategory.GRAVADO);
            assertThat(linea.getTaxScheme()).isEqualTo(TaxScheme.IVA);
            assertThat(linea.getTaxRate()).isEqualByComparingTo("19");
            assertThat(linea.getTaxAmount()).isEqualByComparingTo("190.00");
            assertThat(linea.getTotalAmount()).isEqualByComparingTo("1190.00");
        }

        @Test
        @DisplayName("una linea nueva no tiene id hasta persistirse")
        void una_linea_nueva_no_tiene_id() {
            assertThat(lineaValida().getId()).isNull();
        }
    }
}
